package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.PlayerSettings;

/** Hides the vanilla health hearts, hunger bar and experience level text on demand. */
@Mixin(Hud.class)
public abstract class VanillaBarsMixin {
    @Inject(method = "extractHearts", at = @At("HEAD"), cancellable = true)
    private void voicechat$hideHearts(GuiGraphicsExtractor g, Player player, int left, int top, int rightmostHeartIndex,
                                      int rowHeight, float maxHealth, int health, int displayHealth, int absorption,
                                      boolean blinking, CallbackInfo ci) {
        if (PlayerSettings.hideHealth) ci.cancel();
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void voicechat$hideFood(GuiGraphicsExtractor g, Player player, int top, int right, CallbackInfo ci) {
        if (PlayerSettings.hideHunger) ci.cancel();
    }

    @Redirect(method = "extractHotbarAndDecorations",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    private void voicechat$hideExperienceLevel(GuiGraphicsExtractor g, Font font, int level) {
        if (!PlayerSettings.hideXp) ContextualBar.extractExperienceLevel(g, font, level);
    }
}
