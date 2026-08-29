#!/usr/bin/env python3
"""Gera TODOS os modulos do Mayhem Stage na escala giga: 3 blocos = 1 metro.

Na escala 1:1 um bloco de 1 m nao consegue mostrar friso, balaustre nem
capitel. Triplicando, a fachada vai a 129 blocos de largura e cada detalhe
das fotos ganha espaco: colunas 2x2 com base e capitel, balaustrada de
postes individuais, cornija em balanco, brasao com escudo e volutas.

Coordenadas de palco ja escaladas; a ancora do mundo continua sendo a linha
unica de build_blockout.mcfunction.
"""
import math, os

OUT = os.path.join(os.path.dirname(__file__), "..", "datapack", "data", "mayhem", "function")

# Materiais do mod mayhem-show: marfim envelhecido, preto brilhante e LED
# emissivo de verdade (lightLevel 15). O datapack passa a DEPENDER do mod.
SMOOTH  = "mayhem:ivory_plain"
CHISEL  = "mayhem:ivory_carved"
PILLAR  = "mayhem:ivory_fluted[axis=y]"
BALUST  = "mayhem:ivory_wall"
SLAB    = "mayhem:ivory_slab[type=bottom]"
BLACKP  = "minecraft:polished_blackstone"
TRUSS   = "minecraft:polished_blackstone_wall"
BLACKC  = "mayhem:gloss_black"
VELVET  = "mayhem:velvet_black"
WHITEC  = "mayhem:led_white"
REDC    = "mayhem:led_red"
GOLD    = "mayhem:gold_leaf"
PANE    = "minecraft:white_stained_glass_pane"
CAP     = "mayhem:ivory_stairs[facing=south,half=top]"     # cornija em balanco
FOOT    = "mayhem:ivory_stairs[facing=south,half=bottom]"  # pe de moldura

def F(x1,y1,z1,x2,y2,z2,b):
    return ("fill",min(x1,x2),min(y1,y2),min(z1,z2),max(x1,x2),max(y1,y2),max(z1,z2),b)
def S(x,y,z,b):
    return ("set",x,y,z,b)

def swap_ew(b):
    return b.replace("east","\0").replace("west","east").replace("\0","west")

def mirror(ops):
    out=[]
    for o in ops:
        if o[0]=="fill":
            _,x1,y1,z1,x2,y2,z2,b=o
            out.append(("fill",-x2,y1,z1,-x1,y2,z2,swap_ew(b)))
        else:
            _,x,y,z,b=o
            out.append(("set",-x,y,z,swap_ew(b)))
    return out

# ---------------------------------------------------------------- pavilhao
# Corda 38, flecha 12 (4 m), raio 21.04. dx -19..18, centro entre -1 e 0.
def zf(dx):
    u = dx + 0.5
    return 27 - round(math.sqrt(21.04**2 - u*u) - 9.04)

COLS = [(-19,-18),(-10,-9),(-1,0),(8,9),(17,18)]   # pares 2x2, simetricos
COL_DX = {d for p in COLS for d in p}

def pavilion_left():
    ops=[]
    for dx in range(-19,19):
        x, z = -45+dx, zf(dx)
        # parapeito do nivel 1 (pedestal ate o piso do camarote)
        ops.append(F(x,0,z,x,4,z+1,SMOOTH))
        ops.append(F(x,2,z,x,3,z,CHISEL))
        ops.append(S(x,5,z,SMOOTH)); ops.append(S(x,5,z+1,SMOOTH))
        ops.append(S(x,5,z-1,CAP))
        # faixa intermediaria (piso do nivel 2)
        ops.append(F(x,15,z,x,17,z+1,SMOOTH))
        ops.append(S(x,16,z,CHISEL))
        ops.append(S(x,15,z-1,FOOT)); ops.append(S(x,17,z-1,CAP))
        # parapeito do nivel 2
        ops.append(F(x,18,z,x,20,z+1,SMOOTH))
        ops.append(S(x,19,z,CHISEL))
        ops.append(S(x,21,z,SMOOTH)); ops.append(S(x,21,z+1,SMOOTH))
        ops.append(S(x,21,z-1,CAP))
        # entablamento e cornija
        ops.append(F(x,30,z,x,32,z+1,SMOOTH))
        ops.append(S(x,31,z,CHISEL))
        ops.append(S(x,33,z,SMOOTH)); ops.append(S(x,33,z+1,SMOOTH))
        ops.append(S(x,33,z-1,CAP))
        ops.append(S(x,34,z,SLAB))
        # balaustrada do nivel 1, so entre as colunas
        if dx not in COL_DX:
            ops.append(S(x,6,z,BALUST))
            ops.append(S(x,7,z,SLAB))
    # colunas 2x2: base, fuste canelado, capitel com flare de stairs
    for d1,d2 in COLS:
        x1,x2 = -45+d1, -45+d2
        z = min(zf(d1),zf(d2))
        for lv0,lv1 in ((6,14),(22,29)):
            ops.append(F(x1,lv0,z,x2,lv0+1,z+1,CHISEL))            # base
            ops.append(F(x1,lv0+2,z,x2,lv1-2,z+1,PILLAR))          # fuste
            ops.append(F(x1,lv1-1,z,x2,lv1,z+1,CHISEL))            # capitel
            for cx_,cz in ((x1-1,z),(x2+1,z),(x1,z-1),(x2,z-1)):
                ops.append(S(cx_,lv1,cz,CAP))                      # flare
    # acroterios nos cantos do topo
    for dx in (-19,18):
        x,z = -45+dx, zf(dx)
        ops.append(F(x,35,z,x,36,z,CHISEL))
        ops.append(S(x,37,z,SLAB))
    # interior: pisos pretos, fundo e torres de trelica
    for dx in range(-18,18):
        x, zi = -45+dx, zf(dx)+2
        if zi <= 26:
            ops.append(F(x,5,zi,x,5,26,BLACKP))
            ops.append(F(x,17,zi,x,17,26,BLACKP))
    ops.append(F(-64,0,27,-27,34,27,BLACKP))                        # fundo
    for d1,d2 in ((-13,-12),(-1,0),(11,12)):
        for d in (d1,d2):
            ops.append(F(-45+d,6,24,-45+d,29,24,TRUSS))
        for yy in (14,26):
            ops.append(F(-45+d1,yy,24,-45+d2,yy,24,BLACKP))
    ops.append(F(-64,0,26,-64,34,27,SMOOTH))                        # fecho externo
    return ops

