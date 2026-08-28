package dev.pavliukovbr.mayhem;

import net.fabricmc.api.ModInitializer;
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
        LOGGER.info("Mayhem Show carregado. O relogio do show pertence ao servidor.");
    }
}
