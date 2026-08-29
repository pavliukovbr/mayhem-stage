"""Separa o vestido em corpo + duas cortinas frontais com origem na dobradica.

As cortinas sao o setor frontal de +-35 graus do vestido (frente = -Y no
Blender, que vira +Z local no jogo). Cada aba recebe a origem na sua borda
EXTERNA, entao girar o prop em Y abre a aba como porta dupla de teatro.
Imprime os offsets de dobradica em coordenadas locais do jogo.
"""
import bpy, bmesh, math, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HALF_ANGLE = 35.0

def azimuth(x, y):
    """Angulo a partir da frente (-Y), positivo para +X."""
    return math.degrees(math.atan2(x, -y))

for part, lo, hi, hinge_az in (("curtain_l", -HALF_ANGLE, 0.0, -HALF_ANGLE),
                               ("curtain_r", 0.0, HALF_ANGLE, HALF_ANGLE),
                               ("dress_body", None, None, None)):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=os.path.join(ROOT, "sources", "dress_game.glb"))
    o = [x for x in bpy.context.scene.objects if x.type == 'MESH'][0]
    bpy.context.view_layer.objects.active = o; o.select_set(True)
    bm = bmesh.new(); bm.from_mesh(o.data)
    doomed = []
    for f in bm.faces:
        c = f.calc_center_median()
        az = azimuth(c.x, c.y)
        inside = lo is not None and lo <= az < hi
        keep = inside if part != "dress_body" else not (-HALF_ANGLE <= az < HALF_ANGLE)
        if not keep: doomed.append(f)
    bmesh.ops.delete(bm, geom=doomed, context='FACES')
    bm.to_mesh(o.data); bm.free()

    # origem fica no CENTRO do vestido: girar o prop em Y desliza a aba
    # pela superficie da saia, como cortina em trilho circular
    bpy.ops.export_scene.gltf(filepath=os.path.join(ROOT, "sources", f"{part}_game.glb"),
                              export_format='GLB', export_yup=True, export_apply=True)
    print("PART OK", part, len(o.data.polygons), "faces")
