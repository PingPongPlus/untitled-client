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
import java.util.function.IntSupplier;

/** Changes visuals only. Vanilla owns completion callbacks, failures, timing and overlay removal. */
@Mixin(LoadingOverlay.class)
public abstract class GlassLoadingOverlayMixin {
    @Shadow @Final private ReloadInstance reload;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;
    @Shadow @Final private boolean fadeIn;

    @Redirect(method="extractRenderState", at=@At(value="INVOKE",target="Ljava/util/function/IntSupplier;getAsInt()I"))
    private int glassBackground(IntSupplier supplier) { return 0xFF0D1728; }

    @Redirect(method="extractRenderState", at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
    private void hideLogo(GuiGraphicsExtractor g, RenderPipeline p, Identifier texture, int x,int y,float u,float v,int w,int h,int rw,int rh,int tw,int th,int color) {}

    @Inject(method="extractProgressBar",at=@At("HEAD"),cancellable=true)
    private void hideOldProgress(GuiGraphicsExtractor g,int x,int y,int right,int bottom,float alpha,CallbackInfo ci) { ci.cancel(); }

    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void drawIdentity(GuiGraphicsExtractor g,int mx,int my,float dt,CallbackInfo ci) {
        long now=Util.getMillis();
        float opacity=fadeOutStart >= 0 ? 1-Math.clamp((now-fadeOutStart-1000)/1000f,0,1)
                : fadeIn ? Math.clamp((now-fadeInStart)/500f,0,1) : 1;
        int w=Math.min(260,g.guiWidth()-40), x=(g.guiWidth()-w)/2, y=g.guiHeight()/2;
        // Only built-in solid geometry: no dependency on fonts/textures/shaders being reloaded.
        GlassStyle.round(g,x-12,y-61,w+24,118,18,GlassStyle.alpha(0xFF172B41,opacity));
        GlassStyle.round(g,x-11,y-60,w+22,116,17,GlassStyle.alpha(0xFF112136,opacity));
        int cx=g.guiWidth()/2;
        // Custom U monogram in cut glass, shared with the Untitled client identity.
        GlassStyle.round(g,cx-20,y-42,9,33,4,GlassStyle.alpha(0xFFB7F1FF,opacity));
        GlassStyle.round(g,cx+11,y-42,9,33,4,GlassStyle.alpha(0xFF71B9D8,opacity));
        GlassStyle.round(g,cx-20,y-17,40,9,4,GlassStyle.alpha(0xFF8DDDE9,opacity));
        g.fill(x+12,y+14,x+w-12,y+15,GlassStyle.alpha(0xFF314E68,opacity));
        int length=w-24;
        int progress=Math.round(length*Math.clamp(reload.getActualProgress(),0,1));
        GlassStyle.round(g,x+12,y+14,progress,3,1,GlassStyle.alpha(GlassStyle.ACCENT,opacity));
        g.fill(x+12,y+36,x+35,y+37,GlassStyle.alpha(0xFF6F94AC,opacity));
        g.fill(x+w-35,y+36,x+w-12,y+37,GlassStyle.alpha(0xFF6F94AC,opacity));
    }
}
