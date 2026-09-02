package dev.pavliukovbr.mayhem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C generico de efeitos do show: kind = "lights" (arg: preset) ou
 * "screen" (arg: "play <nome>" / "stop"). Strings de proposito: o cue
 * system vai emitir exatamente isto lendo JSON.
 */
public record ShowFxPayload(String kind, String arg) implements CustomPacketPayload {
    public static final Type<ShowFxPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MayhemShow.MOD_ID, "show_fx"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShowFxPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.kind); buf.writeUtf(p.arg); },
            buf -> new ShowFxPayload(buf.readUtf(), buf.readUtf()));

    @Override
    public Type<ShowFxPayload> type() { return TYPE; }
}
