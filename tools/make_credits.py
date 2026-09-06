#!/usr/bin/env python3
import json
from pathlib import Path
from urllib.parse import quote
ROOT=Path(__file__).resolve().parents[1]
cat=json.loads((ROOT/'app/src/main/assets/heritage.json').read_text())
out=['# 写真・説明の出典一覧','',f'データ取得: {cat["snapshotDate"]}。写真は長辺 800px 以下に縮小し、JPEG に変換しています。各写真の著作権はそれぞれの作者にあります。CC BY-SA の加工物は各写真の同じライセンスで提供します。説明は Wikipedia の抜粋（CC BY-SA 4.0）です。','']
for e in cat['entries']:
    c=e['image'];commons='https://commons.wikimedia.org/wiki/File:'+quote(c['file'].replace(' ','_'))
    out += [f'## {e["name"]}（{e["heritageId"]}）','',f'- 写真: [{c["file"]}]({commons})',f'- 作者: {c["author"]}',f'- ライセンス: [{c["license"]}]({c["licenseUrl"]})',f'- 説明: [Wikipedia「{e["sourceTitle"]}」の執筆者]({e["sourceUrl"]}) / CC BY-SA 4.0',f'- 登録情報・座標: [Wikidata Q{e["id"]}](https://www.wikidata.org/wiki/Q{e["id"]})',f'- 分類: [出典]({e["categorySourceUrl"]})',f'- 登録年: [出典]({e["yearSourceUrl"]})','']
(ROOT/'docs/CREDITS.md').write_text('\n'.join(out))
print('credits',len(cat['entries']))
