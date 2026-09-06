#!/usr/bin/env python3
"""
Commons image metadata, license allowlist and photograph selection.
Adapted from hatake716/animal_planet (MIT). Only CC0 / Public domain / CC BY / CC BY-SA
with required attribution and no additional restriction is eligible. File metadata is
retained by fetch_photos.py, and each packaged photograph has a provenance record.
"""
import html
import os
import re
import sys
from common import get, get_json, load, save, pmap, chunks

COMMONS = "https://commons.wikimedia.org/w/api.php"
OK_MIME = {"image/jpeg", "image/png", "image/tiff", "image/webp"}
FIELDS = "LicenseShortName|License|Artist|Credit|UsageTerms|AttributionRequired|Copyrighted|Restrictions|LicenseUrl|ImageDescription"
MAP_PAT = re.compile(
    r"(map|range|distribution|distribu|verbreitung|\barea\b|habitat|locator|karte|\bmapa\b|carte|\bdist\b|distmap|"
    r"występowanie|分布|区域|areal|localis|location|\bloc\b|\bareas\b|\brange\b)",
    re.I,
)


def strip_tags(s):
    s = re.sub(r"<br\s*/?>", " ", s or "")
    s = re.sub(r"<[^>]+>", "", s)
    s = html.unescape(s)
    return re.sub(r"\s+", " ", s).strip()


def looks_like_map(filename):
    return bool(MAP_PAT.search(filename))


def license_ok(short, usage, lic):
    """(採用可否, 正規化した短い表記)。CC0 / パブリックドメイン / CC BY / CC BY-SA だけを採用する。"""
    s = (short or "").strip()
    u = (usage or "").lower()
    l = (lic or "").lower()
    low = s.lower()
    if not s:
        return False, ""
    if "nc" in l.split("-") or "noncommercial" in u or "non-commercial" in u or "-nc" in low:
        return False, s
    if "-nd" in low or "nd" in l.split("-") or "no derivative" in u:
        return False, s
    if "fair use" in low or "non-free" in low or "copyrighted" == low:
        return False, s
    if low in ("cc0", "cc zero") or low.startswith("cc0"):
        return True, "CC0 1.0"
    if "public domain" in low or low.startswith("pd") or "pd-" in l:
        # 米国内でのみパブリックドメインとされるテンプレート(更新なし・表示なし・1996 年など)は日本向けには使わない
        if re.search(r"pd-us-(not|no-notice|1996|unpublished|expired-abroad|record)", l) or "in the united states" in u:
            return False, s
        return True, "Public domain"
    if low.startswith("cc by-sa") or low.startswith("cc-by-sa"):
        return True, s.replace("CC-BY-SA", "CC BY-SA")
    if low.startswith("cc by") or low.startswith("cc-by"):
        return True, s.replace("CC-BY", "CC BY")
    # GFDL 単独、Attribution / No restrictions などの汎用テンプレート、GPL 等は宣言(NOTICE)と揃えるため不採用
    return False, s


def imageinfo(files):
    d = get_json(COMMONS, {
        "action": "query", "format": "json", "formatversion": 2,
        "titles": "|".join("File:" + f for f in files),
        "prop": "imageinfo", "iiprop": "extmetadata|url|size|mime|user", "iiurlwidth": 800,
        "iiextmetadatafilter": FIELDS,
    })
    out = {}
    if not d:
        return out
    q = d.get("query", {})
    norm = {r["from"]: r["to"] for r in q.get("normalized", [])}
    pages = {p["title"]: p for p in q.get("pages", [])}
    for f in files:
        t = norm.get("File:" + f, "File:" + f)
        p = pages.get(t)
        if not p or p.get("missing") or not p.get("imageinfo"):
            out[f] = None
            continue
        ii = p["imageinfo"][0]
        em = {k: v.get("value", "") for k, v in ii.get("extmetadata", {}).items()}
        ok, lic = license_ok(em.get("LicenseShortName"), em.get("UsageTerms"), em.get("License"))
        name = t[5:]
        artist = strip_tags(em.get("Artist", ""))
        credit = strip_tags(em.get("Credit", ""))
        uploader = ii.get("user", "")
        # 作者が空のとき: 「Own work」ならアップロード者が作者。それ以外は Credit(出典)欄で代用する。
        if not artist:
            if uploader and re.search(r"own work|自身の作品|eigenes werk|travail personnel|obra propia", credit, re.I):
                artist = uploader
        attribution_required = lic not in ("CC0 1.0", "Public domain")
        out[f] = {
            "file": name, "mime": ii.get("mime", ""), "width": ii.get("width", 0), "height": ii.get("height", 0),
            "thumb": ii.get("thumburl", ""), "descUrl": ii.get("descriptionurl", ""),
            "artist": artist, "credit": credit[:200], "uploader": uploader,
            "licenseShort": em.get("LicenseShortName", ""), "license": lic, "licenseUrl": em.get("LicenseUrl", ""),
            "licenseTemplate": em.get("License", ""),
            "usageTerms": em.get("UsageTerms", ""), "restrictions": em.get("Restrictions", ""),
            # 帰属表示が必要なライセンスで作者が分からない写真は採用しない
            "ok": ok and ii.get("mime", "") in OK_MIME and bool(ii.get("thumburl")) and not looks_like_map(name)
                and (artist != "" or not attribution_required) and not em.get("Restrictions", "") and bool(em.get("LicenseUrl", "") or lic in ("CC0 1.0", "Public domain")),
            "map": looks_like_map(name),
        }
    return out


def category_files(cat, limit=12):
    d = get_json(COMMONS, {
        "action": "query", "format": "json", "formatversion": 2, "list": "categorymembers",
        "cmtitle": "Category:" + cat, "cmtype": "file", "cmlimit": 40,
    })
    if not d:
        return []
    names = [m["title"][5:] for m in d.get("query", {}).get("categorymembers", [])]
    names = [n for n in names if n.lower().endswith((".jpg", ".jpeg", ".png")) and not looks_like_map(n)]
    # 写真らしい JPEG を先に
    names.sort(key=lambda n: 0 if n.lower().endswith((".jpg", ".jpeg")) else 1)
    return names[:limit]


def choose(cands, info):
    """候補(優先順)から採用可能な最初の画像。JPEG を PNG より優先する。"""
    usable = [info[f] for f in cands if info.get(f) and info[f]["ok"]]
    if not usable:
        return None
    jpeg = [m for m in usable if m["mime"] == "image/jpeg"]
    return (jpeg or usable)[0]
