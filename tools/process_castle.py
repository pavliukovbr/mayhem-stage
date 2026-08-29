"""Roda DENTRO do Blender: decima o castelo do Meshy e exporta o GLB do jogo.

- Alvo ~150k triangulos (de 3.14M): silhueta intacta, detalhe fino fica
  para o normal map do material original (o Meshy ja embute as texturas).
- Escala 3.04: 15.0 m do modelo -> 15.2 m reais -> x3 blocos por metro.
- Origem no chao, centro X/Z: casa com a ancora do palco no mundo.
"""
import bpy, sys

SRC = "/Users/joseph/Desktop/CLAUDE-CODE/mayhem-stage/sources/castle_master.glb"
DST = "/Users/joseph/Desktop/CLAUDE-CODE/mayhem-stage/sources/castle_game.glb"
TARGET_TRIS = 150_000

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=SRC)
objs = [o for o in bpy.context.scene.objects if o.type == 'MESH']
assert objs, "nenhuma mesh importada"
o = objs[0]
bpy.context.view_layer.objects.active = o
o.select_set(True)

tris = sum(len(p.vertices) - 2 for p in o.data.polygons)
ratio = TARGET_TRIS / tris
mod = o.modifiers.new("decimate", 'DECIMATE')
mod.ratio = ratio
bpy.ops.object.modifier_apply(modifier="decimate")
tris2 = sum(len(p.vertices) - 2 for p in o.data.polygons)

# escala real e origem no centro do chao
s = (15.2 / 15.0) * 3.0
o.scale = (s, s, s)
bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
xs = [v.co.x for v in o.data.vertices]; ys=[v.co.y for v in o.data.vertices]
zs = [v.co.z for v in o.data.vertices]
o.location = (-(min(xs)+max(xs))/2, -(min(ys)+max(ys))/2, -min(zs))
bpy.ops.object.transform_apply(location=True)

bpy.ops.export_scene.gltf(filepath=DST, export_format='GLB',
                          export_yup=True, export_apply=True)
print(f"DECIMADO {tris} -> {tris2} tris, escala {s:.3f}, exportado {DST}")
