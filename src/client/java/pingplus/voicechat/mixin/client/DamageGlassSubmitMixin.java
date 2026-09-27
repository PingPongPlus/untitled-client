package pingplus.voicechat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import pingplus.voicechat.client.damageglass.DamageGlassRenderer;
import pingplus.voicechat.client.damageglass.DamageGlassSettings;

@Mixin(SubmitNodeCollection.class)
public abstract class DamageGlassSubmitMixin {
    @WrapMethod(method = "submitModel")
    private <S> void voicechat$glassModel(Model<? super S> model, S state, PoseStack pose, RenderType type,
                                         int light, int overlay, int tint, TextureAtlasSprite sprite, int outline,
                                         ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        if (DamageGlassRenderer.current != 0) {
            boolean rippleOnly = !DamageGlassSettings.enabled();
            RenderType glass = DamageGlassRenderer.material(type, rippleOnly);
            if (glass != type) {
                if (rippleOnly) {
                    // Keep the complete vanilla draw, including hurt tint, lighting, armor and outlines.
                    original.call(model, state, pose, type, light, overlay, tint, sprite, outline, crumbling);
                    outline = 0;
                    crumbling = null;
                }
                type = glass;
                // Strength uses the low byte; the remaining UV1.x bits carry 0–100% reflectivity.
                overlay = DamageGlassRenderer.current | DamageGlassSettings.reflectivity() << 8;
                overlay |= (rippleOnly ? 7 : DamageGlassSettings.preset().shaderId) << 24;
                if (DamageGlassSettings.impactRipple()) overlay |= 1 << 27;
                // Vanilla light occupies the low byte of each coordinate; pack screen-space origin above it.
                light = (light & 0x00FF00FF) | DamageGlassRenderer.impactCenter;
            }
        }
        original.call(model, state, pose, type, light, overlay, tint, sprite, outline, crumbling);
    }
}
