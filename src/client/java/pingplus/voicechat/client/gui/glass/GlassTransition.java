package pingplus.voicechat.client.gui.glass;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Lightweight common backdrop; original loading text, progress and cancel actions stay intact. */
public final class GlassTransition {
    public static void draw(GuiGraphicsExtractor g) {
        int w=g.guiWidth(), h=g.guiHeight();
        g.fill(0,0,w,h,0xFF0D1728);
        g.fillGradient(0,0,w,h,0xFF19354B,0xFF0B1323);
        GlassStyle.surface(g,16,20,Math.max(1,w-32),Math.max(1,h-40),1,.3f);
        g.nextStratum();
    }
    private GlassTransition() {}
}
