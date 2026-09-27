package pingplus.voicechat.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.damageglass.DamageGlassRenderer;

@Mixin(targets = "net.minecraft.client.renderer.feature.FeatureRenderDispatcher$PreparedFrame")
public abstract class DamageGlassCaptureMixin {
    @Inject(method = "executeSolid", at = @At("HEAD"))
    private void voicechat$captureScene(CallbackInfo ci) { DamageGlassRenderer.capture(); }
}
