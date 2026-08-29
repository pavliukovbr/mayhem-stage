"""Pipeline generico de prop: decima, bake de cor e AO por vertice, exporta.

Uso: blender -b --python tools/bake_prop.py -- <nome> <tris_alvo> <altura_em_blocos>
Le sources/<nome>_master.glb e escreve sources/<nome>_game.glb.
Regras aprendidas no castelo: flat shading sempre (suavizadores corrompem
normais em malha decimada), bake de cor com a superficie colada no denso,
AO propria com alcance curto.
"""
import bpy, os, sys

args = sys.argv[sys.argv.index("--")+1:]
NAME, TRIS, HEIGHT = args[0], int(args[1]), float(args[2])
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "sources", NAME + "_master.glb")
DST = os.path.join(ROOT, "sources", NAME + "_game.glb")

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=SRC)
src = [o for o in bpy.context.scene.objects if o.type == 'MESH'][0]

bpy.ops.object.select_all(action='DESELECT')
src.select_set(True); bpy.context.view_layer.objects.active = src
bpy.ops.object.duplicate()
dst = bpy.context.view_layer.objects.active
tris = sum(len(p.vertices)-2 for p in dst.data.polygons)
mod = dst.modifiers.new("dec", 'DECIMATE'); mod.ratio = TRIS/tris
bpy.ops.object.modifier_apply(modifier="dec")

dst.data.color_attributes.new(name="Col", type='BYTE_COLOR', domain='CORNER')
matd = bpy.data.materials.new("baked"); matd.use_nodes = True
dst.data.materials.clear(); dst.data.materials.append(matd)

sc = bpy.context.scene
sc.render.engine = 'CYCLES'
sc.cycles.samples = 4
sc.cycles.device = 'CPU'
sc.render.bake.use_selected_to_active = True
sc.render.bake.cage_extrusion = 0.05
sc.render.bake.max_ray_distance = 0.2
sc.render.bake.target = 'VERTEX_COLORS'
sc.render.bake.use_pass_direct = False
sc.render.bake.use_pass_indirect = False

bpy.ops.object.select_all(action='DESELECT')
src.select_set(True); dst.select_set(True)
bpy.context.view_layer.objects.active = dst
bpy.ops.object.bake(type='DIFFUSE')

ao = dst.data.color_attributes.new(name="AO", type='BYTE_COLOR', domain='CORNER')
dst.data.color_attributes.active_color = ao
sc.render.bake.use_selected_to_active = False
sc.cycles.samples = 32
w = bpy.data.worlds.new("w"); bpy.context.scene.world = w
try: w.light_settings.distance = 0.3
except Exception: pass
bpy.ops.object.select_all(action='DESELECT')
dst.select_set(True); bpy.context.view_layer.objects.active = dst
bpy.ops.object.bake(type='AO')

bpy.data.objects.remove(src, do_unlink=True)
zs = [v.co.z for v in dst.data.vertices]
s = HEIGHT / (max(zs)-min(zs))
dst.scale = (s,s,s)
bpy.ops.object.select_all(action='DESELECT'); dst.select_set(True)
bpy.context.view_layer.objects.active = dst
bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
xs=[v.co.x for v in dst.data.vertices]; ys=[v.co.y for v in dst.data.vertices]; zs=[v.co.z for v in dst.data.vertices]
dst.location = (-(min(xs)+max(xs))/2, -(min(ys)+max(ys))/2, -min(zs))
bpy.ops.object.transform_apply(location=True)
bpy.ops.export_scene.gltf(filepath=DST, export_format='GLB', export_yup=True, export_apply=True)
print("BAKE OK:", NAME, sum(len(p.vertices)-2 for p in dst.data.polygons), "tris")
