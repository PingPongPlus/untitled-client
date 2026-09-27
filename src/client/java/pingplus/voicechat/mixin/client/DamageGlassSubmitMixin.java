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
            RenderType glass = DamageGlassRenderer.material(type);
            if (glass != type) {
                type = glass;
                // Strength uses the low byte; the remaining UV1.x bits carry 0–100% reflectivity.
                overlay = DamageGlassRenderer.current | DamageGlassSettings.reflectivity() << 8;
            }
        }
        original.call(model, state, pose, type, light, overlay, tint, sprite, outline, crumbling);
    }
}
