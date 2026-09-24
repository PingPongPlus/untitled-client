package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
public abstract class LogoRendererMixin {
    @Shadow
    public abstract boolean keepLogoThroughFade();

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void drawCustomLogo(
            GuiGraphicsExtractor graphics,
            int width,
            float alpha,
            int heightOffset,
            CallbackInfo ci
    ) {
        // Preserve the original mountain/air mark with its actual aspect ratio.
        int textureWidth = 1707;
        int textureHeight = 924;

        // Display width in GUI pixels; height preserves the aspect ratio.
        int logoWidth = Math.min(216, width - 24);
        int logoHeight = Math.round(
                logoWidth * textureHeight / (float) textureWidth
        );

        int x = (width - logoWidth) / 2;
        float effectiveAlpha = keepLogoThroughFade() ? 1.0F : alpha;
        int y = heightOffset - 30;
        pingplus.voicechat.client.gui.glass.GlassLogoRenderer.draw(graphics, x, y, logoWidth, logoHeight, effectiveAlpha);

        // Skip both the Minecraft logo and the Java Edition subtitle.
        ci.cancel();
    }
}
