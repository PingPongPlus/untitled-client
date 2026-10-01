package pingplus.voicechat.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pingplus.voicechat.client.gui.glass.GlassStyle;

public class CoordinatesHud {
    private boolean enabled = true;
    private boolean glass = true, edges = false;

    public boolean isGlass() { return glass; }
    public void toggleGlass() { glass = !glass; }
    public boolean isEdges() { return edges; }
    public void toggleEdges() { edges = !edges; }
    public int width() { return Minecraft.getInstance().font.width(text()) + 16; }
    public int height() { return Minecraft.getInstance().font.lineHeight + 10; }
    private String text() {
        var player = Minecraft.getInstance().player;
        return player == null ? "XYZ  0 0 0" : "XYZ  " + player.getBlockX() + " "
                + player.getBlockY() + " " + player.getBlockZ();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }


    public void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.player == null) {
            return;
        }

        if (glass) {
            graphics.nextStratum();
            GlassStyle.surface(graphics, 0, 0, width(), height(), .88f, .08f, edges);
            graphics.nextStratum();
        }
        graphics.text(client.font, text(), 8, 5, GlassStyle.TEXT, !glass);
    }
}
