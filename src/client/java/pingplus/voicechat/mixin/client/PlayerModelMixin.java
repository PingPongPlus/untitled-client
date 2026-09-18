package pingplus.voicechat.mixin.client;


import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.PlayerSettings;


@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL")
    )
    private void yourmodscaleHead(
            AvatarRenderState state,
            CallbackInfo ci
    ) {
        PlayerModel model = (PlayerModel) (Object) this;

        model.head.xScale = PlayerSettings.xyzHeadscale;
        model.head.yScale = PlayerSettings.xyzHeadscale;
        model.head.zScale = PlayerSettings.xyzHeadscale;
        if(!PlayerSettings.mainBodyPart){
            model.body.xScale = 0;
            model.body.yScale = 0;
            model.body.zScale = 0;
        }
    }
}
