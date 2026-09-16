package pingplus.voicechat.client.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** The panel shell with one FPS button. Add your own controls in init(). */
public final class ClickGuiScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 620;
    private static final int MAX_PANEL_HEIGHT = 366;
    private static final int WINDOW_MARGIN = 8;
    private static final int CONTENT_PADDING = 14;
    private static final int CARD_WIDTH = 224;

    private final FpsHud fpsHud;
    private final KeyMapping openGuiKey;

    // Minecraft supplies GUI-scaled dimensions for drawing and input.
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int sidebarWidth;
    private int contentX;

    public ClickGuiScreen(FpsHud fpsHud, KeyMapping openGuiKey) {
        super(Component.literal("PingPlus"));
        this.fpsHud = fpsHud;
        this.openGuiKey = openGuiKey;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - WINDOW_MARGIN * 2);
        panelHeight = Math.min(MAX_PANEL_HEIGHT, height - WINDOW_MARGIN * 2);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        sidebarWidth = panelWidth >= 470 ? 116 : 82;
        contentX = panelX + sidebarWidth + CONTENT_PADDING;

        int availableWidth = panelWidth - sidebarWidth - CONTENT_PADDING * 2;
        addRenderableWidget(new FpsButton(contentX, panelY + 50, Math.min(CARD_WIDTH, availableWidth), fpsHud));
        addRenderableWidget(new FlatButton(panelX + panelWidth - 33, panelY + 12,
                21, 20, "x", () -> false, this::onClose));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawPanel(graphics);
        drawLabels(graphics);
    }

    private void drawPanel(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, width, height, 0xA0090A10);
        GuiTheme.drawRoundedRect(graphics, panelX - 3, panelY + 5, panelWidth + 6, panelHeight + 3, 11, 0x55000000);
        GuiTheme.drawRoundedRect(graphics, panelX, panelY, panelWidth, panelHeight, 9, GuiTheme.BORDER);
        GuiTheme.drawRoundedRect(graphics, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, 8, GuiTheme.PANEL);
        graphics.fill(panelX + sidebarWidth, panelY + 12,
                panelX + sidebarWidth + 1, panelY + panelHeight - 12, GuiTheme.BORDER);
    }

    private void drawLabels(GuiGraphicsExtractor graphics) {
        GuiTheme.drawRoundedRect(graphics, panelX + 12, panelY + 15, 23, 23, 6, GuiTheme.ACCENT);
        graphics.text(font, "P", panelX + 21, panelY + 23, GuiTheme.PANEL, false);
        if (sidebarWidth > 100) {
            graphics.text(font, "PINGPLUS", panelX + 41, panelY + 23, GuiTheme.TEXT, false);
        }
        graphics.text(font, "PVP CLIENT", panelX + 12, panelY + 46, GuiTheme.MUTED, false);
        graphics.text(font, "Modules", contentX, panelY + 18, GuiTheme.TEXT, false);
        graphics.fill(contentX, panelY + panelHeight - 31,
                panelX + panelWidth - CONTENT_PADDING, panelY + panelHeight - 30, GuiTheme.BORDER);
        graphics.text(font, "ESC to close", contentX, panelY + panelHeight - 19, GuiTheme.MUTED, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (openGuiKey.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (openGuiKey.matchesMouse(event)) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
    }
}
