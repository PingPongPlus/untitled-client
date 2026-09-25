package pingplus.voicechat.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.PlayerSettings;

/** Hides the vanilla recovery-compass locator bar above the hotbar on demand. */
@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void voicechat$hideLocatorBackground(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (PlayerSettings.hideLocator) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void voicechat$hideLocatorBar(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (PlayerSettings.hideLocator) ci.cancel();
    }
}
