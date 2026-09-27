package pingplus.voicechat.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.ChatHud;

/**
 * Replace message backgrounds with glass, preserving the separate queue and
 * restricted-chat prompts as well as scrollbar and message badges.
 */
@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"
})
public abstract class ChatLineFillMixin {
    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void voicechat$hideLineBackground(int x, int y, int width, int height, int color, CallbackInfo ci) {
        if (x == -4 && ChatHud.INSTANCE.isActive() && ChatHud.INSTANCE.isGlass()) ci.cancel();
    }
}
