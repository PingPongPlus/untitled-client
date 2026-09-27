package pingplus.voicechat.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pingplus.voicechat.client.ZoomFeature;

/** Scales mouse sensitivity down while zooming so aiming stays natural. */
@Mixin(MouseHandler.class)
public abstract class ZoomSensitivityMixin {
    @ModifyVariable(method = "turnPlayer", at = @At(value = "STORE"), index = 7)
    private double voicechat$zoomSensitivity(double sens) {
        return ZoomFeature.progress() > 0 ? sens * Math.pow(ZoomFeature.sensitivityFactor(), 1.0 / 3.0) : sens;
    }
}
