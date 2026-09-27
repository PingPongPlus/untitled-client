package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.ShoulderCamFeature;

/** While the shoulder cam is active, mouse look rotates the camera, not the player. */
@Mixin(Entity.class)
public abstract class ShoulderCamEntityMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void voicechat$shoulderCamTurn(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (ShoulderCamFeature.active() && (Object) this == Minecraft.getInstance().player) {
            ShoulderCamFeature.rotate(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
