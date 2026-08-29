#!/usr/bin/env python3
"""castle_game.glb -> assets do mod: mesh binaria + basecolor PNG 2048.

Formato mesh.bin (little-endian):
  int32 magic 0x4D594D53 ("MSMY"), int32 vertexCount, int32 indexCount
  vertexCount x (pos xyz f32, normal xyz f32, uv f32x2)  -- 32 bytes/vertice
  indexCount x uint32
"""
import json, struct, io, os
from PIL import Image

SRC = "sources/castle_game.glb"
OUT = "mod/src/client/resources/assets/mayhem/castle"
os.makedirs(OUT, exist_ok=True)

f = open(SRC, "rb").read()
_,_,_ = struct.unpack("<III", f[:12])
jlen, _ = struct.unpack("<II", f[12:20])
g = json.loads(f[20:20+jlen])
boff = 20 + jlen + 8
def buf(view_i):
    v = g["bufferViews"][view_i]
    s = boff + v.get("byteOffset", 0)
    return f[s:s+v["byteLength"]]
def acc(i):
    a = g["accessors"][i]
    data = buf(a["bufferView"])
    o = a.get("byteOffset", 0)
    n = a["count"]
    comp = {5126:("f",4), 5125:("I",4), 5123:("H",2)}[a["componentType"]]
    ncomp = {"SCALAR":1,"VEC2":2,"VEC3":3}[a["type"]]
    vals = struct.unpack_from("<"+comp[0]*n*ncomp, data, o)
    return [vals[i*ncomp:(i+1)*ncomp] for i in range(n)], a

prim = g["meshes"][0]["primitives"][0]
pos,_ = acc(prim["attributes"]["POSITION"])
nrm,_ = acc(prim["attributes"]["NORMAL"])
uv,_  = acc(prim["attributes"]["TEXCOORD_0"])
idx,_ = acc(prim["indices"])
idx = [i[0] for i in idx]
assert len(pos)==len(nrm)==len(uv)

out = io.BytesIO()
out.write(struct.pack("<III", 0x4D594D53, len(pos), len(idx)))
for p,n,t in zip(pos,nrm,uv):
    out.write(struct.pack("<8f", p[0],p[1],p[2], n[0],n[1],n[2], t[0],t[1]))
for i in idx:
    out.write(struct.pack("<I", i))
open(os.path.join(OUT,"castle_mesh.bin"),"wb").write(out.getvalue())

# basecolor: material -> pbr -> baseColorTexture -> image
mat = g["materials"][0]
ti = mat["pbrMetallicRoughness"]["baseColorTexture"]["index"]
img_i = g["textures"][ti]["source"]
img = Image.open(io.BytesIO(buf(g["images"][img_i]["bufferView"])))
print("basecolor original:", img.size, g["images"][img_i].get("mimeType"))
img = img.convert("RGBA").resize((2048,2048), Image.LANCZOS)
img.save(os.path.join(OUT,"castle_basecolor.png"))

mb = os.path.getsize(os.path.join(OUT,'castle_mesh.bin'))/2**20
print(f"mesh.bin {mb:.1f} MB, {len(pos)} vertices, {len(idx)//3} tris")
