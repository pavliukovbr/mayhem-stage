package dev.pavliukovbr.mayhem.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.pavliukovbr.mayhem.client.PropRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void mayhem$afterRender(GraphicsResourceAllocator alloc, DeltaTracker delta,
            boolean blockOutline, CameraRenderState camera, Matrix4fc frustumMatrix,
            GpuBufferSlice projection, Vector4f fogColor, boolean sky, CallbackInfo ci) {
        PropRenderer.render(camera);
    }
}