# ------------------------------------------------------------------- arco
def arch():
    ops=[]
    # ombreiras macicas (3 m de cada lado do vao de 9 m)
    ops.append(F(-22,0,27,-14,35,35,SMOOTH))
    ops.append(F(14,0,27,22,35,35,SMOOTH))
    # emendas com os pavilhoes
    ops.append(F(-26,0,27,-23,35,35,SMOOTH))
    ops.append(F(23,0,27,26,35,35,SMOOTH))
    for sx in (-1,1):
        # pilastra canelada na frente da ombreira
        ops.append(F(sx*17,0,26,sx*19,2,26,CHISEL))
        ops.append(F(sx*17,3,26,sx*19,30,26,PILLAR))
        ops.append(F(sx*17,31,26,sx*19,33,26,CHISEL))
        # moldura vertical do vao
        ops.append(F(sx*14,0,27,sx*14,31,27,CHISEL))
        # chanfro em degraus nos cantos superiores do vao (sugere o arco)
        ops.append(F(sx*11,29,27,sx*13,29,35,SMOOTH))
        ops.append(F(sx*12,28,27,sx*13,28,35,SMOOTH))
        ops.append(F(sx*13,27,27,sx*13,27,35,SMOOTH))
    # verga sobre o vao ate o topo das ombreiras
    ops.append(F(-13,30,27,13,35,35,SMOOTH))
    ops.append(F(-13,31,27,13,31,27,CHISEL))
    # teto escuro do tunel
    ops.append(F(-12,30,31,12,30,35,VELVET))
    # entablamento e cornija atravessando tudo (X -26..26)
    ops.append(F(-26,36,27,26,38,35,SMOOTH))
    ops.append(F(-26,37,27,26,37,27,CHISEL))
    ops.append(F(-26,39,27,26,40,35,SMOOTH))
    ops.append(F(-26,40,26,26,40,26,CAP))
    ops.append(F(-26,41,27,26,41,35,SMOOTH))
    # brasao: pedestal, volutas, escudo dourado, querubins e figura
    ops.append(F(-8,42,27,8,43,29,SMOOTH))
    ops.append(F(-4,42,27,4,43,27,CHISEL))
    for sx in (-1,1):
        ops.append(S(sx*9,42,27,FOOT))
        ops.append(S(sx*10,42,27,FOOT))
    ops.append(F(-3,44,27,3,49,28,SMOOTH))
    ops.append(F(-2,45,27,2,48,27,GOLD))
    ops.append(F(-3,44,27,3,44,27,CHISEL))
    ops.append(F(-3,49,27,3,49,27,CHISEL))
    for sx in (-1,1):
        ops.append(F(sx*5,44,27,sx*6,47,28,SMOOTH))       # querubim
        ops.append(S(sx*5,48,27,CHISEL))                  # cabeca
        ops.append(S(sx*7,46,27,FOOT))                    # asa
        ops.append(S(sx*8,45,27,FOOT))
    ops.append(F(-1,50,27,1,50,27,CHISEL))
    # cortina de fios: 13 fios de vidro, um a cada 2 blocos
    for x in range(-12,13,2):
        ops.append(F(x,0,30,x,28,30,PANE))
    # fundo preto do tunel
    ops.append(F(-14,0,36,14,35,36,VELVET))
    return ops

# ----------------------------------------------------------------- resto
# O palco real e elevado ~2 m: seis camadas de bloco, fascia preta fosca,
# topo preto brilhante e friso de LED vermelho na borda frontal.
DECK_TOP = 4   # topo do deck; jogador pisa em y=5

