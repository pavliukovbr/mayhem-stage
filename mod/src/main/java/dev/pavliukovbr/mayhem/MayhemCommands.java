package dev.pavliukovbr.mayhem;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Comandos do show. O vestido e um conjunto rigido: corpo, duas cortinas
 * frontais com dobradica na borda externa, e a gaiola dentro com porta.
 * Tudo viaja junto; abrir e girar cortinas e porta nas dobradicas.
 */
public final class MayhemCommands {
    public static final Map<String, PropMovePayload> STATE = new ConcurrentHashMap<>();

    // dobradicas em coordenadas locais do modelo (medidas nos scripts)
    private static final double CL_X = -3.449, CL_Z = 4.925;
    private static final double CR_X =  3.446, CR_Z = 4.922;
    private static final double CD_X = -1.500, CD_Z = 2.598;
    private static final float CURTAIN_SWING = 75f;   // graus de abertura
    private static final float DOOR_SWING = 100f;

    private static ShowMarks.Mark center = ShowMarks.DRESS.get("backstage");
    private static float curtain = 0f;   // 0 fechado .. 1 aberto
    private static float door = 0f;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            var root = literal("mayhem")
                    .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));

            var dress = literal("dress");
            ShowMarks.DRESS.forEach((name, m) -> dress.then(timed(name,
                    (src, sec) -> { center = m; return sync(src, sec); })));
            dress.then(timed("open",  (src, sec) -> { curtain = 1f; return sync(src, sec); }));
            dress.then(timed("close", (src, sec) -> { curtain = 0f; return sync(src, sec); }));
            root.then(dress);

            var cage = literal("cage");
            cage.then(timed("open",  (src, sec) -> { door = 1f; return sync(src, sec); }));
            cage.then(timed("close", (src, sec) -> { door = 0f; return sync(src, sec); }));
            root.then(cage);

            dispatcher.register(root);
        });
    }

    private interface Move { int run(CommandSourceStack src, float seconds); }

    private static LiteralArgumentBuilder<CommandSourceStack> timed(String name, Move move) {
        return literal(name)
                .executes(ctx -> move.run(ctx.getSource(), 10f))
                .then(argument("segundos", FloatArgumentType.floatArg(0f, 600f))
                        .executes(ctx -> move.run(ctx.getSource(),
                                FloatArgumentType.getFloat(ctx, "segundos"))));
    }

    /** Recalcula as seis pecas a partir de centro + angulos e transmite. */
    private static int sync(CommandSourceStack src, float seconds) {
        int ms = (int) (seconds * 1000);
        var server = src.getServer();
        double yawRad = Math.toRadians(center.yaw());
        double c = Math.cos(yawRad), s = Math.sin(yawRad);

        send(server, new PropMovePayload("dress_body",
                center.x(), center.y(), center.z(), center.yaw(), ms));
        // cortinas: posicao no ponto da dobradica (girado pelo rumo do grupo),
        // rumo do grupo mais o giro de abertura para fora
        sendHinged(server, "curtain_l", CL_X, CL_Z,  CURTAIN_SWING * curtain, c, s, ms);
        sendHinged(server, "curtain_r", CR_X, CR_Z, -CURTAIN_SWING * curtain, c, s, ms);
        send(server, new PropMovePayload("cage",
                center.x(), center.y(), center.z(), center.yaw(), ms));
        sendHinged(server, "cage_door", CD_X, CD_Z,  DOOR_SWING * door, c, s, ms);

        src.sendSuccess(() -> Component.literal(String.format(
                "cena em movimento (%.1fs) cortinas=%s gaiola=%s",
                seconds, curtain > 0 ? "abertas" : "fechadas",
                door > 0 ? "aberta" : "fechada")), true);
        return 1;
    }

    private static void sendHinged(MinecraftServer server, String prop,
                                   double lx, double lz, float swing,
                                   double c, double s, int ms) {
        double wx = center.x() + lx * c + lz * s;
        double wz = center.z() - lx * s + lz * c;
        send(server, new PropMovePayload(prop, wx, center.y(), wz,
                center.yaw() + swing, ms));
    }

    private static void send(MinecraftServer server, PropMovePayload payload) {
        STATE.put(payload.prop(), payload);
        for (var player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private MayhemCommands() {}
}
