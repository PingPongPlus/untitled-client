package pingplus.voicechat.mixin.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pingplus.voicechat.client.ShoulderCamFeature;

/** Replaces the camera rotation while the shoulder cam is active; the third person offset follows. */
@Mixin(Camera.class)
public abstract class ShoulderCamCameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void voicechat$shoulderCamRotation(Camera camera, float yaw, float pitch) {
        if (ShoulderCamFeature.active()) {
            setRotation(ShoulderCamFeature.cameraYaw(), ShoulderCamFeature.cameraPitch());
        } else {
            setRotation(yaw, pitch);
        }
    }
}
