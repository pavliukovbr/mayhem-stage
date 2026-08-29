package dev.pavliukovbr.mayhem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lado servidor/comum do show.
 *
 * O servidor e dono de exatamente tres coisas: o relogio do show, a lista
 * de cues da musica atual e o estado (parado, tocando, pausado). Particula,
 * feixe de luz, conteudo de telao e animacao sao trabalho do cliente.
 */
public class MayhemShow implements ModInitializer {
    public static final String MOD_ID = "mayhem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        StageBlocks.init();
        PayloadTypeRegistry.clientboundPlay().register(PropMovePayload.TYPE, PropMovePayload.CODEC);
        MayhemCommands.register();
        // quem entra no meio do show recebe o ultimo estado de cada prop
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                MayhemCommands.STATE.values().forEach(p ->
                        ServerPlayNetworking.send(handler.getPlayer(), p)));
        LOGGER.info("Mayhem Show carregado. O relogio do show pertence ao servidor.");
    }
}
