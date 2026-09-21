package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
public abstract class LogoRendererMixin {
    @Unique
    private static final Identifier CUSTOM_LOGO =
            Identifier.fromNamespaceAndPath(
                    "voicechat", "textures/gui/img.png"
            );

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
        // Replace these with your PNG's actual dimensions.
        int textureWidth = 512;
        int textureHeight = 200;

        // Display width in GUI pixels; height preserves the aspect ratio.
        int logoWidth = 256;
        int logoHeight = Math.round(
                logoWidth * textureHeight / (float) textureWidth
        );

        int x = (width - logoWidth) / 2;
        float effectiveAlpha = keepLogoThroughFade() ? 1.0F : alpha;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CUSTOM_LOGO,
                x, heightOffset,
                0.0F, 0.0F,
                logoWidth, logoHeight,
                textureWidth, textureHeight,
                textureWidth, textureHeight,
                ARGB.white(effectiveAlpha)
        );

        // Skip both the Minecraft logo and the Java Edition subtitle.
        ci.cancel();
    }
}