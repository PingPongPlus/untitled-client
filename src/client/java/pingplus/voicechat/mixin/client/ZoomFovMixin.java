package pingplus.voicechat.mixin.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pingplus.voicechat.client.ZoomFeature;

/** Divides the camera FOV while zooming; the projection matrix is built from calculateFov. */
@Mixin(Camera.class)
public abstract class ZoomFovMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void voicechat$zoomFov(float partialTick, CallbackInfoReturnable<Float> ci) {
        if (ZoomFeature.progress() > 0) {
            ci.setReturnValue(ci.getReturnValueF() / (float) ZoomFeature.fovFactor());
        }
    }
}
