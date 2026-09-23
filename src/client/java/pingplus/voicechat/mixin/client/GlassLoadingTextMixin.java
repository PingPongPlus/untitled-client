package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pingplus.voicechat.client.gui.glass.GlassStyle;

/** Restyle the original localized status without replacing its content or timing. */
@Mixin({ConnectScreen.class, LevelLoadingScreen.class, ProgressScreen.class})
public abstract class GlassLoadingTextMixin {
    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 1)
    private Component glassStatus(Component original) {
        return original.copy().withStyle(GlassStyle.FONT);
    }
}
