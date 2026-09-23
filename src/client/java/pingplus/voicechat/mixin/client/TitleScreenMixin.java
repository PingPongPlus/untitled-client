package pingplus.voicechat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique
    private static final Identifier voicechat$background = Identifier.fromNamespaceAndPath(
            "voicechat", "textures/gui/title_background.png");

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    // LabyMod's MixinExtras 0.5.4 cannot read the array-valued @Redirect.at
    // emitted by our newer Mixin dependency. WrapOperation avoids that reader.
    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/gui/screens/TitleScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V")
    )
    private void drawCustomBackground(
            TitleScreen screen, GuiGraphicsExtractor graphics, float delta, Operation<Void> original
    ) {
        int imageWidth = 1672;
        int imageHeight = 941;
        // Fill the window without stretching; crop equally on opposite edges.
        double scale = Math.max(width / (double) imageWidth, height / (double) imageHeight);
        int drawWidth = (int) Math.ceil(imageWidth * scale);
        int drawHeight = (int) Math.ceil(imageHeight * scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, voicechat$background,
                (width - drawWidth) / 2, (height - drawHeight) / 2,
                0.0F, 0.0F, drawWidth, drawHeight,
                imageWidth, imageHeight, imageWidth, imageHeight);
    }
}
