package pingplus.voicechat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.PlayerSettings;
import pingplus.voicechat.client.VoicechatClient;
import pingplus.voicechat.client.gui.VoiceNametag;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void voicechat$decorateNametag(Avatar avatar, AvatarRenderState state, float delta, CallbackInfo ci) {
        // Respect vanilla visibility and exclude non-player avatars such as mannequins.
        if (avatar instanceof Player player && state.nameTag != null) {
            state.nameTag = VoiceNametag.decorate(state.nameTag, VoicechatClient.voiceStatus(player.getUUID()));
        }
    }

    @Inject(
            method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("TAIL")
    )
    private void changePlayerSize(
            AvatarRenderState state,
            PoseStack poseStack,
            CallbackInfo ci) {

        poseStack.scale(PlayerSettings.xScale, PlayerSettings.yScale, PlayerSettings.zScale);
    }
}
