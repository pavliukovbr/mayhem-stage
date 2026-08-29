"""Decima, re-UV e faz bake do castelo: high-poly 3.1M -> game mesh 150k.

O atlas do Meshy e por micro-ilha; decimar quebra os UVs. Aqui o modelo
decimado ganha UV novo (Smart UV Project) e recebe a cor do original por
bake selected->active. Sai: sources/castle_game.glb + baked 2048 direto
nos assets do mod.
"""
import bpy

SRC = "/Users/joseph/Desktop/CLAUDE-CODE/mayhem-stage/sources/castle_master.glb"
DST = "/Users/joseph/Desktop/CLAUDE-CODE/mayhem-stage/sources/castle_game.glb"
TEXOUT = "/Users/joseph/Desktop/CLAUDE-CODE/mayhem-stage/mod/src/client/resources/assets/mayhem/castle/castle_basecolor.png"

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=SRC)
src = [o for o in bpy.context.scene.objects if o.type == 'MESH'][0]

# copia decimada
bpy.ops.object.select_all(action='DESELECT')
src.select_set(True); bpy.context.view_layer.objects.active = src
bpy.ops.object.duplicate()
dst = bpy.context.view_layer.objects.active
tris = sum(len(p.vertices)-2 for p in dst.data.polygons)
mod = dst.modifiers.new("dec", 'DECIMATE'); mod.ratio = 150000/tris
bpy.ops.object.modifier_apply(modifier="dec")

# alvo do bake: cor por vertice (UV de decimado vira confete de ilhas
# sub-texel; 213k vertices carregam o marfim melhor que qualquer atlas)
dst.data.color_attributes.new(name="Col", type='BYTE_COLOR', domain='CORNER')
matd = bpy.data.materials.new("baked"); matd.use_nodes = True
dst.data.materials.clear(); dst.data.materials.append(matd)

# bake cycles, cor difusa apenas
sc = bpy.context.scene
sc.render.engine = 'CYCLES'
sc.cycles.samples = 4
sc.cycles.device = 'CPU'  # bake Metal com 1 sample perde raios; CPU e deterministico
sc.render.bake.use_selected_to_active = True
sc.render.bake.cage_extrusion = 0.35
sc.render.bake.max_ray_distance = 1.2
sc.render.bake.target = 'VERTEX_COLORS'
sc.render.bake.use_pass_direct = False
sc.render.bake.use_pass_indirect = False

bpy.ops.object.select_all(action='DESELECT')
src.select_set(True); dst.select_set(True)
bpy.context.view_layer.objects.active = dst
bpy.ops.object.bake(type='DIFFUSE')

# segundo passe: ambient occlusion do proprio decimado, por vertice.
# E o que faz o entalhe aparecer numa superficie monocromatica.
ao = dst.data.color_attributes.new(name="AO", type='BYTE_COLOR', domain='CORNER')
dst.data.color_attributes.active_color = ao
sc.render.bake.use_selected_to_active = False
sc.cycles.samples = 32
bpy.context.scene.world = bpy.data.worlds.new("w")
bpy.ops.object.select_all(action='DESELECT')
dst.select_set(True); bpy.context.view_layer.objects.active = dst
bpy.ops.object.bake(type='AO')


# escala real, origem no chao, exporta so a mesh decimada
bpy.data.objects.remove(src, do_unlink=True)
s = (15.2/15.0)*3.0
dst.scale = (s,s,s)
bpy.ops.object.select_all(action='DESELECT'); dst.select_set(True)
bpy.context.view_layer.objects.active = dst
bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
xs=[v.co.x for v in dst.data.vertices]; ys=[v.co.y for v in dst.data.vertices]; zs=[v.co.z for v in dst.data.vertices]
dst.location = (-(min(xs)+max(xs))/2, -(min(ys)+max(ys))/2, -min(zs))
bpy.ops.object.transform_apply(location=True)
bpy.ops.export_scene.gltf(filepath=DST, export_format='GLB', export_yup=True, export_apply=True)
print("BAKE OK: mesh", sum(len(p.vertices)-2 for p in dst.data.polygons), "tris")
