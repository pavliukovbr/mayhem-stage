#!/usr/bin/env python3
"""Gera as texturas dos materiais do palco (32x32, PNG, deterministicas).

Marfim envelhecido com veios, friso entalhado com rosacea em relevo falso
(sombra + brilho pintados), coluna canelada, preto brilhante com sheen,
preto veludo, LED branco e vermelho, folha de ouro. Nada vem de asset da
Mojang: tudo desenhado aqui, entao pode ir para repo publico.
"""
import os, random
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main",
                   "resources", "assets", "mayhem", "textures", "block")
os.makedirs(OUT, exist_ok=True)
S = 32
rng = random.Random(1989)

def noise(im, amp, chance=1.0):
    px = im.load()
    for y in range(S):
        for x in range(S):
            if rng.random() < chance:
                r,g,b,a = px[x,y]
                d = rng.randint(-amp, amp)
                px[x,y] = (max(0,min(255,r+d)), max(0,min(255,g+d)), max(0,min(255,b+d)), a)

def base(c):
    return Image.new("RGBA", (S,S), c+(255,))

def shade(px, x, y, d):
    if 0<=x<S and 0<=y<S:
        r,g,b,a = px[x,y]
        px[x,y] = (max(0,min(255,r+d)), max(0,min(255,g+d)), max(0,min(255,b+d)), a)

IVORY = (226, 216, 194)

def ivory_plain():
    im = base(IVORY); noise(im, 7)
    px = im.load()
    # veios sutis de pedra envelhecida
    for _ in range(5):
        x = rng.randint(0, S-1)
        for y in range(S):
            x = max(0, min(S-1, x + rng.choice((-1,0,0,1))))
            shade(px, x, y, -10)
    # cantos levemente escurecidos (sujeira de idade)
    for i in range(S):
        for d,k in ((0,-8),(1,-4)):
            shade(px,i,d,k); shade(px,i,S-1-d,k); shade(px,d,i,k); shade(px,S-1-d,i,k)
    return im

def ivory_carved():
    im = ivory_plain(); px = im.load(); dr = ImageDraw.Draw(im)
    hi, lo = 24, -34
    # moldura em bisel
    for k in (2, 3):
        for i in range(k, S-k):
            shade(px,i,k,lo); shade(px,k,i,lo)          # sombra em cima/esq
            shade(px,i,S-1-k,hi); shade(px,S-1-k,i,hi)  # luz embaixo/dir
    # rosacea central: petalas em relevo
    cx = cy = S//2
    for ang in range(0, 360, 45):
        import math
        a = math.radians(ang)
        for r_ in range(4, 11):
            x = round(cx + r_*math.cos(a)); y = round(cy + r_*math.sin(a))
            shade(px, x, y, lo)
            shade(px, x+1, y+1, hi)
    dr.ellipse((cx-3, cy-3, cx+3, cy+3), outline=None)
    for dx in range(-2,3):
        for dy in range(-2,3):
            if dx*dx+dy*dy <= 4: shade(px, cx+dx, cy+dy, hi)
    shade(px, cx, cy, lo); shade(px, cx+1, cy, lo)
    return im

def ivory_fluted_side():
    im = base(IVORY); noise(im, 5)
    px = im.load()
    # 8 caneluras de 4px: sombra, meio, luz, meio
    for x in range(S):
        m = x % 4
        d = {0:-26, 1:-6, 2:18, 3:-6}[m]
        for y in range(S):
            shade(px, x, y, d)
    return im

def ivory_fluted_end():
    im = ivory_plain(); px = im.load()
    for k in (1,2):
        for i in range(k, S-k):
            shade(px,i,k,-18); shade(px,k,i,-18); shade(px,i,S-1-k,14); shade(px,S-1-k,i,14)
    return im

def gloss_black():
    im = base((14,14,17)); noise(im, 4)
    px = im.load()
    # sheen diagonal de reflexo
    for t in range(-S, S, 11):
        for i in range(S):
            x, y = i, i+t
            if 0<=y<S:
                shade(px, x, y, 13)
                shade(px, x, y+1, 6)
    return im

def velvet_black():
    im = base((9,9,11)); noise(im, 3)
    return im

def led(c_core, c_edge):
    im = base(c_edge)
    px = im.load()
    for y in range(S):
        for x in range(S):
            d = min(x, y, S-1-x, S-1-y)
            f = min(1.0, d/6.0)
            r = round(c_edge[0]+(c_core[0]-c_edge[0])*f)
            g = round(c_edge[1]+(c_core[1]-c_edge[1])*f)
            b = round(c_edge[2]+(c_core[2]-c_edge[2])*f)
            px[x,y] = (r,g,b,255)
    return im

def gold_leaf():
    im = base((214,171,84)); noise(im, 14)
    px = im.load()
    for _ in range(60):
        shade(px, rng.randint(0,S-1), rng.randint(0,S-1), -34)
    for _ in range(40):
        shade(px, rng.randint(0,S-1), rng.randint(0,S-1), 30)
    return im

TEX = {
    "ivory_plain": ivory_plain, "ivory_carved": ivory_carved,
    "ivory_fluted_side": ivory_fluted_side, "ivory_fluted_end": ivory_fluted_end,
    "gloss_black": gloss_black, "velvet_black": velvet_black,
    "led_white": lambda: led((255,255,255),(200,205,215)),
    "led_red": lambda: led((255,40,48),(140,10,16)),
    "gold_leaf": gold_leaf,
}
for name, fn in TEX.items():
    fn().save(os.path.join(OUT, name + ".png"))
    print("texture", name)
