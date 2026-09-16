package pingplus.voicechat.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared ARGB colors and drawing helpers. Colors use 0xAARRGGBB. */
public final class GuiTheme {
    public static final int ACCENT = 0xFF9C8CFF;
    public static final int TEXT = 0xFFF1F0F7;
    public static final int MUTED = 0xFFA09DB2;
    public static final int PANEL = 0xFF17171F;
    public static final int CARD = 0xFF22222D;
    public static final int BORDER = 0xFF33333F;

    private GuiTheme() {}

    /** Draws a center rectangle and mirrored scanlines for the rounded corners. */
    public static void drawRoundedRect(GuiGraphicsExtractor graphics, int x, int y,
                                       int width, int height, int radius, int color) {
        int cornerRadius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        graphics.fill(x, y + cornerRadius, x + width, y + height - cornerRadius, color);

        for (int row = 0; row < cornerRadius; row++) {
            // Circle equation: horizontal distance = sqrt(radius² - vertical distance²).
            double distanceFromCenter = cornerRadius - row - 0.5;
            double cornerWidth = Math.sqrt(cornerRadius * cornerRadius - distanceFromCenter * distanceFromCenter);
            int inset = (int) Math.ceil(cornerRadius - cornerWidth);
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            graphics.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
        }
    }

    /** Blends RGB channels; the result is opaque. Amount 0 selects from, 1 selects to. */
    public static int blendColors(int from, int to, double amount) {
        double weight = Math.clamp(amount, 0.0, 1.0);
        int red = blendChannel((from >> 16) & 255, (to >> 16) & 255, weight);
        int green = blendChannel((from >> 8) & 255, (to >> 8) & 255, weight);
        int blue = blendChannel(from & 255, to & 255, weight);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int blendChannel(int from, int to, double weight) {
        return (int) (from * (1 - weight) + to * weight);
    }
}
