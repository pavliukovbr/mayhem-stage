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
 * Comandos do show. Todas as pecas do vestido compartilham o mesmo centro;
 * abrir cortinas e girar as abas em volta do eixo, deslizando pela saia
 * como tecido em trilho. O elevador central sobe pelo eixo ate o topo.
 */
public final class MayhemCommands {
    public static final Map<String, PropMovePayload> STATE = new ConcurrentHashMap<>();

    // no show a saia e icada: sobe ~5 m e se comprime num dossel com franja
    // pairando sobre a gaiola, e a cantora fica em pe no topo dele
    private static final double HOIST_RISE = 15.0;
    private static final float HOIST_SQUASH = 0.45f;
    private static final float DOOR_SLIDE = 50f;
    private static final double LIFT_TOP = 26.0;

    private static ShowMarks.Mark center = ShowMarks.DRESS.get("backstage");
    private static float hoist = 0f, door = 0f, lift = 0f;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            var root = literal("mayhem")
                    .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));

            var dress = literal("dress");
            ShowMarks.DRESS.forEach((name, m) -> dress.then(timed(name, 10f,
                    (src, sec) -> { center = m; return sync(src, sec); })));
            dress.then(timed("open", 3f,  (src, sec) -> { hoist = 1f; return sync(src, sec); }));
            dress.then(timed("close", 3f, (src, sec) -> { hoist = 0f; return sync(src, sec); }));
            root.then(dress);

            var cage = literal("cage");
            cage.then(timed("open", 1.5f,  (src, sec) -> { door = 1f; return sync(src, sec); }));
            cage.then(timed("close", 1.5f, (src, sec) -> { door = 0f; return sync(src, sec); }));
            root.then(cage);

            var liftCmd = literal("lift");
            liftCmd.then(timed("top", 8f,    (src, sec) -> { lift = 1f; return sync(src, sec); }));
            liftCmd.then(timed("bottom", 8f, (src, sec) -> { lift = 0f; return sync(src, sec); }));
            root.then(liftCmd);

            dispatcher.register(root);
        });
    }

    private interface Move { int run(CommandSourceStack src, float seconds); }

    private static LiteralArgumentBuilder<CommandSourceStack> timed(
            String name, float defaultSec, Move move) {
        return literal(name)
                .executes(ctx -> move.run(ctx.getSource(), defaultSec))
                .then(argument("segundos", FloatArgumentType.floatArg(0f, 600f))
                        .executes(ctx -> move.run(ctx.getSource(),
                                FloatArgumentType.getFloat(ctx, "segundos"))));
    }

    /** Recalcula as pecas a partir de centro e estados, e transmite. */
    private static int sync(CommandSourceStack src, float seconds) {
        int ms = (int) (seconds * 1000);
        var server = src.getServer();
        float yaw = center.yaw();

        double rise = HOIST_RISE * hoist;
        float squash = 1f - (1f - HOIST_SQUASH) * hoist;
        at(server, "dress_body", rise, yaw, squash, ms);
        at(server, "curtain_l", rise, yaw, squash, ms);
        at(server, "curtain_r", rise, yaw, squash, ms);
        at(server, "cage", 0, yaw, 1f, ms);
        at(server, "cage_door", 0, yaw + DOOR_SLIDE * door, 1f, ms);
        at(server, "lift", LIFT_TOP * lift, yaw, 1f, ms);

        src.sendSuccess(() -> Component.literal(String.format(
                "cena (%.1fs) saia=%s gaiola=%s elevador=%s",
                seconds, hoist > 0 ? "icada" : "baixada",
                door > 0 ? "aberta" : "fechada", lift > 0 ? "topo" : "base")), true);
        return 1;
    }

    private static void at(MinecraftServer server, String prop,
                           double dy, float yaw, float scaleY, int ms) {
        var payload = new PropMovePayload(prop,
                center.x(), center.y() + dy, center.z(), yaw, scaleY, ms);
        STATE.put(prop, payload);
        for (var player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private MayhemCommands() {}
}
