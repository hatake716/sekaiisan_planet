#!/usr/bin/env python3
"""Assemble real, attributed assets. Missing facts are reported, never guessed."""
import json,re,hashlib,datetime,sys,os
from pathlib import Path
from collections import Counter
from common import load,save
from fetch_catalog import path
from fetch_content import claims,val
ROOT=Path(__file__).resolve().parents[1]
REGIONS=['日本','アジア','ヨーロッパ','アフリカ','北アメリカ','南アメリカ','オセアニア']
CRITERIA=['Q23038972','Q23038976','Q23038977','Q23038978','Q23038979','Q23038980','Q23038981','Q23038983','Q23038985','Q23038986']
ROMAN=['i','ii','iii','iv','v','vi','vii','viii','ix','x']
def label(e,lang):return e.get('labels',{}).get(lang,{}).get('value','')
def url(p,lang):return f'https://{lang}.wikipedia.org/w/index.php?oldid={p["revision"]}'
def param(text,key):
    m=re.search(r'\|\s*'+key+r'\s*=\s*',text,re.I)
    if not m:return ''
    i=m.end();start=i;depth=0
    while i<len(text):
        if text[i:i+2]=='{{':depth+=1;i+=2;continue
        if text[i:i+2]=='}}':
            if depth==0:break
            depth-=1;i+=2;continue
        if depth==0 and text[i] in '|\n':break
        i+=1
    return re.sub(r'<!--.*?-->','',text[start:i],flags=re.S).strip()
def category(pages,wdcriteria):
    # Wikipedia is used first when both are present; it commonly records later extensions to mixed status.
    for lang,p in pages.items():
        t=p.get('text','');c=param(t,'criterion_c');n=param(t,'criterion_n')
        if re.search(r'\d',c+n):return (2 if re.search(r'\d',c) and re.search(r'\d',n) else (1 if re.search(r'\d',n) else 0)),url(p,lang)
        cr=param(t,r'(?:UNESCO_)?criteria');ty=param(t,r'(?:UNESCO_)?type')
        for n in range(1,5):
            if re.search(r'WHS|UNESCO',param(t,'designation'+str(n)),re.I):
                cr=cr+' '+param(t,'designation'+str(n)+'_criteria');ty=ty+' '+param(t,'designation'+str(n)+'_type')
        whole=cr+' '+ty
        if re.search(r'\bmixed\b',whole,re.I):return 2,url(p,lang)
        if re.search(r'\bnatural\b',whole,re.I):return 1,url(p,lang)
        if re.search(r'\bcultural\b',whole,re.I):return 0,url(p,lang)
        roman=re.findall(r'\b(?:i|ii|iii|iv|v|vi|vii|viii|ix|x)\b',cr.lower())
        if roman:
            codes={ROMAN.index(x)+1 for x in roman};return (2 if min(codes)<=6<max(codes) else (1 if min(codes)>6 else 0)),url(p,lang)
    if wdcriteria:
        return (2 if min(wdcriteria)<=6<max(wdcriteria) else (1 if min(wdcriteria)>6 else 0)),''
    return None,''
def geographic(lat,lon):
    if lon < -30:
        if (lat>5 and lon < -125) or lon < -145:return 6
        return 5 if lat<13 and lon>-83 else 4
    if lat < -10 and lon > 110:return 6
    if -20<lon<52 and -36<lat<37:return 3
    if -30<lon<60 and lat>=35:return 2
    return 1

