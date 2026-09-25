package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

            @Mixin(LogoRenderer.class)

            public abstract class LogoRendererMixin {
        private static final Identifier VOICECHAT_LOGO = Identifier.fromNamespaceAndPath("voicechat", "textures/gui/img_7.png");
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
        // img_7 is square; do not stretch it using the previous logo's proportions.
        int textureWidth = 1254;
        int textureHeight = 1254;

        // Display width in GUI pixels; height preserves the aspect ratio.
        int y = heightOffset - 30;
        int headerSpace = graphics.guiHeight() / 4 + 40 - y;
        int logoWidth = Math.max(0, Math.min(216, Math.min(width - 24, headerSpace)));
        int logoHeight = logoWidth;

        int x = (width - logoWidth) / 2;
        float effectiveAlpha = keepLogoThroughFade() ? 1.0F : alpha;
        if (logoWidth > 0 && logoHeight > 0 && effectiveAlpha > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, VOICECHAT_LOGO, x, y, 0, 0, logoWidth, logoHeight,
                    textureWidth, textureHeight, textureWidth, textureHeight, ARGB.white(effectiveAlpha));
        }

        // Skip both the Minecraft logo and the Java Edition subtitle.
        ci.cancel();
    }
}
