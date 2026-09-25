package pingplus.voicechat.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.PlayerSettings;

/** Hides the vanilla experience bar above the hotbar on demand. */
@Mixin(ExperienceBar.class)
public abstract class ExperienceBarMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void voicechat$hideXpBackground(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (PlayerSettings.hideXp) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void voicechat$hideXpBar(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (PlayerSettings.hideXp) ci.cancel();
    }
}
