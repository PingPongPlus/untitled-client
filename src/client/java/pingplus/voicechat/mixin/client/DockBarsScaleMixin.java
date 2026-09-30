package pingplus.voicechat.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.hud.HotbarDock;

/** Lifts the health, hunger, armor, air and XP bars up when dock slots magnify into them. */
@Mixin(Hud.class)
public abstract class DockBarsScaleMixin {
    private static boolean barsPushed;

    @Inject(method = "extractHotbarAndDecorations",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
                    shift = At.Shift.AFTER))
    private void voicechat$scaleBars(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (!HotbarDock.shifting()) return;
        barsPushed = true;
        g.pose().pushMatrix();
        float lift = HotbarDock.barLift();
        if (lift > 0.5f) g.pose().translate(0, -lift);
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
    private void voicechat$unscaleBars(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        if (barsPushed) {
            g.pose().popMatrix();
            barsPushed = false;
        }
    }
}
