package pingplus.voicechat.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.ChatHud;

/** The chat widget owns the gameplay chat; vanilla only draws it when the widget is toggled off. */
@Mixin(Hud.class)
public abstract class ChatHudCancelMixin {
    @Inject(method = "extractChat", at = @At("HEAD"), cancellable = true)
    private void voicechat$replaceVanillaChat(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (ChatHud.INSTANCE.isEnabled()) ci.cancel();
    }
}
