package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.KillAuraFeature;

@Mixin(GameRenderer.class)
public abstract class KillAuraFrameMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void voicechat$smoothAim(CallbackInfo ci) { KillAuraFeature.frame(Minecraft.getInstance()); }
}
