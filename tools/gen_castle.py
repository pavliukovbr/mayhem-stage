#!/usr/bin/env python3
"""Gera os modulos do castelo (fachada barroca) do Mayhem Stage.

O castelo real tem tres pecas: pavilhao curvo de dois niveis a esquerda,
arco central com brasao, pavilhao curvo a direita. Este gerador produz as
tres em blocos vanilla, na escala 1 bloco = 1 metro, em coordenadas de
palco (origem no centro da frente do deck, +Z para o backstage).

A curva dos pavilhoes e um arco de circulo com corda 13 e flecha 4
(raio 7.3). O pavilhao direito e o espelho exato do esquerdo, com
facing=east/west trocados nos stairs.
"""
import math, os

OUT = os.path.join(os.path.dirname(__file__), "..", "datapack", "data", "mayhem", "function")

SMOOTH = "minecraft:smooth_quartz"
CHISEL = "minecraft:chiseled_quartz_block"
PILLAR = "minecraft:quartz_pillar[axis=y]"
BALUST = "minecraft:diorite_wall"
BLACKP = "minecraft:polished_blackstone"
TRUSS  = "minecraft:polished_blackstone_wall"
BLACKC = "minecraft:black_concrete"
GOLD   = "minecraft:gold_block"
PANE   = "minecraft:white_stained_glass_pane"
CORNICE = "minecraft:quartz_stairs[facing=south,half=top]"

def zf(dx):
    """Recuo da frente curva: corda 13, flecha 4, raio 7.3."""
    return 9 - round(math.sqrt(7.3**2 - dx*dx) - 3.3)

def op_fill(x1,y1,z1,x2,y2,z2,b): return ("fill",x1,y1,z1,x2,y2,z2,b)
def op_set(x,y,z,b): return ("set",x,y,z,b)

def pavilion_left():
    cx = -15
    cols = {-6,-3,0,3,6}
    ops = []
    for dx in range(-6, 7):
        x, z = cx+dx, zf(dx)
        # faixas horizontais: base (0-1), piso do nivel 2 (5), entablamento (9-10)
        for y in (0,1,5,9,10):
            ops.append(op_fill(x,y,z,x,y,z+1,SMOOTH))
        for y in (1,5,9):
            ops.append(op_set(x,y,z,CHISEL))          # frente entalhada
        ops.append(op_set(x,10,z-1,CORNICE))          # cornija em balanco
        # nivel 1 (2-4) e nivel 2 (6-8): coluna ou guarda-corpo
        for base in (2,6):
            if dx in cols:
                ops.append(op_fill(x,base,z,x,base+2,z,PILLAR))
            else:
                ops.append(op_set(x,base,z,BALUST))
        if dx in (-6,6):
            ops.append(op_set(x,11,z,CHISEL))         # acroterio dos cantos
    # interior: pisos pretos e trelica, atras da frente curva
    for dx in range(-5, 6):
        x, zi = cx+dx, zf(dx)+2
        if zi <= 8:
            for y in (1,5):
                ops.append(op_fill(x,y,zi,x,y,8,BLACKP))
    for tx in (cx-3, cx, cx+3):
        ops.append(op_fill(tx,2,8,tx,8,8,TRUSS))
    # fundo preto, pilar de fechamento externo e emenda com o arco
    ops.append(op_fill(cx-5,0,9,cx+5,9,9,BLACKP))
    ops.append(op_fill(cx-6,0,9,cx-6,10,9,SMOOTH))
    ops.append(op_fill(-8,0,8,-8,10,9,SMOOTH))
    return ops

def mirror(ops):
    out = []
    for o in ops:
        if o[0] == "fill":
            _,x1,y1,z1,x2,y2,z2,b = o
            out.append(("fill",-x2,y1,z1,-x1,y2,z2,swap_ew(b)))
        else:
            _,x,y,z,b = o
            out.append(("set",-x,y,z,swap_ew(b)))
    return out

def swap_ew(b):
    return b.replace("east","\0").replace("west","east").replace("\0","west")

