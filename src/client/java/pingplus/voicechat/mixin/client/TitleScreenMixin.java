package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.AnimatedTitleBackground;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique
    private AnimatedTitleBackground voicechat$background;

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

        if (voicechat$background == null) {
            voicechat$background = new AnimatedTitleBackground(minecraft);
        }
        voicechat$background.draw(graphics, width, height);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void voicechat$stopBackground(CallbackInfo ci) {
        if (voicechat$background != null) {
            voicechat$background.close();
            voicechat$background = null;
        }
    }
}
