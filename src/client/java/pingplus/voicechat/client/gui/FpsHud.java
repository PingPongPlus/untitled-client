package pingplus.voicechat.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Holds the FPS toggle and draws its display during gameplay. */
public final class FpsHud {
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.player == null || client.gui.screen() != null) {
            return;
        }

        String text = "FPS  " + client.getFps();
        int x = 10;
        int y = 10;
        graphics.text(client.font, text, x + 12, y + 7, GuiTheme.TEXT, false);
    }
}
