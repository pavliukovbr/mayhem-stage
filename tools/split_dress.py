"""Corta o vestido bakeado em duas metades (x<0 e x>0), mantendo a origem.

Fechadas, as metades se encostam sem emenda; abrir o vestido no show e
afastar cada uma para o seu lado. Corre sobre o *_game.glb ja decimado e
bakeado, entao cor e AO por vertice sobrevivem ao corte.
"""
import bpy, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

for side, keep_positive in (("l", False), ("r", True)):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=os.path.join(ROOT, "sources", "dress_game.glb"))
    o = [x for x in bpy.context.scene.objects if x.type == 'MESH'][0]
    bpy.context.view_layer.objects.active = o; o.select_set(True)
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.mesh.bisect(plane_co=(0,0,0), plane_no=(1,0,0),
                        clear_inner=keep_positive, clear_outer=not keep_positive)
    bpy.ops.object.mode_set(mode='OBJECT')
    bpy.ops.export_scene.gltf(filepath=os.path.join(ROOT, "sources", f"dress_{side}_game.glb"),
                              export_format='GLB', export_yup=True, export_apply=True)
    print("SPLIT OK", side, sum(len(p.vertices)-2 for p in o.data.polygons), "tris")
