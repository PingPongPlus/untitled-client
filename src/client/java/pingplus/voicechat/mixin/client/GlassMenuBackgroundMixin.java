package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.glass.GlassRain;

@Mixin(Screen.class)
public abstract class GlassMenuBackgroundMixin {
    @Unique private static final Identifier MOUNTAIN_AIR =
            Identifier.fromNamespaceAndPath("voicechat", "textures/gui/title_background.png");

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void mountainAir(GuiGraphicsExtractor g, float delta, CallbackInfo ci) {
        double scale = Math.max(g.guiWidth() / 1672.0, g.guiHeight() / 941.0);
        int w = (int) Math.ceil(1672 * scale), h = (int) Math.ceil(941 * scale);
        g.blit(RenderPipelines.GUI_TEXTURED, MOUNTAIN_AIR, (g.guiWidth() - w) / 2,
                (g.guiHeight() - h) / 2, 0, 0, w, h, 1672, 941, 1672, 941);
        ci.cancel();
    }

    // This common hook covers Pause, world selection/creation, multiplayer and options screens,
    // including overrides that don't call Screen.extractBackground(). Inventory and chat omit it.
    @Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            at = @At("HEAD"), cancellable = true)
    private void rainBackground(GuiGraphicsExtractor g, CallbackInfo ci) {
        boolean inWorld = Minecraft.getInstance().level != null;
        g.fillGradient(0, 0, g.guiWidth(), g.guiHeight(),
                inWorld ? 0x40101920 : 0x18101920, inWorld ? 0x70101920 : 0x48101920);
        GlassRain.draw(g);
        ci.cancel();
    }
}
