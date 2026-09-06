#!/usr/bin/env python3
"""Download only individually verified free-licensed Commons photographs."""
import sys,os
from concurrent.futures import ThreadPoolExecutor,as_completed
from pathlib import Path
from PIL import Image
from common import get,load,save,chunks
from fetch_catalog import CACHE,path
from fetch_content import claims,val
from image_sources import imageinfo,choose,category_files
ROOT=Path(__file__).resolve().parents[1]
def main():
    es=load(path('entities.json'));selected=load(path('selected.json'));info=load(path('imageinfo.json'),{});meta=load(path('photos.json'),{})
    wiki={lang:load(path('wiki-'+lang+'.json'),{}) for lang in ['ja','en']}
    links=load(path('article-links.json'),{})
    cands={}
    for s in selected:
        q=s['qid'];e=es[q];cs=[val(c) for c in claims(e,'P18')]
        for lang in ['ja','en']:
            title=e.get('sitelinks',{}).get(lang+'wiki',{}).get('title','');pi=wiki[lang].get(title,links.get(q,{}).get(lang,{})).get('pageimage')
            if pi and pi not in cs:cs.append(pi)
        cands[q]=[x.replace('_',' ') for x in cs if isinstance(x,str)]
    need=sorted({f for fs in cands.values() for f in fs if f not in info})
    for i,b in enumerate(chunks(need,35)):
        info.update(imageinfo(b));save(path('imageinfo.json'),info);print('metadata',i*35+len(b),'/',len(need),flush=True)
    if '--fallback' in sys.argv:
        weak=[s for s in selected if not choose(cands[s['qid']],info)]
        cats=load(path('category-files.json'),{})
        for i,s in enumerate(weak):
            q=s['qid'];e=es[q];cat=next((val(c) for c in claims(e,'P373')),'')
            if cat:
                if cat not in cats:cats[cat]=category_files(cat,20);save(path('category-files.json'),cats)
                fs=cats[cat];missing=[f for f in fs if f not in info]
                if missing:info.update(imageinfo(missing));save(path('imageinfo.json'),info)
                cands[q]+=fs
            print('fallback',i+1,'/',len(weak),flush=True)
    for s in selected:
        q=s['qid'];m=choose(cands[q],info)
        if m:meta[q]=m
    save(path('photos.json'),meta)
    dest=ROOT/'app/src/main/assets/img';dest.mkdir(parents=True,exist_ok=True)
    todo=[(q,m) for q,m in meta.items() if m and not (dest/(q[1:]+'.jpg')).exists()]
    def dl(qm):
        q,m=qm
        try:
            import io
            raw=get(m['thumb'],binary=True,timeout=40,retries=3)
            im=Image.open(io.BytesIO(raw));im.thumbnail((800,800));im.convert('RGB').save(dest/(q[1:]+'.jpg'),quality=80,optimize=True)
            return q,True
        except Exception as ex:return q,str(ex)
    with ThreadPoolExecutor(max_workers=3) as pool:
        for i,f in enumerate(as_completed([pool.submit(dl,qm) for qm in todo])):
            q,ok=f.result()
            if ok is not True:print('FAILED',q,ok,flush=True)
            if i%25==0:print('download',i+1,'/',len(todo),flush=True)
    print('files',len(list(dest.glob('*.jpg'))),'/',len(selected),flush=True)
if __name__=='__main__':main()
