package pingplus.voicechat.mixin.client;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pingplus.voicechat.client.damageglass.DamageGlassRenderer;

@Mixin(RenderType.class)
public abstract class DamageGlassPrepareMixin {
    @Inject(method = "prepare", at = @At("RETURN"), cancellable = true)
    private void voicechat$sceneBinding(CallbackInfoReturnable<PreparedRenderType> cir) {
        if (DamageGlassRenderer.isGlass(cir.getReturnValue().pipeline()))
            cir.setReturnValue(DamageGlassRenderer.bindScene(cir.getReturnValue()));
    }
}
