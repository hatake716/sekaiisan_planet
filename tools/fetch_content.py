#!/usr/bin/env python3
"""Fetch Wikipedia revision-linked lead text and Commons photo candidates (CC BY-SA)."""
import re,json,sys
from collections import defaultdict
from common import get_json,load,save,chunks
from fetch_catalog import CACHE,path

def val(c):return c.get('mainsnak',c).get('datavalue',{}).get('value')
def claims(e,p):return [c for c in e.get('claims',{}).get(p,[]) if c.get('rank')!='deprecated' and val(c) is not None]
def main():
    es=load(path('entities.json'));groups=defaultdict(list);rejected=[]
    for q,e in es.items():
        wh=[c for c in claims(e,'P1435') if isinstance(val(c),dict) and val(c).get('id')=='Q9259' and 'P582' not in c.get('qualifiers',{})]
        if not wh:rejected.append([q,'no current world heritage statement']);continue
        ids=[re.match(r'^(\d+)(?:bis|ter|quater|quinquies|rev)*$',val(c)) for c in claims(e,'P757') if isinstance(val(c),str)]
        ids=[m[1] for m in ids if m]
        for id in ids:groups[id].append(e)
    def score(e):
        wh=[c for c in claims(e,'P1435') if val(c).get('id')=='Q9259' and 'P582' not in c.get('qualifiers',{})]
        # Prefer the registered ensemble over one of its constituent sites.
        official=[val(v).get('text','') for c in wh for v in c.get('qualifiers',{}).get('P1448',[]) if isinstance(val(v),dict) and val(v).get('language')=='en']
        title=e.get('sitelinks',{}).get('enwiki',{}).get('title','')
        return (bool(official and title in official)*20 + any(val(c).get('id')=='Q9259' for c in claims(e,'P31'))*10
            + bool(wh)*5+bool(claims(e,'P625'))*5+bool(e.get('sitelinks',{}).get('jawiki'))*3+bool(title)*3+bool(claims(e,'P18')))
    selected=[];duplicates={}
    for id,ee in groups.items():
        ee=sorted(ee,key=lambda e:(-score(e),int(e['id'][1:])))
        selected.append({'qid':ee[0]['id'],'heritageId':id})
        if len(ee)>1:duplicates[id]=[e['id'] for e in ee]
    save(path('selected.json'),selected);save(path('duplicates.json'),duplicates);save(path('rejected.json'),rejected)
    print('selected',len(selected),'duplicates',len(duplicates),'rejected',len(rejected),flush=True)
    for lang in ['ja','en']:
        out=load(path('wiki-'+lang+'.json'),{})
        titles=sorted(set(es[s['qid']].get('sitelinks',{}).get(lang+'wiki',{}).get('title','') for s in selected)-{''})
        todo=[t for t in titles if t not in out]
        for i,b in enumerate(chunks(todo,20)):
            d=get_json('https://'+lang+'.wikipedia.org/w/api.php',{'action':'query','format':'json','formatversion':2,'redirects':1,'prop':'extracts|pageimages|pageprops|revisions','exintro':1,'explaintext':1,'exlimit':20,'exchars':3500,'piprop':'name','ppprop':'disambiguation|wikibase_item','rvprop':'ids|timestamp|content','rvslots':'main','titles':'|'.join(b)})
            qu=d.get('query',{});redir={x['from']:x['to'] for x in qu.get('redirects',[])+qu.get('normalized',[])}
            pages={p['title']:p for p in qu.get('pages',[])}
            for t in b:
                final=t
                for _ in range(6):
                    if final not in redir:break
                    final=redir[final]
                p=pages.get(final)
                if not p or p.get('missing') or 'disambiguation' in p.get('pageprops',{}):out[t]={'missing':True};continue
                rev=p.get('revisions',[{}])[0]
                out[t]={'title':p['title'],'extract':p.get('extract',''),'pageimage':p.get('pageimage',''),'qid':p.get('pageprops',{}).get('wikibase_item',''),'revision':rev.get('revid'),'timestamp':rev.get('timestamp'),'text':rev.get('slots',{}).get('main',{}).get('content','')}
            save(path('wiki-'+lang+'.json'),out)
            print(lang,i*20+len(b),'/',len(todo),flush=True)
if __name__=='__main__':main()
