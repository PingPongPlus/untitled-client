package pingplus.voicechat.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pingplus.voicechat.client.hud.CustomCrosshairHud;

/** Replace only the ordinary sprite, after vanilla camera/spectator/debug guards. */
@Mixin(Hud.class)
public abstract class CustomCrosshairMixin {
    @Redirect(method = "extractCrosshair", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void voicechat$customCrosshair(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
                                           int x, int y, int width, int height) {
        var crosshair = CustomCrosshairHud.INSTANCE;
        if (crosshair.isEnabled()) {
            var client = Minecraft.getInstance();
            crosshair.render(graphics, graphics.guiWidth() / 2, graphics.guiHeight() / 2,
                    client.player.getAttackStrengthScale(client.getDeltaTracker().getGameTimeDeltaPartialTick(true)));
        } else graphics.blitSprite(pipeline, sprite, x, y, width, height);
    }

    @Redirect(method = "extractCrosshair", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object voicechat$cooldownIndicator(OptionInstance<?> option) {
        var crosshair = CustomCrosshairHud.INSTANCE;
        return crosshair.isEnabled() && crosshair.isCooldownRing() ? AttackIndicatorStatus.OFF : option.get();
    }
}
