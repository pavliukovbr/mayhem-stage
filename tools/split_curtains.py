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

    if hinge_az is not None:
        b = math.radians(hinge_az)
        best = 0.0
        for v in o.data.vertices:
            if abs(azimuth(v.co.x, v.co.y) - hinge_az) < 4.0:
                best = max(best, math.hypot(v.co.x, v.co.y))
        hx, hy = math.sin(b)*best, -math.cos(b)*best
        for v in o.data.vertices:
            v.co.x -= hx; v.co.y -= hy
        # offset local do jogo: X igual, Z = -Y do Blender
        print(f"HINGE {part} game_local=({hx:.3f}, {-hy:.3f}) r={best:.3f}")

    bpy.ops.export_scene.gltf(filepath=os.path.join(ROOT, "sources", f"{part}_game.glb"),
                              export_format='GLB', export_yup=True, export_apply=True)
    print("PART OK", part, len(o.data.polygons), "faces")
