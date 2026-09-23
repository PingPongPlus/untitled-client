package pingplus.voicechat.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.glass.GlassStyle;
import pingplus.voicechat.client.gui.glass.LoadingTitleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import pingplus.voicechat.client.gui.glass.LoadingTitleRenderer;
import java.util.function.IntSupplier;

/** Changes visuals only. Vanilla owns completion callbacks, failures, timing and overlay removal. */
@Mixin(LoadingOverlay.class)
public abstract class GlassLoadingOverlayMixin {
    @Shadow @Final private ReloadInstance reload;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;
    @Shadow @Final private boolean fadeIn;

    @Inject(method="registerTextures", at=@At("TAIL"))
    private static void registerTitle(TextureManager textures, CallbackInfo ci) {
        textures.registerAndLoad(LoadingTitleTexture.ID, new LoadingTitleTexture());
    }

    @Redirect(method="extractRenderState", at=@At(value="INVOKE",target="Ljava/util/function/IntSupplier;getAsInt()I"))
    private int glassBackground(IntSupplier supplier) { return 0xFFF4F3F1; }

    @Redirect(method="extractRenderState", at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
    private void hideLogo(GuiGraphicsExtractor g, RenderPipeline p, Identifier texture, int x,int y,float u,float v,int w,int h,int rw,int rh,int tw,int th,int color) {}

    @Inject(method="extractProgressBar",at=@At("HEAD"),cancellable=true)
    private void hideOldProgress(GuiGraphicsExtractor g,int x,int y,int right,int bottom,float alpha,CallbackInfo ci) { ci.cancel(); }

    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void drawIdentity(GuiGraphicsExtractor g,int mx,int my,float dt,CallbackInfo ci) {
        long now=Util.getMillis();
        float opacity=fadeOutStart >= 0 ? 1-Math.clamp((now-fadeOutStart-1000)/1000f,0,1)
                : fadeIn ? Math.clamp((now-fadeInStart)/500f,0,1) : 1;
        // Preserve the supplied artwork's aspect ratio, with only a slim progress bar below it.
        double scale = Math.min(g.guiWidth() * 0.82 / 1024.0, (g.guiHeight() - 52) / 559.0);
        int w = Math.max(1, (int)Math.round(1024 * scale));
        int h = Math.max(1, (int)Math.round(559 * scale));
        int x = (g.guiWidth() - w) / 2;
        int y = (g.guiHeight() - h - 18) / 2;
        LoadingTitleRenderer.draw(g, x, y, w, h, opacity);
        int barWidth = Math.max(20, (int)(w * 0.55));
        int barX = (g.guiWidth() - barWidth) / 2;
        int barY = y + h + 6;
        GlassStyle.round(g, barX, barY, barWidth, 4, 2, GlassStyle.alpha(0xFFD7DEE0, opacity));
        int progress = Math.round(barWidth * Math.clamp(reload.getActualProgress(), 0, 1));
        GlassStyle.round(g, barX, barY, progress, 4, 2, GlassStyle.alpha(0xFF526E7C, opacity));
        if (progress > 2) {
            g.fill(barX + 1, barY + 1, barX + progress - 1, barY + 2, GlassStyle.alpha(0xFF9DB4C0, opacity));
        }
    }
}
