package dev.pavliukovbr.mayhem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

/**
 * Fisica do show, lado servidor: barreiras invisiveis acompanham o
 * elevador e o piso do segundo andar da gaiola, e quem esta em cima do
 * elevador viaja junto. O servidor reproduz a MESMA interpolacao que o
 * cliente usa para desenhar, entao colisao e visual nunca divergem.
 */
public final class ShowPhysics {
    private static final double LIFT_TOP = 20.0, LIFT_R = 2.6, DISC_H = 0.42;
    private static final double CAGE_FLOOR_DY = 11.0, CAGE_R = 7.75;

    private static double cx, cy, cz;              // centro atual do grupo
    private static double sy, ty;                  // anim do elevador (dy)
    private static long start; private static int dur;
    private static boolean active;

    private static final Set<BlockPos> placed = new HashSet<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ShowPhysics::tick);
    }

    static void onSync(ShowMarks.Mark center, float lift, int ms) {
        cx = center.x(); cy = center.y(); cz = center.z();
        sy = liftDy(); ty = LIFT_TOP * lift;
        start = System.currentTimeMillis(); dur = Math.max(ms, 1);
        active = true;
    }

    private static double liftDy() {
        if (!active) return LIFT_TOP;
        double t = Math.min(1.0, (System.currentTimeMillis() - start) / (double) dur);
        t = t * t * (3 - 2 * t);
        return sy + (ty - sy) * t;
    }

    private static void tick(MinecraftServer server) {
        if (!active) return;
        ServerLevel level = server.overworld();
        double liftTop = cy + liftDy() + DISC_H;

        Set<BlockPos> want = new HashSet<>();
        int ly = (int) Math.floor(liftTop) - 1;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                want.add(new BlockPos((int) Math.floor(cx) + dx, ly,
                                      (int) Math.floor(cz) + dz));
        int fy = (int) Math.floor(cy + CAGE_FLOOR_DY) - 1;
        for (int dx = -8; dx <= 8; dx++)
            for (int dz = -8; dz <= 8; dz++)
                if (dx * dx + dz * dz <= CAGE_R * CAGE_R)
                    want.add(new BlockPos((int) Math.floor(cx) + dx, fy,
                                          (int) Math.floor(cz) + dz));

        for (BlockPos pos : placed) {
            if (!want.contains(pos) && level.getBlockState(pos).is(Blocks.BARRIER)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
        for (BlockPos pos : want) {
            if (!placed.contains(pos) && level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
            }
        }
        placed.clear();
        placed.addAll(want);

        // quem esta na plataforma viaja com ela
        boolean moving = System.currentTimeMillis() - start < dur;
        if (moving) {
            for (var player : server.getPlayerList().getPlayers()) {
                double dx = player.getX() - cx, dz = player.getZ() - cz;
                if (dx * dx + dz * dz <= LIFT_R * LIFT_R
                        && Math.abs(player.getY() - liftTop) < 2.5) {
                    player.teleportTo(player.getX(), liftTop, player.getZ());
                }
            }
        }
    }

    private ShowPhysics() {}
}