def arch():
    ops = []
    # ombreiras macicas, 3 m de profundidade
    ops.append(op_fill(-7,0,9,-5,10,11,SMOOTH))
    ops.append(op_fill(5,0,9,7,10,11,SMOOTH))
    # pilastras na frente das ombreiras
    for sx in (-6,6):
        ops.append(op_set(sx,0,8,CHISEL))
        ops.append(op_fill(sx,1,8,sx,8,8,PILLAR))
        ops.append(op_set(sx,9,8,CHISEL))
    # moldura interna do vao (vao: X -4..4, Y 0..9)
    ops.append(op_fill(-5,0,9,-5,9,9,CHISEL))
    ops.append(op_fill(5,0,9,5,9,9,CHISEL))
    # verga e sugestao de arco nos cantos superiores do vao
    ops.append(op_fill(-4,10,9,4,10,11,SMOOTH))
    ops.append(op_fill(-4,10,9,4,10,9,CHISEL))
    ops.append(op_set(-4,9,9,"minecraft:quartz_stairs[facing=west,half=top]"))
    ops.append(op_set(4,9,9,"minecraft:quartz_stairs[facing=east,half=top]"))
    # entablamento e cornija atravessando arco + emendas (X -8..8)
    ops.append(op_fill(-8,11,9,8,11,11,SMOOTH))
    ops.append(op_fill(-8,11,9,8,11,9,CHISEL))
    ops.append(op_fill(-8,12,9,8,12,11,SMOOTH))
    ops.append(op_fill(-8,12,8,8,12,8,CORNICE))
    # brasao: base, escudo dourado, volutas e figura no topo
    ops.append(op_fill(-3,13,9,3,13,10,SMOOTH))
    ops.append(op_fill(-1,14,9,1,15,9,GOLD))
    ops.append(op_set(-2,14,9,CHISEL))
    ops.append(op_set(2,14,9,CHISEL))
    ops.append(op_set(-3,14,9,"minecraft:quartz_stairs[facing=east,half=bottom]"))
    ops.append(op_set(3,14,9,"minecraft:quartz_stairs[facing=west,half=bottom]"))
    ops.append(op_set(0,16,9,CHISEL))
    # cortina de fios: paineis finos isolados, um sim um nao
    for x in (-4,-2,0,2,4):
        ops.append(op_fill(x,0,10,x,8,10,PANE))
    # tunel escuro atras da cortina
    ops.append(op_fill(-5,0,12,5,10,12,BLACKC))
    return ops

def plinths():
    ops = []
    for x1,x2 in ((-8,-6),(6,8)):
        ops.append(op_fill(x1,0,1,x2,1,3,SMOOTH))
        ops.append(op_fill(x1,1,1,x2,1,1,CHISEL))
    return ops

def clear():
    # UNICA funcao que remove blocos: limpa o envelope do palco para
    # reconstruir. X -32..32, Z -44..20, Y -1..19, uma camada por fill
    # para ficar abaixo do limite de 32768 blocos por comando.
    return [op_fill(-32,y,-44,32,y,20,"minecraft:air") for y in range(-1,20)]

def render(ops):
    lines = []
    for o in ops:
        if o[0] == "fill":
            _,x1,y1,z1,x2,y2,z2,b = o
            lines.append(f"fill ~{x1} ~{y1} ~{z1} ~{x2} ~{y2} ~{z2} {b}")
        else:
            _,x,y,z,b = o
            lines.append(f"setblock ~{x} ~{y} ~{z} {b}")
    return lines

def write(name, header, ops):
    path = os.path.join(OUT, "blockout", name + ".mcfunction")
    with open(path, "w") as f:
        f.write(f"# {header}\n# GERADO por tools/gen_castle.py; nao editar a mao.\n")
        f.write("\n".join(render(ops)) + "\n")
    print(f"{name:24s} {len(ops):4d} comandos")

left = pavilion_left()
write("castle_arch", "Arco central: ombreiras, pilastras, brasao e cortina de fios.", arch())
write("castle_pavilion_left", "Pavilhao curvo esquerdo: dois niveis, colunas e trelica preta.", left)
write("castle_pavilion_right", "Pavilhao curvo direito: espelho exato do esquerdo.", mirror(left))
write("plinths", "Dois plintos baixos de marfim ao lado da boca da passarela.", plinths())
write("clear", "Limpa o envelope do palco. Chamado so por mayhem:demolish.", clear())

with open(os.path.join(OUT, "demolish.mcfunction"), "w") as f:
    f.write("# Demole o palco inteiro (e SO o palco) para reconstruir do zero.\n"
            "# Mesma ancora do build_blockout: mudou la, mude aqui.\n"
            "execute positioned 0.0 -59.0 0.0 run function mayhem:blockout/clear\n")
print("demolish.mcfunction ok")
