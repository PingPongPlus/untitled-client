package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.VoicechatClient;

@Mixin(MouseHandler.class)
public abstract class VoiceVolumeMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void voicechat$playerVolume(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (window == client.getWindow().handle() && action == GLFW.GLFW_PRESS
                && button.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && VoicechatClient.openPlayerVoiceVolume(client))
            ci.cancel();
    }
}
