package pingplus.voicechat.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.ChatHud;

/**
 * With the glass panel enabled, the vanilla per-line black boxes and prompt
 * backgrounds are redundant. Only negative-x fills are backgrounds; the scrollbar
 * and tag badges stay untouched.
 */
@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"
})
public abstract class ChatLineFillMixin {
    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void voicechat$hideLineBackground(int x, int y, int width, int height, int color, CallbackInfo ci) {
        if (x < 0 && ChatHud.INSTANCE.isEnabled() && ChatHud.INSTANCE.isGlass()) ci.cancel();
    }
}
