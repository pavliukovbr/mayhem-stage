package dev.pavliukovbr.mayhem.client;

import net.fabricmc.api.ClientModInitializer;
import dev.pavliukovbr.mayhem.MayhemShow;

/**
 * Lado cliente: renderizacao de tudo que e efemero — luzes, feixes,
 * haze, conteudo do telao e interpolacao entre cues. Nada disso viaja
 * pela rede; o cliente recebe timecode e estado, e desenha o resto.
 */
public class MayhemShowClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MayhemShow.LOGGER.info("Mayhem Show client pronto para renderizar.");
    }
}
