package pingplus.voicechat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.damageglass.DamageGlassRenderer;

@Mixin(LevelRenderer.class)
public abstract class DamageGlassLevelMixin {
    @Inject(method = "submitFeatures", at = @At("TAIL"))
    private void voicechat$deathBlobs(net.minecraft.client.renderer.state.level.LevelRenderState state,
            net.minecraft.client.renderer.SubmitNodeCollector collector, boolean outline, CallbackInfo ci) {
        pingplus.voicechat.client.damageglass.DeathGlassMelt.submit(state.cameraRenderState, collector);
    }
    @WrapMethod(method = "render")
    private void voicechat$worldScope(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline,
                                      CameraRenderState camera, Matrix4fc view, GpuBufferSlice fog, Vector4f color,
                                      boolean sky, Operation<Void> original) {
        DamageGlassRenderer.begin();
        try { original.call(allocator, delta, outline, camera, view, fog, color, sky); }
        finally { DamageGlassRenderer.end(); }
    }
    @Inject(method = "close", at = @At("HEAD"))
    private void voicechat$closeGlass(CallbackInfo ci) { DamageGlassRenderer.close(); }
}