def deck():
    ops=[]
    for y in range(-1, DECK_TOP):
        ops.append(F(-72,y,0,72,y,53,VELVET))
    ops.append(F(-72,DECK_TOP,0,72,DECK_TOP,53,BLACKC))
    # friso de LED vermelho na borda frontal do topo, aberto na passarela
    ops.append(F(-72,DECK_TOP,0,-14,DECK_TOP,0,REDC))
    ops.append(F(14,DECK_TOP,0,72,DECK_TOP,0,REDC))
    return ops

def plinths():
    ops=[]
    for sx in (-1,1):
        ops.append(F(sx*16,5,3,sx*24,9,11,SMOOTH))
        ops.append(F(sx*16,7,3,sx*24,8,3,CHISEL))
        ops.append(F(sx*16,10,3,sx*24,10,11,SLAB))
    return ops

# passarela elevada terminando em crescente: circulo central + bracos em
# anel que curvam de volta na direcao do palco, como na vista aerea
import math as _m
ZC, RC, RIN, ROUT, ZARM = -104, 15, 33, 51, -64

def _in_head(x, z):
    d1 = _m.hypot(x, z - ZC)
    if d1 <= RC: return True
    return RIN <= d1 <= ROUT and z <= ZARM

def runway():
    ops=[]
    for y in range(-1, DECK_TOP):
        ops.append(F(-13,y,-90,13,y,-1,VELVET))
    ops.append(F(-12,DECK_TOP,-90,12,DECK_TOP,-1,BLACKC))
    ops.append(F(-13,DECK_TOP,-90,-13,DECK_TOP,-1,REDC))
    ops.append(F(13,DECK_TOP,-90,13,DECK_TOP,-1,REDC))
    for z in range(-53, -156, -1):
        runs=[]
        x=-52
        while x <= 52:
            if _in_head(x, z):
                x0=x
                while x <= 52 and _in_head(x, z): x += 1
                runs.append((x0, x-1))
            else:
                x += 1
        for x0,x1 in runs:
            for y in range(-1, DECK_TOP):
                ops.append(F(x0,y,z,x1,y,z,VELVET))
            for x2 in range(x0, x1+1):
                border = (not _in_head(x2-1,z)) or (not _in_head(x2+1,z)) \
                      or (not _in_head(x2,z-1)) or (not _in_head(x2,z+1))
                ops.append(S(x2,DECK_TOP,z, REDC if border else BLACKC))
    return ops

def led_wall():
    # nasce no nivel do deck, com fascia ate o chao
    ops=[F(-94,-1,54,94,4,56,BLACKP)]
    ops.append(F(-94,5,55,94,58,56,VELVET))
    ops.append(F(-93,6,54,93,57,54,VELVET))
    ops.append(F(-94,5,54,94,5,54,WHITEC))
    ops.append(F(-94,58,54,94,58,54,WHITEC))
    ops.append(F(-94,5,54,-94,58,54,WHITEC))
    ops.append(F(94,5,54,94,58,54,WHITEC))
    ops.append(F(-94,59,54,94,59,56,BLACKP))
    for x in range(-92,93,4):
        ops.append(S(x,60,55,REDC))
    return ops

def clear():
    ops=[]
    for y in range(-1,63):
        ops.append(F(-96,y,-160,96,y,-50,"minecraft:air"))
        ops.append(F(-96,y,-49,96,y,62,"minecraft:air"))
    return ops

# ------------------------------------------------------------------ saida
def render(ops):
    out=[]
    for o in ops:
        if o[0]=="fill":
            _,x1,y1,z1,x2,y2,z2,b=o
            out.append(f"fill ~{x1} ~{y1} ~{z1} ~{x2} ~{y2} ~{z2} {b}")
        else:
            _,x,y,z,b=o
            out.append(f"setblock ~{x} ~{y} ~{z} {b}")
    return out

def write(name, header, ops):
    with open(os.path.join(OUT,"blockout",name+".mcfunction"),"w") as f:
        f.write(f"# {header}\n# GERADO por tools/gen_castle.py; nao editar a mao.\n")
        f.write("\n".join(render(ops))+"\n")
    print(f"{name:24s} {len(ops):5d} comandos")

left = pavilion_left()
write("castle_arch","Arco central com pilastras, brasao, querubins e cortina de fios.",arch())
write("castle_pavilion_left","Pavilhao curvo esquerdo: 2 niveis, colunas 2x2, balaustrada.",left)
write("castle_pavilion_right","Pavilhao curvo direito: espelho exato do esquerdo.",mirror(left))
write("deck","Deck principal 145x54 (48x18 m na escala 3:1).",deck())
write("plinths","Dois plintos de marfim ao lado da boca da passarela.",plinths())
write("runway","Passarela e cabeca arredondada com borda vermelha de 1 bloco.",runway())
write("led_wall","Telao 189x54 com moldura branca e luzes vermelhas no topo.",led_wall())
write("clear","Limpa o envelope do palco. Chamado so por mayhem:demolish.",clear())
