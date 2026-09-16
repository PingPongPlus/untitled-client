package pingplus.voicechat.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** The single FPS toggle, styled as a card. */
public final class FpsButton extends FlatButton {
    public static final int HEIGHT = 66;

    public FpsButton(int x, int y, int width, FpsHud fpsHud) {
        super(x, y, width, HEIGHT, "FPS counter", fpsHud::isEnabled, fpsHud::toggle);
    }

    @Override
    protected void drawContents(GuiGraphicsExtractor graphics, boolean enabled) {
        var font = Minecraft.getInstance().font;
        int iconBackground = GuiTheme.blendColors(GuiTheme.CARD, GuiTheme.ACCENT, 0.18);
        GuiTheme.drawRoundedRect(graphics, getX() + 10, getY() + 10, 28, 25, 5, iconBackground);
        graphics.text(font, "FPS", getX() + 24 - font.width("FPS") / 2, getY() + 18, GuiTheme.ACCENT, false);

        graphics.text(font, font.plainSubstrByWidth("FPS counter", width - 89),
                getX() + 46, getY() + 13, GuiTheme.TEXT, false);
        graphics.text(font, enabled ? "ENABLED" : "DISABLED", getX() + 46, getY() + 26,
                enabled ? GuiTheme.ACCENT : GuiTheme.MUTED, false);
        graphics.text(font, font.plainSubstrByWidth("Live rendering performance", width - 20),
                getX() + 10, getY() + 47, GuiTheme.MUTED, false);

        int trackColor = enabled ? GuiTheme.ACCENT : 0xFF484653;
        int thumbX = getRight() - (enabled ? 20 : 32);
        GuiTheme.drawRoundedRect(graphics, getRight() - 34, getY() + 14, 24, 12, 6, trackColor);
        GuiTheme.drawRoundedRect(graphics, thumbX, getY() + 16, 8, 8, 4, GuiTheme.TEXT);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return Component.literal("FPS counter" + (isSelected() ? ", enabled" : ", disabled"));
    }
}
