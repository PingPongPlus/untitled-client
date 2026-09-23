package pingplus.voicechat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pingplus.voicechat.client.gui.glass.GlassButtonRenderer;

/** Keep vanilla slider input, thumb placement and text, replacing only its two sprites. */
@Mixin(AbstractSliderButton.class)
public abstract class GlassSliderMixin {
    @WrapOperation(method = "extractWidgetRenderState", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
            ordinal = 0))
    private void voicechat$glassTrack(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
                                      int x, int y, int width, int height, int color, Operation<Void> original) {
        GlassButtonRenderer.draw(graphics, (AbstractSliderButton) (Object) this);
    }

    @WrapOperation(method = "extractWidgetRenderState", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
            ordinal = 1))
    private void voicechat$glassThumb(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
                                      int x, int y, int width, int height, int color, Operation<Void> original) {
        AbstractSliderButton slider = (AbstractSliderButton) (Object) this;
        int data = (color & 0xFF000000) | (slider.active ? 0xFFFF00 : 0x400000);
        GlassButtonRenderer.drawRect(graphics, x + 1, y + 2, Math.max(1, width - 2), Math.max(1, height - 4), data);
    }
}
