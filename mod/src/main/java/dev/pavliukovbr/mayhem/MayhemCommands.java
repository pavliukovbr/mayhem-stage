package dev.pavliukovbr.mayhem;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /mayhem <prop> <marca> [segundos] — move um prop para uma marca de palco.
 * O estado vive aqui no servidor; quem entra depois recebe o ultimo estado.
 */
public final class MayhemCommands {
    /** Ultimo destino de cada prop, para sincronizar quem entrar no meio. */
    public static final Map<String, PropMovePayload> STATE = new ConcurrentHashMap<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            var root = literal("mayhem").requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
            ShowMarks.PROPS.forEach((propName, marks) -> {
                var prop = literal(propName);
                marks.forEach((markName, m) -> prop
                    .then(literal(markName)
                        .executes(ctx -> move(ctx.getSource(), propName, m, 10f))
                        .then(argument("segundos", FloatArgumentType.floatArg(0f, 600f))
                            .executes(ctx -> move(ctx.getSource(), propName, m,
                                    FloatArgumentType.getFloat(ctx, "segundos"))))));
                root.then(prop);
            });
            dispatcher.register(root);
        });
    }

    private static int move(CommandSourceStack src, String prop, ShowMarks.Mark m, float seconds) {
        var payload = new PropMovePayload(prop, m.x(), m.y(), m.z(), m.yaw(),
                (int) (seconds * 1000));
        STATE.put(prop, payload);
        broadcast(src.getServer(), payload);
        src.sendSuccess(() -> Component.literal(
                prop + " indo para a marca em " + seconds + "s"), true);
        return 1;
    }

    public static void broadcast(MinecraftServer server, PropMovePayload payload) {
        for (var player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private MayhemCommands() {}
}
