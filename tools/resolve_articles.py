#!/usr/bin/env python3
"""Link registered ensembles to Wikipedia only when the article contains the same heritage ID."""
import re
from common import get_json,load,save,chunks
from fetch_catalog import path,entities
from fetch_content import claims,val

def fetch(lang,titles):
    out=load(path('extra-'+lang+'.json'),{})
    todo=sorted(set(titles)-{''}-set(out))
    for i,b in enumerate(chunks(todo,20)):
        d=get_json('https://'+lang+'.wikipedia.org/w/api.php',{'action':'query','format':'json','formatversion':2,'redirects':1,'prop':'extracts|pageimages|pageprops|revisions','exintro':1,'explaintext':1,'exlimit':20,'exchars':3500,'piprop':'name','ppprop':'disambiguation|wikibase_item','rvprop':'ids|timestamp|content','rvslots':'main','titles':'|'.join(b)})
        qu=d.get('query',{});red={x['from']:x['to'] for x in qu.get('normalized',[])+qu.get('redirects',[])};pages={p['title']:p for p in qu.get('pages',[])}
        for title in b:
            t=title
            for _ in range(6):
                if t not in red:break
                t=red[t]
            p=pages.get(t,{})
            if not p or p.get('missing') or 'disambiguation' in p.get('pageprops',{}):out[title]={'missing':True};continue
            rev=p.get('revisions',[{}])[0];out[title]={'title':p['title'],'extract':p.get('extract',''),'pageimage':p.get('pageimage',''),'qid':p.get('pageprops',{}).get('wikibase_item',''),'revision':rev.get('revid'),'timestamp':rev.get('timestamp'),'text':rev.get('slots',{}).get('main',{}).get('content','')}
        save(path('extra-'+lang+'.json'),out);print(lang,i*20+len(b),'/',len(todo),flush=True)
    return out

def main():
    es=load(path('entities.json'));sel=load(path('selected.json'));wiki={l:load(path('wiki-'+l+'.json'),{}) for l in ['ja','en']};related={};need=set()
    for s in sel:
        e=es[s['qid']]
        if not e.get('sitelinks',{}).get('jawiki') or not e.get('sitelinks',{}).get('enwiki'):
            rr=[val(c)['id'] for p in ['P527','P131','P301'] for c in claims(e,p) if isinstance(val(c),dict) and 'id' in val(c)]
            related[s['qid']]=rr;need.update(rr)
    refs=entities(sorted(need),'article-entities.json','labels|sitelinks')
    links=load(path('article-links.json'),{})
    for lang in ['ja','en']:
        cands={}
        for s in sel:
            q=s['qid'];e=es[q]
            old=e.get('sitelinks',{}).get(lang+'wiki',{}).get('title','')
            if old and wiki[lang].get(old,{}).get('extract'):continue
            names=[e.get('labels',{}).get(lang,{}).get('value','')]
            names += [a['value'] for a in e.get('aliases',{}).get(lang,[])][:3]
            for qs in related.get(q,[]):names += [refs.get(qs,{}).get('sitelinks',{}).get(lang+'wiki',{}).get('title','')]
            cands[q]=list(dict.fromkeys(n for n in names if n))
        extra=fetch(lang,[t for ts in cands.values() for t in ts])
        count=0
        for s in sel:
            q=s['qid'];id=s['heritageId']
            for t in cands.get(q,[]):
                p=extra.get(t,{})
                if len(p.get('extract',''))<100:continue
                text=p.get('text','')
                # Exact infobox property, not an incidental mention/link to another site.
                if (re.search(r'\|\s*(?:url_no|UNESCO_ID|UNESCO_id|id|ID)\s*=\s*'+id+r'(?:bis|ter|quater|quinquies|rev)*(?:[^\d]|$)',text) or any(re.search(r'\|\s*designation'+str(n)+r'\s*=\s*(?:WHS|UNESCO)',text,re.I) and re.search(r'\|\s*designation'+str(n)+r'_number\s*=\s*'+id+r'(?:bis|ter|quater|quinquies|rev)*(?:[^\d]|$)',text,re.I) for n in range(1,5))) and ('世界遺産' in text or re.search('UNESCO|World Heritage',text,re.I)):
                    links.setdefault(q,{})[lang]=p;count+=1;break
        save(path('article-links.json'),links);print('matched',lang,count,flush=True)
if __name__=='__main__':main()
