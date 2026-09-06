#!/usr/bin/env python3
"""Fetch Wikidata CC0 entities; checkpoints allow safe incremental refresh."""
import sys, re, json
from pathlib import Path
from common import get_json, load, save, chunks
CACHE=Path(__file__).resolve().parents[1]/'data/cache'
CACHE.mkdir(parents=True,exist_ok=True)
def path(n): return str(CACHE/n)

def entities(ids, out, props='labels|aliases|descriptions|claims|sitelinks'):
    result=load(path(out),{})
    todo=[x for x in ids if x not in result]
    for i,b in enumerate(chunks(todo,40)):
        d=get_json('https://www.wikidata.org/w/api.php',{'action':'wbgetentities','format':'json','ids':'|'.join(b),'props':props,'languages':'ja|en','sitefilter':'jawiki|enwiki|commonswiki'})
        result.update(d['entities']);save(path(out),result)
        print(out,i*40+len(b),'/',len(todo),flush=True)
    return result

def main():
    raw=load(path('ids.json'))
    if raw is None:
        raw=get_json('https://query.wikidata.org/sparql',{'query':'SELECT ?item ?id WHERE { ?item wdt:P757 ?id }','format':'json'})
        save(path('ids.json'),raw)
    rows=[(x['item']['value'].split('/')[-1],x['id']['value']) for x in raw['results']['bindings'] if re.fullmatch(r'\d+(?:bis|ter|quater|quinquies|rev)*',x['id']['value'])]
    save(path('candidates.json'),rows)
    print('candidates',len(rows),flush=True)
    es=entities(sorted(set(x[0] for x in rows)),'entities.json')
    refs=set()
    for e in es.values():
        for prop in ['P17','P1435','P31']:
            for c in e.get('claims',{}).get(prop,[]):
                v=c['mainsnak'].get('datavalue',{}).get('value',{})
                if isinstance(v,dict) and 'id' in v: refs.add(v['id'])
    entities(sorted(refs),'references.json')
if __name__=='__main__':main()
