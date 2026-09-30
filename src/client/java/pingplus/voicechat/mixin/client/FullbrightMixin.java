package pingplus.voicechat.mixin.client;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.PlayerSettings;

/** Drives the lightmap to full ambient white so the world is fully lit everywhere. */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class FullbrightMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void voicechat$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (!PlayerSettings.fullbright && !pingplus.voicechat.client.XrayFeature.enabled()) return;
        state.ambientColor = LightmapRenderStateExtractor.WHITE;
        state.blockFactor = 0;
        state.skyFactor = 0;
        state.brightness = 0;
        state.darknessEffectScale = 0;
        state.nightVisionEffectIntensity = 0;
        state.bossOverlayWorldDarkening = 0;
    }
}
