#!/usr/bin/env python3
"""castle_game.glb -> castle_mesh.bin v3: pos3f nrm3f rgb3f ao1f (40 B/vertice).

COLOR_1 = cor difusa bakeada do high-poly (linear); COLOR_2 = AO propria do
decimado. COLOR_0 e o branco herdado do Meshy e e ignorado.

Uso: python3 tools/glb_to_mesh.py <nome>   (default: castle)
"""
import json, struct, io, os, sys
NAME = sys.argv[1] if len(sys.argv) > 1 else "castle"
TINT = None
if "--tint" in sys.argv:
    TINT = [float(v) for v in sys.argv[sys.argv.index("--tint")+1].split(",")]
f=open(f'sources/{NAME}_game.glb','rb').read()
jlen,_=struct.unpack('<II',f[12:20]); g=json.loads(f[20:20+jlen]); boff=20+jlen+8
def buf(i):
    v=g['bufferViews'][i]; s=boff+v.get('byteOffset',0); return f[s:s+v['byteLength']]
def acc(i):
    a=g['accessors'][i]; d=buf(a['bufferView']); o=a.get('byteOffset',0); n=a['count']
    c={5126:('f',4),5125:('I',4),5123:('H',2),5121:('B',1)}[a['componentType']]
    k={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']]
    vals=struct.unpack_from('<'+c[0]*n*k,d,o)
    if a.get('normalized') and c[0]=='B': vals=[v/255.0 for v in vals]
    if a.get('normalized') and c[0]=='H': vals=[v/65535.0 for v in vals]
    return [tuple(vals[i*k:(i+1)*k]) for i in range(n)]
p=g['meshes'][0]['primitives'][0]
print('atributos:', sorted(p['attributes']))
pos=acc(p['attributes']['POSITION']); nrm=acc(p['attributes']['NORMAL'])
col=acc(p['attributes']['COLOR_1']); ao=acc(p['attributes']['COLOR_2'])
idx=[i[0] for i in acc(p['indices'])]
import statistics
print('AO media:', round(statistics.mean(a[0] for a in ao[::701]),3))
out=io.BytesIO(); out.write(struct.pack('<III',0x4D534D33,len(pos),len(idx)))
for P,N,C,A in zip(pos,nrm,col,ao):
    c = C
    if TINT:
        # recolore por luminancia: sombra do bake permanece, matiz vira o alvo
        luma = min(1.5, (0.2126*C[0]+0.7152*C[1]+0.0722*C[2]) / 0.30)
        c = (TINT[0]*luma, TINT[1]*luma, TINT[2]*luma)
    out.write(struct.pack('<10f',P[0],P[1],P[2],N[0],N[1],N[2],c[0],c[1],c[2],A[0]))
for i in idx: out.write(struct.pack('<I',i))
outdir='mod/src/client/resources/assets/mayhem/meshes'
os.makedirs(outdir, exist_ok=True)
open(f'{outdir}/{NAME}.bin','wb').write(out.getvalue())
print(f'{NAME}.bin v3:', len(pos),'verts,', len(idx)//3,'tris')
