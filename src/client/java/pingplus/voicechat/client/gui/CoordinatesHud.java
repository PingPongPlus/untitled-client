package pingplus.voicechat.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CoordinatesHud {
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

        String text = "XYZ  "
                + client.player.getBlockX() + " "
                + client.player.getBlockY() + " "
                + client.player.getBlockZ();

// Below the FPS display, without a background.
        graphics.text(client.font, text, 22, 42, GuiTheme.TEXT, false);
    }
}
