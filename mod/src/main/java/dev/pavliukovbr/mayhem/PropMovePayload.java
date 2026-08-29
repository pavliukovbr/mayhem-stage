package dev.pavliukovbr.mayhem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: manda um prop para um ponto, em N milissegundos. O servidor e dono
 * do estado; o cliente so interpola. E o embriao do sistema de cues.
 */
public record PropMovePayload(String prop, double x, double y, double z,
                              float yaw, int durationMs) implements CustomPacketPayload {
    public static final Type<PropMovePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MayhemShow.MOD_ID, "prop_move"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PropMovePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.prop);
                buf.writeDouble(p.x); buf.writeDouble(p.y); buf.writeDouble(p.z);
                buf.writeFloat(p.yaw); buf.writeInt(p.durationMs);
            },
            buf -> new PropMovePayload(buf.readUtf(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readFloat(), buf.readInt()));

    @Override
    public Type<PropMovePayload> type() { return TYPE; }
}
