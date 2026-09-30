package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pingplus.voicechat.client.hud.ChatHud;

/** Keeps chat link clicks aligned with the relocated chat widget. */
@Mixin(ChatScreen.class)
public abstract class ChatClickRelocateMixin {
    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;captureClickableText(Lnet/minecraft/client/gui/ActiveTextCollector;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V"))
    private void voicechat$relocateClickCapture(ChatComponent chat, ActiveTextCollector collector, int height,
                                                int tickCount, ChatComponent.DisplayMode mode) {
        if (!ChatHud.INSTANCE.isActive()) {
            chat.captureClickableText(collector, height, tickCount, mode);
            return;
        }
        var bounds = ChatHud.screenBounds();
        var params = collector.defaultParameters();
        var pose = new Matrix3x2f(params.pose());
        pose.translate((float) bounds.x(), (float) bounds.y());
        pose.scale((float) bounds.scale());
        pose.translate(ChatHud.INSTANCE.textOffsetX(), 0);
        collector.defaultParameters(params.withPose(pose));
        try {
            chat.captureClickableText(collector, ChatHud.INSTANCE.renderAnchor(), tickCount, mode);
        } finally {
            collector.defaultParameters(params);
        }
    }
}
