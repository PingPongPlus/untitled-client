package pingplus.voicechat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
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

        if (voicechat$background == null) {
            voicechat$background = new AnimatedTitleBackground(minecraft);
        }
        voicechat$background.draw(graphics, width, height);
        // Intentionally replace the panorama; do not draw it over our background.
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void voicechat$stopBackground(CallbackInfo ci) {
        if (voicechat$background != null) {
            voicechat$background.close();
            voicechat$background = null;
        }
    }
}
