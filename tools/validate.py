#!/usr/bin/env python3
"""Release gate for real data, per-item license attribution and all packaged photos."""
import json,re,hashlib,sys
from pathlib import Path
from urllib.parse import urlparse
ROOT=Path(__file__).resolve().parents[1]
def main():
    cat=json.loads((ROOT/'app/src/main/assets/heritage.json').read_text());rows=cat['entries'];prov=json.loads((ROOT/'data/provenance.json').read_text());errors=[]
    def check(ok,message):
        if not ok:errors.append(message)
    check(len(rows)>900,'catalog unexpectedly small')
    check(len({e['id'] for e in rows})==len(rows),'duplicate stable IDs')
    check(len({e['heritageId'] for e in rows})==len(rows),'duplicate registered properties')
    check({e['group'] for e in rows}=={0,1,2},'missing heritage category')
    check(len(prov)==len(rows),'missing provenance rows')
    byid={p['id']:p for p in prov};used=set()
    audit=json.loads((ROOT/'data/photo-license-audit.json').read_text())['photos']
    for e in rows:
        q=e['id'];name=e['name'];prefix=f'{q} {name}: '
        check(-90<=e['lat']<=90 and -180<=e['lon']<=180,prefix+'invalid coordinate')
        check(e['regionMask']>0 and e['regionMask']<(1<<len(cat['regions'])),prefix+'invalid region')
        check(e['group'] in [0,1,2],prefix+'invalid classification')
        check(e['year']==0 or 1978<=e['year']<=int(cat['snapshotDate'][:4]),prefix+'invalid inscription year')
        check(len(e['desc'])>=100,prefix+'description too short')
        check(not any(s in e['desc'] for s in ['(仮)説明文','TODO','Lorem ipsum']),prefix+'placeholder description')
        check(bool(e['wikiTitle'] or e['enTitle']),prefix+'no Wikipedia article')
        check(urlparse(e['sourceUrl']).hostname in ['ja.wikipedia.org','en.wikipedia.org'] and 'oldid=' in e['sourceUrl'],prefix+'no revision-linked description source')
        check(bool(e['categorySourceUrl']),prefix+'no classification source')
        photo=e.get('image');check(bool(photo),prefix+'no licensed photo')
        if not photo:continue
        a=audit.get(str(q),{})
        check(a.get('ok') is True and not a.get('restrictions'),prefix+'restricted/unverified photo')
        check(a.get('license')==photo['license'] and a.get('file')==photo['file'],prefix+'license audit mismatch')
        check(urlparse(a.get('descUrl','')).hostname=='commons.wikimedia.org',prefix+'non-Commons source')
        p=ROOT/f'app/src/main/assets/img/{q}.jpg';used.add(p.name)
        check(p.exists(),prefix+'photo missing')
        check(bool(photo['author']) and bool(photo['file']),prefix+'incomplete photo attribution')
        check(bool(re.fullmatch(r'CC BY(?:-SA)? (?:1\.0|2\.0|2\.1|2\.5|3\.0|4\.0)(?: [A-Za-z -]+)?|CC0 1\.0|Public domain',photo['license'])),prefix+'unsupported license: '+photo['license'])
        check(urlparse(photo['licenseUrl']).scheme=='https',prefix+'license URL must be HTTPS')
        pp=byid.get(q,{}).get('photo',{})
        if p.exists():check(pp.get('sha256')==hashlib.sha256(p.read_bytes()).hexdigest(),prefix+'photo hash/provenance mismatch')
    packaged={p.name for p in (ROOT/'app/src/main/assets/img').glob('*.jpg')}
    check(packaged==used,f'unattributed/unused image assets: {len(packaged-used)}')
    check(len(list((ROOT/'app/src/main/assets/tiles').rglob('*.jpg')))>0,'missing NASA globe tiles')
    if errors:
        print('\n'.join(errors));print(f'FAILED: {len(errors)} issues');return 1
    print(f'PASS: {len(rows)} properties, {len(used)} photos, complete provenance, categories and coordinates')
    return 0
if __name__=='__main__':sys.exit(main())
