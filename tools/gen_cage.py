#!/usr/bin/env python3
"""Gaiola de aco em grade cilindrica + elevador central, no bin v3 do mod.

Como nas fotos do show: cilindro de barras verticais densas com aneis
horizontais formando andares de celas, em aco escovado. A porta e um setor
que desliza em volta do centro (mesma origem). O elevador e um disco que
sobe pelo eixo ate o topo do vestido.
"""
import math, struct, io

H, R = 22.0, 8.0
BARS, SIDES = 28, 6
RINGS = (0.3, 5.5, 11.0, 16.5, 21.7)
STEEL = (0.40, 0.41, 0.44)
DOOR_HALF = math.radians(26)
FRONT = math.pi/2

verts, idxs = [], []

def tube(path, radius, col):
    base = len(verts)
    for i,(x,y,z) in enumerate(path):
        if i+1 < len(path):
            dx,dy,dz = (path[i+1][j]-path[i][j] for j in range(3))
        else:
            dx,dy,dz = (path[i][j]-path[i-1][j] for j in range(3))
        L = math.sqrt(dx*dx+dy*dy+dz*dz) or 1
        dx,dy,dz = dx/L,dy/L,dz/L
        ux,uy,uz = (0,0,1) if abs(dy)>0.9 else (0,1,0)
        vx,vy,vz = uy*dz-uz*dy, uz*dx-ux*dz, ux*dy-uy*dx
        L2=math.sqrt(vx*vx+vy*vy+vz*vz) or 1; vx,vy,vz=vx/L2,vy/L2,vz/L2
        wx,wy,wz = dy*vz-dz*vy, dz*vx-dx*vz, dx*vy-dy*vx
        for k in range(SIDES):
            a = 2*math.pi*k/SIDES
            nx = vx*math.cos(a)+wx*math.sin(a)
            ny = vy*math.cos(a)+wy*math.sin(a)
            nz = vz*math.cos(a)+wz*math.sin(a)
            shade = 0.8 + 0.2*(y/H)
            verts.append((x+nx*radius, y+ny*radius, z+nz*radius, nx, ny, nz,
                          col[0]*shade, col[1]*shade, col[2]*shade, 1.0))
    for i in range(len(path)-1):
        for k in range(SIDES):
            a=base+i*SIDES+k; b=base+i*SIDES+(k+1)%SIDES
            c=base+(i+1)*SIDES+(k+1)%SIDES; d=base+(i+1)*SIDES+k
            idxs.extend((a,b,c, a,c,d))

def in_door(ang):
    d = (ang - FRONT + math.pi) % (2*math.pi) - math.pi
    return abs(d) <= DOOR_HALF

def build(door):
    verts.clear(); idxs.clear()
    for b in range(BARS):
        ang = 2*math.pi*b/BARS
        if in_door(ang) != door: continue
        tube([(R*math.cos(ang), 0, R*math.sin(ang)),
              (R*math.cos(ang), H, R*math.sin(ang))], 0.09, STEEL)
    for hy in RINGS:
        arc=[]
        for s in range(121):
            a=2*math.pi*s/120
            if in_door(a) != door:
                if len(arc) > 1: tube(arc, 0.11, STEEL)
                arc=[]
                continue
            arc.append((R*math.cos(a), hy, R*math.sin(a)))
        if len(arc) > 1: tube(arc, 0.11, STEEL)

def write(name):
    out=io.BytesIO()
    out.write(struct.pack('<III',0x4D534D33,len(verts),len(idxs)))
    for v in verts: out.write(struct.pack('<10f',*v))
    for i in idxs: out.write(struct.pack('<I',i))
    open(f'mod/src/client/resources/assets/mayhem/meshes/{name}.bin','wb').write(out.getvalue())
    print(f"{name}.bin: {len(verts)} verts, {len(idxs)//3} tris")

build(False); write("cage")
build(True);  write("cage_door")

# elevador: disco com borda dourada e piso escuro
verts.clear(); idxs.clear()
LR = 2.6
ring=[(LR*math.cos(2*math.pi*s/48), 0.25, LR*math.sin(2*math.pi*s/48)) for s in range(49)]
tube(ring, 0.22, (0.55, 0.34, 0.09))
base=len(verts)
verts.append((0, 0.42, 0, 0,1,0, 0.09,0.09,0.10, 1.0))
for s in range(49):
    a=2*math.pi*s/48
    verts.append((LR*math.cos(a), 0.42, LR*math.sin(a), 0,1,0, 0.11,0.11,0.12, 1.0))
for s in range(48):
    idxs.extend((base, base+1+s, base+2+s))
write("lift")
