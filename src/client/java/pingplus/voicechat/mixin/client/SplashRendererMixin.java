package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

@Mixin(SplashRenderer.class)
public abstract class SplashRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void drawCustomSplah(
            GuiGraphicsExtractor graphics,
            int screenWidth,
            Font font,
            float alpha,
            CallbackInfo ci
    ){
        //no Splash here return instantly
        ci.cancel();
    }
}
