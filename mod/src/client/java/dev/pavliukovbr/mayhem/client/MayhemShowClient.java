package dev.pavliukovbr.mayhem.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import dev.pavliukovbr.mayhem.MayhemShow;
import dev.pavliukovbr.mayhem.PropMovePayload;
import dev.pavliukovbr.mayhem.ShowFxPayload;

/**
 * Lado cliente: renderizacao de tudo que e efemero — luzes, feixes,
 * haze, conteudo do telao e interpolacao entre cues. Nada disso viaja
 * pela rede; o cliente recebe timecode e estado, e desenha o resto.
 */
public class MayhemShowClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PropMovePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> PropRenderer.moveProp(payload.prop(),
                        payload.x(), payload.y(), payload.z(),
                        payload.yaw(), payload.scaleY(), payload.durationMs())));
        ClientPlayNetworking.registerGlobalReceiver(ShowFxPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    if (payload.kind().equals("lights")) ShowLights.setPreset(payload.arg());
                    else if (payload.kind().equals("screen")) VideoScreen.handle(payload.arg());
                }));
        MayhemShow.LOGGER.info("Mayhem Show client pronto para renderizar.");
    }
}
