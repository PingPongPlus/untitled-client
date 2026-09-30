package pingplus.voicechat.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.hud.HotbarDock;

/** Replaces the vanilla item hotbar row with the macOS-style dock while enabled. */
@Mixin(Hud.class)
public abstract class HotbarDockMixin {
    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void voicechat$dockHotbar(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (HotbarDock.INSTANCE.render(g, dt)) ci.cancel();
    }
}
