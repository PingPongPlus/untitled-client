package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pingplus.voicechat.client.gui.glass.GlassStyle;

@Mixin(GenericMessageScreen.class)
public abstract class GlassMessageTextMixin {
    @ModifyArg(method = "init", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/components/FocusableTextWidget;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;I)Lnet/minecraft/client/gui/components/FocusableTextWidget$Builder;"), index = 0)
    private Component glassStatus(Component original) {
        return original.copy().withStyle(GlassStyle.FONT);
    }
}
