#!/usr/bin/env python3
"""Gaiola dourada parametrica, escrita direto no bin v3 do mod.

Barras curvas de uma base circular convergindo numa cupula, tres aneis
horizontais, final no topo. Ouro com sombra falsa por altura. 2.5 m de
altura na escala 3:1 = 7.5 blocos.
"""
import math, struct, io

H, R = 7.5, 3.0            # altura e raio em blocos
BARS, SIDES, SEGS = 14, 6, 24
GOLD = (0.55, 0.34, 0.09)  # linear

verts, idxs = [], []

def tube(path, radius):
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
            shade = 0.75 + 0.25*(y/H)          # sombra falsa: base mais escura
            verts.append((x+nx*radius, y+ny*radius, z+nz*radius,
                          nx, ny, nz,
                          GOLD[0]*shade, GOLD[1]*shade, GOLD[2]*shade, 1.0))
    for i in range(len(path)-1):
        for k in range(SIDES):
            a=base+i*SIDES+k; b=base+i*SIDES+(k+1)%SIDES
            c=base+(i+1)*SIDES+(k+1)%SIDES; d=base+(i+1)*SIDES+k
            idxs.extend((a,b,c, a,c,d))

# barras: sobem retas e fecham em cupula no topo
for b in range(BARS):
    ang = 2*math.pi*b/BARS
    path=[]
    for s in range(SEGS+1):
        t = s/SEGS
        r = R if t < 0.72 else R*math.cos((t-0.72)/0.28*math.pi/2)
        path.append((r*math.cos(ang), H*t, r*math.sin(ang)))
    tube(path, 0.10)
# aneis
for hy, rr in ((0.04,R),(0.5,R),(0.72,R)):
    ring=[]
    for s in range(49):
        a=2*math.pi*s/48
        ring.append((rr*math.cos(a), H*hy, rr*math.sin(a)))
    tube(ring, 0.09)
# final do topo
tube([(0,H,0),(0,H+0.5,0)], 0.16)

out=io.BytesIO()
out.write(struct.pack('<III',0x4D534D33,len(verts),len(idxs)))
for v in verts: out.write(struct.pack('<10f',*v))
for i in idxs: out.write(struct.pack('<I',i))
open('mod/src/client/resources/assets/mayhem/meshes/cage.bin','wb').write(out.getvalue())
print(f"cage.bin: {len(verts)} verts, {len(idxs)//3} tris")
