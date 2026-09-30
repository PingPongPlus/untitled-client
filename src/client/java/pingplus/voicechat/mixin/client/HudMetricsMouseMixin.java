package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.hud.HudMetrics;

@Mixin(MouseHandler.class)
public abstract class HudMetricsMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void voicechat$countPress(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().handle() && action == GLFW.GLFW_PRESS)
            HudMetrics.mousePressed(client, button.button());
    }
}
