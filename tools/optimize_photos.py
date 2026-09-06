#!/usr/bin/env python3
"""Bound offline asset size to fit Google Play's base module download limit."""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
count=0
for p in (ROOT/'app/src/main/assets/img').glob('*.jpg'):
    with Image.open(p) as im:
        if max(im.size)<=800:continue
        im.thumbnail((800,800));im.convert('RGB').save(str(p)+'.tmp',format='JPEG',quality=80,optimize=True)
    Path(str(p)+'.tmp').replace(p);count+=1
print('Optimized photos:',count)
