package pingplus.voicechat.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Reads shared settings and draws enabled modules independently of the ClickGUI. */
public final class ClientHud {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int SCREEN_MARGIN = 10;
    private static final int CHIP_HEIGHT = 21;
    private static final int CHIP_GAP = 4;
    private final ClientConfig config;
    private ClientLevel currentWorld;
    private long sessionStartNanos;

    public ClientHud(ClientConfig config) {
        this.config = config;
    }

    public void tick(Minecraft client) {
        // The world reference also changes on dimension switches and disconnects.
        if (currentWorld != client.level) {
            currentWorld = client.level;
            sessionStartNanos = System.nanoTime();
        }
    }

    public void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) {
            return;
        }

        int nextY = SCREEN_MARGIN;
        for (ClientModule module : ClientModule.values()) {
            if (!config.isEnabled(module)) {
                continue;
            }
            String text = moduleText(module, client, client.player);
            drawChip(graphics, client.font, SCREEN_MARGIN, nextY, text, config);
            nextY += CHIP_HEIGHT + CHIP_GAP;
        }
    }

    private String moduleText(ClientModule module, Minecraft client, LocalPlayer player) {
        return switch (module) {
            case FPS -> "FPS  " + client.getFps();
            case COORDINATES -> "XYZ  " + player.getBlockX() + " / " + player.getBlockY() + " / " + player.getBlockZ();
            case DIRECTION -> "FACING  " + player.getDirection().getName().toUpperCase(Locale.ROOT);
            case MOVEMENT -> movementText(player);
            case CLOCK -> "TIME  " + LocalTime.now().format(CLOCK_FORMAT);
            case SESSION -> "SESSION  " + sessionTime();
        };
    }

    private String movementText(LocalPlayer player) {
        if (player.isSprinting()) {
            return "SPRINTING";
        }
        if (player.isShiftKeyDown()) {
            return "SNEAKING";
        }
        return "WALKING";
    }

    private String sessionTime() {
        long elapsedSeconds = (System.nanoTime() - sessionStartNanos) / 1_000_000_000L;
        return String.format(Locale.ROOT, "%d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);
    }

    /** Also used by the appearance preview so it matches the actual HUD styling. */
    public static void drawChip(GuiGraphicsExtractor graphics, Font font, int x, int y,
                                String text, ClientConfig config) {
        int alpha = (int) (config.hudOpacity() * 255);
        int background = (alpha << 24) | (GuiTheme.PANEL & 0x00FFFFFF);
        GuiTheme.drawRoundedRect(graphics, x, y, font.width(text) + 20, CHIP_HEIGHT, 5, background);
        graphics.fill(x + 5, y + 6, x + 7, y + 15, config.accentColor());
        graphics.text(font, text, x + 12, y + 7, GuiTheme.TEXT, false);
    }
}