def main():
    es=load(path('entities.json'));refs=load(path('references.json'));sel=load(path('selected.json'));photos=load(path('photos.json'),{})
    wiki={l:load(path('wiki-'+l+'.json'),{}) for l in ['ja','en']};links=load(path('article-links.json'),{});out=[];issues=[];provenance=[]
    expanded_text={l:load(path('expanded-'+l+'.json'),{}) for l in ['ja','en']}
    for s in sel:
        q=s['qid'];e=es[q];wdurl='https://www.wikidata.org/wiki/'+q
        pages={l:wiki[l].get(e.get('sitelinks',{}).get(l+'wiki',{}).get('title',''),{}) for l in ['ja','en']}
        pages={l:p for l,p in pages.items() if p and not p.get('missing') and p.get('revision')}
        for l,p in links.get(q,{}).items():
            if l not in pages:pages[l]=p
        pages={l:pages[l] for l in ['ja','en'] if l in pages}
        for l,p in pages.items():
            expanded=expanded_text[l].get(p.get('title',''),{})
            if len(expanded.get('extract',''))>len(p.get('extract','')):pages[l]=dict(p,**expanded)
        lang=next((l for l,p in pages.items() if len(p.get('extract',''))>=100),None)
        if not lang:issues.append([q,s['heritageId'],'no substantial Wikipedia lead']);continue
        page=pages[lang];wh=[c for c in claims(e,'P1435') if isinstance(val(c),dict) and val(c).get('id')=='Q9259' and 'P582' not in c.get('qualifiers',{})]
        qs=[c.get('qualifiers',{}) for c in wh]
        cr=sorted({CRITERIA.index(val(v)['id'])+1 for qs0 in qs for v in qs0.get('P2614',[]) if isinstance(val(v),dict) and val(v).get('id') in CRITERIA})
        group,caturl=category(pages,cr)
        if group is None:issues.append([q,s['heritageId'],'no verified classification']);continue
        coords=[val(c) for c in claims(e,'P625') if isinstance(val(c),dict) and val(c).get('globe','').endswith('/Q2')]
        if not coords:issues.append([q,s['heritageId'],'no Wikidata coordinate']);continue
        coord=coords[0];lat=coord['latitude'];lon=coord['longitude']
        countries=[val(c)['id'] for c in claims(e,'P17') if isinstance(val(c),dict)]
        if not countries:issues.append([q,s['heritageId'],'no country']);continue
        names=[label(refs.get(c,{}),'ja') or label(refs.get(c,{}),'en') for c in countries];names=[n for n in names if n]
        if not names:issues.append([q,s['heritageId'],'no country label']);continue
        bits=set()
        cm={'Q48':1,'Q46':2,'Q15':3,'Q49':4,'Q18':5,'Q538':6,'Q55643':6,'Q3800':6}
        for c in countries:
            cs={cm[v['id']] for cl in claims(refs.get(c,{}),'P30') if isinstance(v:=val(cl),dict) and v.get('id') in cm}
            bits.update(cs if len(cs)==1 else [geographic(lat,lon)])
        if 'Q17' in countries:bits|={0,1}
        years=[]
        for qs0 in qs:
            for v in qs0.get('P580',[]):
                vv=val(v)
                if isinstance(vv,dict):years.append(int(vv['time'][1:5]))
        year=min(years) if years else 0
        year_source=wdurl
        if year and not 1978<=year<=datetime.date.today().year:
            year=0
        if not year:
            for l,p in pages.items():
                t=p.get('text','');year_text=param(t,'rg_year')+' '+param(t,'year')
                for n in range(1,5):
                    if re.search(r'WHS|UNESCO',param(t,'designation'+str(n)),re.I):year_text+=' '+param(t,'designation'+str(n)+'_date')
                matches=[int(y) for y in re.findall(r'\b(?:19|20)\d{2}\b',year_text) if 1978<=int(y)<=datetime.date.today().year]
                if matches:year=min(matches);year_source=url(p,l);break
        jaTitle=pages.get('ja',{}).get('title','');enTitle=pages.get('en',{}).get('title','')
        name=jaTitle or label(e,'ja') or label(e,'en')
        # Native names of the registered ensemble take precedence over geographic feature names.
        official=[val(v)['text'] for qs0 in qs for v in qs0.get('P1448',[]) if isinstance(val(v),dict) and val(v).get('language')=='ja']
        if official:name=official[0]
        aliases=list(dict.fromkeys([a['value'] for a in e.get('aliases',{}).get('ja',[])]+([jaTitle] if jaTitle and jaTitle!=name else [])))
        desc=re.sub(r'\[\d+\]','',page['extract']).strip()
        if desc.endswith('...') or desc.endswith('…'):
            stop=max(desc.rfind('。'),desc.rfind('. '));desc=desc[:stop+1] if stop>150 else desc
        image=None;m=photos.get(q);file=ROOT/f'app/src/main/assets/img/{q[1:]}.jpg'
        if m and m['ok'] and file.exists():
            licurl=m['licenseUrl'] or ('https://creativecommons.org/publicdomain/zero/1.0/' if m['license']=='CC0 1.0' else 'https://creativecommons.org/publicdomain/mark/1.0/')
            if licurl.startswith('//'):licurl='https:'+licurl
            if licurl.startswith('http:'):licurl='https:'+licurl[5:]
            image={'file':m['file'],'author':m['artist'] or '作者表記なし（パブリックドメイン）','license':m['license'],'licenseUrl':licurl}
        if '--final' in sys.argv and image is None:
            issues.append([q,s['heritageId'],'no verified free-licensed photo']);continue
        row={'id':int(q[1:]),'heritageId':s['heritageId'],'name':name,'aliases':aliases,'englishName':label(e,'en'),'wikiTitle':jaTitle,'enTitle':enTitle,'lat':lat,'lon':lon,'country':'・'.join(dict.fromkeys(names)),'group':group,'regionMask':sum(1<<i for i in bits),'importance':3 if len(e.get('sitelinks',{}))>=2 and ('Q17' in countries or s['heritageId'] in ['252','153','86','438','66','91','404','75','709','274','600','422']) else 2,'desc':desc,'year':year,'criteria':' '.join('('+ROMAN[x-1]+')' for x in cr),'sourceUrl':url(page,lang),'sourceTitle':page['title'],'sourceLang':lang,'categorySourceUrl':caturl or wdurl,'yearSourceUrl':year_source,'image':image}
        out.append(row)
        provenance.append({'id':row['id'],'heritageId':row['heritageId'],'wikidata':{'url':wdurl,'revision':e.get('lastrevid'),'license':'CC0 1.0'},'description':{'url':row['sourceUrl'],'title':row['sourceTitle'],'language':lang,'license':'CC BY-SA 4.0','changes':'Lead excerpt; references and formatting removed'},'inscriptionYear':year_source,'classification':row['categorySourceUrl'],'photo':dict(image or {},**({'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'source':m['descUrl'],'changes':'Resized to maximum 800 pixels; JPEG conversion'} if image else {}))})
    if '--final' in sys.argv:
        unused=ROOT/'data/cache/unused-photos';unused.mkdir(parents=True,exist_ok=True)
        used={str(r['id'])+'.jpg' for r in out if r['image']}
        for f in (ROOT/'app/src/main/assets/img').glob('*.jpg'):
            if f.name not in used:os.replace(f,unused/f.name)
    out.sort(key=lambda r:int(r['heritageId']))
    result={'schemaVersion':1,'snapshotDate':datetime.date.today().isoformat(),'regions':REGIONS,'entries':out}
    save(str(ROOT/'app/src/main/assets/heritage.json'),result)
    save(str(ROOT/'data/provenance.json'),provenance);save(str(ROOT/'data/exclusions.json'),issues)
    audit={str(r['id']):{k:photos['Q'+str(r['id'])].get(k) for k in ['file','mime','artist','credit','uploader','licenseShort','license','licenseUrl','licenseTemplate','usageTerms','restrictions','descUrl','ok']} for r in out if r['image']}
    save(str(ROOT/'data/photo-license-audit.json'),{'snapshotDate':result['snapshotDate'],'photos':audit})
    stats={'count':len(out),'images':sum(x['image'] is not None for x in out),'categories':dict(Counter(['文化遺産','自然遺産','複合遺産'][x['group']] for x in out)),'languages':dict(Counter(x['sourceLang'] for x in out)),'excluded':len(issues)}
    save(str(ROOT/'data/catalog-summary.json'),stats);print(stats);print('issues',issues[:30])
if __name__=='__main__':main()
