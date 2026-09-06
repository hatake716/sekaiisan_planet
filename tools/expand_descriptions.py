#!/usr/bin/env python3
"""Create detailed offline descriptions from already fetched Wikipedia revisions.
No network requests. Wikipedia prose remains CC BY-SA 4.0 with its exact revision URL.
"""
import re
import mwparserfromhell
from common import load,save
from fetch_catalog import path

def plain_excerpt(raw,language):
    # Exclude reference lists, navigation sections and quoted third-party passages.
    raw=re.split(r'\n==+\s*(?:脚注|参考文献|外部リンク|関連項目|注釈|References|See also|External links|Notes)\s*==+',raw)[0]
    raw=re.sub(r'\{\|.*?\|\}', '', raw, flags=re.S)
    code=mwparserfromhell.parse(raw)
    for tag in list(code.filter_tags(recursive=True)):
        if str(tag.tag).lower() in ['ref','gallery','table','timeline','score','math','blockquote','syntaxhighlight']:
            try:code.remove(tag,recursive=True)
            except ValueError:pass
    for link in list(code.filter_wikilinks(recursive=True)):
        if re.match(r'^(?:File|Image|Category|ファイル|画像|カテゴリ):',str(link.title),re.I):
            try:code.remove(link,recursive=True)
            except ValueError:pass
    text=code.strip_code(normalize=True,collapse=True)
    text=re.sub(r'__[A-Z_]+__','',text)
    text=re.sub(r'https?://\S+','',text)
    text=re.sub(r'\[\d+\]','',text)
    text=re.sub(r'^[*#:;]+\s*','',text,flags=re.M)
    text=re.sub(r'[ \t]+',' ',text)
    text=re.sub(r'\n\s*\n\s*\n','\n\n',text).strip()
    cap=1800 if language=='ja' else 3200
    if len(text)>cap:
        stop=max(text.rfind('。',100,cap),text.rfind('. ',100,cap))
        text=text[:stop+1] if stop>100 else text[:cap].rsplit(' ',1)[0]
    return text

def main():
    es=load(path('entities.json'));selected=load(path('selected.json'));links=load(path('article-links.json'),{})
    for lang in ['ja','en']:
        wiki=load(path('wiki-'+lang+'.json'),{})
        out={}
        pages=list(wiki.values())+[v[lang] for v in links.values() if lang in v]
        for p in pages:
            if not p.get('text') or not p.get('revision'):continue
            text=plain_excerpt(p['text'],lang)
            # Keep a long, cleaner API lead; otherwise use source prose.
            lead=p.get('extract','')
            if len(lead)>=700 and len(text)<len(lead):text=lead
            out[p['title']]={'extract':text,'revision':p['revision'],'timestamp':p.get('timestamp'),'source':'cached Wikipedia wikitext; formatting and references removed'}
        save(path('expanded-'+lang+'.json'),out)
        print(lang,len(out),'descriptions; median length',sorted(len(p['extract']) for p in out.values())[len(out)//2],flush=True)
if __name__=='__main__':main()
