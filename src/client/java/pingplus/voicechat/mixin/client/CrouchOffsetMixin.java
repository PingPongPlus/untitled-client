package pingplus.voicechat.mixin.client;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class CrouchOffsetMixin {

    @Inject(
            method = "getRenderOffset(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void changeOffset(AvatarRenderState state, CallbackInfoReturnable<Vec3> cir) {

        Vec3 originalOffset = cir.getReturnValue();

        // Additional offset: X, Y, Z, measured in blocks.
        Vec3 newOffset = originalOffset.add(0.0, 0.0, 0.0);

        cir.setReturnValue(newOffset);
    }
}