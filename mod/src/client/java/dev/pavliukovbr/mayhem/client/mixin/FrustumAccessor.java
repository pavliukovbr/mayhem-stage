package dev.pavliukovbr.mayhem.client.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Frustum.class)
public interface FrustumAccessor {
    @Accessor("matrix") Matrix4f mayhem$matrix();
    @Accessor("camX") double mayhem$camX();
    @Accessor("camY") double mayhem$camY();
    @Accessor("camZ") double mayhem$camZ();
}
