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
 * Comandos do show. O vestido e um grupo de duas metades: elas viajam
 * juntas e abrem afastando-se no eixo X. O servidor guarda o centro e o
 * vao atuais, e o ultimo destino de cada prop para sincronizar quem entra.
 */
public final class MayhemCommands {
    public static final Map<String, PropMovePayload> STATE = new ConcurrentHashMap<>();

    private static ShowMarks.Mark dressCenter = ShowMarks.DRESS.get("backstage");
    private static double dressGap = 0;
    private static final double OPEN_GAP = 6.5;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            var root = literal("mayhem")
                    .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));

            var dress = literal("dress");
            ShowMarks.DRESS.forEach((name, m) -> dress.then(timed(name,
                    (src, sec) -> moveDress(src, m, dressGap, sec))));
            dress.then(timed("open",  (src, sec) -> moveDress(src, dressCenter, OPEN_GAP, sec)));
            dress.then(timed("close", (src, sec) -> moveDress(src, dressCenter, 0, sec)));
            root.then(dress);

            var cage = literal("cage");
            ShowMarks.CAGE.forEach((name, m) -> cage.then(timed(name,
                    (src, sec) -> moveOne(src, "cage", m, sec))));
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

    private static int moveDress(CommandSourceStack src, ShowMarks.Mark center,
                                 double gap, float seconds) {
        dressCenter = center;
        dressGap = gap;
        send(src.getServer(), new PropMovePayload("dress_l",
                center.x() - gap, center.y(), center.z(), center.yaw(), (int) (seconds * 1000)));
        send(src.getServer(), new PropMovePayload("dress_r",
                center.x() + gap, center.y(), center.z(), center.yaw(), (int) (seconds * 1000)));
        src.sendSuccess(() -> Component.literal(
                "vestido em movimento (" + seconds + "s, vao " + gap + ")"), true);
        return 1;
    }

    private static int moveOne(CommandSourceStack src, String prop,
                               ShowMarks.Mark m, float seconds) {
        send(src.getServer(), new PropMovePayload(prop,
                m.x(), m.y(), m.z(), m.yaw(), (int) (seconds * 1000)));
        src.sendSuccess(() -> Component.literal(prop + " em movimento (" + seconds + "s)"), true);
        return 1;
    }

    private static void send(MinecraftServer server, PropMovePayload payload) {
        STATE.put(payload.prop(), payload);
        for (var player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private MayhemCommands() {}
}
