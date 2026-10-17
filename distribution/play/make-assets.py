"""Render the existing CallGuard vector logo into Google Play asset sizes."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
r=Path(__file__).resolve().parent
G='#08634e';BG='#f5f7f4'
def q(start,control,end):
 return [((1-t)**2*start[0]+2*(1-t)*t*control[0]+t*t*end[0],(1-t)**2*start[1]+2*(1-t)*t*control[1]+t*t*end[1]) for t in [i/20 for i in range(1,21)]]
def shield(im,x,y,size):
 d=ImageDraw.Draw(im);points=[(54,18),(83,30),(80,60)]+q((80,60),(74,80),(54,91))+q((54,91),(34,80),(28,60))+[(25,30)]
 scale=lambda p:(x+p[0]*size/108,y+p[1]*size/108)
 d.polygon([scale(p) for p in points],fill=G)
 check=[scale(p) for p in [(39,53),(50,64),(70,43)]];w=round(size*7/108);d.line(check,fill='white',width=w,joint='curve')
 for a,b in check:d.ellipse((a-w/2,b-w/2,a+w/2,b+w/2),fill='white')
icon=Image.new('RGB',(2048,2048),BG);shield(icon,0,0,2048);icon.resize((512,512),Image.Resampling.LANCZOS).save(r/'icon-512.png')
s=2;banner=Image.new('RGB',(1024*s,500*s),BG);shield(banner,40*s,65*s,350*s);d=ImageDraw.Draw(banner)
font=lambda name,size:ImageFont.truetype('/usr/share/fonts/truetype/open-sans/OpenSans-'+name+'.ttf',size*s)
d.text((410*s,120*s),'CallGuard',font=font('Bold',63),fill=G)
d.text((414*s,220*s),'Aramalarını kendi',font=font('Regular',27),fill='#243e34')
d.text((414*s,262*s),'kurallarınla yönet.',font=font('Regular',27),fill='#243e34')
d.text((415*s,350*s),'Call Guard by AC',font=font('Semibold',19),fill=G)
banner.resize((1024,500),Image.Resampling.LANCZOS).save(r/'feature-1024x500.png')
print('Generated 512x512 icon and 1024x500 feature graphic')
