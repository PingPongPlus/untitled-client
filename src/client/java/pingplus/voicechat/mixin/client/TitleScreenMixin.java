package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(
                    "voicechat", "textures/gui/title_background.png"
            );

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/gui/screens/TitleScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V")
    )
    private void drawCustomBackground(
            TitleScreen screen, GuiGraphicsExtractor graphics, float delta
    ) {
        int imageWidth = 3878;
        int imageHeight = 2579;

        // Cover the window while preserving the photo's aspect ratio.
        double scale = Math.max(width / (double) imageWidth, height / (double) imageHeight);
        int drawWidth = (int) Math.ceil(imageWidth * scale);
        int drawHeight = (int) Math.ceil(imageHeight * scale);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                (width - drawWidth) / 2, (height - drawHeight) / 2,
                0.0F, 0.0F,
                drawWidth, drawHeight,
                imageWidth, imageHeight,
                imageWidth, imageHeight
        );
    }
}
