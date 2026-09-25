package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The editable HUD owns the sidebar; tab-list and below-name scores remain vanilla. */
@Mixin(Hud.class)
public abstract class ScoreboardHudMixin {
    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void replaceSidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        ci.cancel();
    }
}
