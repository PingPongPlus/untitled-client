package pingplus.voicechat.client.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Owns screen layout and navigation. Widgets handle input; ClientConfig owns settings. */
public final class ClickGuiScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 620;
    private static final int MAX_PANEL_HEIGHT = 366;
    private static final int WINDOW_MARGIN = 8;
    private static final int CONTENT_PADDING = 14;
    private static final int HEADER_HEIGHT = 67;
    private static final int FOOTER_HEIGHT = 32;
    private static final int CARD_GAP = 8;
    private static final int CARD_ROW_HEIGHT = ModuleCard.HEIGHT + CARD_GAP;
    private static final int TWO_COLUMN_MIN_WIDTH = 360;

    private final ClientConfig config;
    private final KeyMapping openGuiKey;
    private final Screen parentScreen;
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private GuiCategory selectedCategory = GuiCategory.ALL;
    private String searchQuery = "";
    private EditBox searchField;

    // All bounds use Minecraft GUI coordinates, which already account for GUI scale.
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int sidebarWidth;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int visibleRows;
    private int columnCount;
    private int firstVisibleRow;
    private int totalRows;
    private boolean noResults;

    public ClickGuiScreen(ClientConfig config, KeyMapping openGuiKey, Screen parentScreen) {
        super(Component.literal("PingPlus | Client modules"));
        this.config = config;
        this.openGuiKey = openGuiKey;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        calculateLayout();
        contentWidgets.clear();
        createNavigation();
        createSearchField();
        refreshContent();
    }

    private void calculateLayout() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - WINDOW_MARGIN * 2);
        panelHeight = Math.min(MAX_PANEL_HEIGHT, height - WINDOW_MARGIN * 2);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        sidebarWidth = panelWidth >= 470 ? 116 : 82;
        contentX = panelX + sidebarWidth + CONTENT_PADDING;
        contentWidth = panelWidth - sidebarWidth - CONTENT_PADDING * 2;
        contentY = panelY + HEADER_HEIGHT;
        columnCount = contentWidth >= TWO_COLUMN_MIN_WIDTH ? 2 : 1;
        visibleRows = Math.max(1, (panelHeight - HEADER_HEIGHT - FOOTER_HEIGHT) / CARD_ROW_HEIGHT);
    }

    private void createNavigation() {
        GuiCategory[] categories = GuiCategory.values();
        for (int index = 0; index < categories.length; index++) {
            GuiCategory category = categories[index];
            addRenderableWidget(new FlatButton(panelX + 8, panelY + 66 + index * 29,
                    sidebarWidth - 16, 25, category.label(), config,
                    () -> selectedCategory == category, () -> selectCategory(category)));
        }
        addRenderableWidget(new FlatButton(panelX + panelWidth - 33, panelY + 12, 21, 20, "x", config,
                () -> false, this::onClose)).setTooltip(Tooltip.create(Component.literal("Close (Escape)")));
    }

    private void createSearchField() {
        searchField = new EditBox(font, contentX + 9, panelY + 39, contentWidth - 18, 16, Component.literal("Search modules"));
        searchField.setBordered(false);
        searchField.setTextColor(GuiTheme.TEXT);
        searchField.setTextShadow(false);
        searchField.setMaxLength(64);
        searchField.setHint(Component.literal("Search modules..."));
        searchField.setValue(searchQuery);
        searchField.setResponder(value -> {
            searchQuery = value;
            firstVisibleRow = 0;
            refreshContent();
        });
        searchField.setVisible(selectedCategory != GuiCategory.APPEARANCE);
        addRenderableWidget(searchField);
    }

    private void selectCategory(GuiCategory category) {
        selectedCategory = category;
        firstVisibleRow = 0;
        searchField.setVisible(category != GuiCategory.APPEARANCE);
        refreshContent();
    }

    private <T extends AbstractWidget> T addContent(T widget) {
        contentWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    private void refreshContent() {
        // Keep the search box (and its cursor) alive when replacing result cards.
        if (contentWidgets.contains(getFocused())) {
            setFocused(null);
        }
        for (AbstractWidget widget : contentWidgets) {
            removeWidget(widget);
        }
        contentWidgets.clear();
        noResults = false;
        totalRows = 0;
        if (selectedCategory == GuiCategory.APPEARANCE) {
            createAppearanceControls();
        } else {
            createModuleCards();
        }
    }

    private void createAppearanceControls() {
        addContent(new FlatButton(contentX, contentY, contentWidth, 28,
                "Accent: " + config.accentName(), config, () -> true, this::cycleAccent));
        addContent(new FlatButton(contentX, contentY + 35, contentWidth, 28,
                "Animations: " + (config.animationsEnabled() ? "On" : "Off"), config,
                config::animationsEnabled, this::toggleAnimations));
        addContent(new OpacitySlider(contentX, contentY + 70, contentWidth, config));
    }

    private void cycleAccent() {
        config.cycleAccent();
        config.save();
        refreshContent();
    }

    private void toggleAnimations() {
        config.toggleAnimations();
        config.save();
        refreshContent();
    }

    private List<ClientModule> matchingModules() {
        String needle = searchQuery.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(ClientModule.values())
                .filter(module -> selectedCategory == GuiCategory.ALL || selectedCategory == module.category)
                .filter(module -> (module.title + " " + module.description).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private void createModuleCards() {
        List<ClientModule> matching = matchingModules();
        noResults = matching.isEmpty();
        totalRows = (matching.size() + columnCount - 1) / columnCount;
        firstVisibleRow = Math.clamp(firstVisibleRow, 0, Math.max(0, totalRows - visibleRows));
        int cardWidth = (contentWidth - (columnCount - 1) * CARD_GAP) / columnCount;
        int firstIndex = firstVisibleRow * columnCount;
        int endIndex = Math.min(matching.size(), (firstVisibleRow + visibleRows) * columnCount);

        // Instantiate only visible rows, so offscreen cards cannot receive focus or clicks.
        for (int index = firstIndex; index < endIndex; index++) {
            int slot = index - firstIndex;
            int cardX = contentX + (slot % columnCount) * (cardWidth + CARD_GAP);
            int cardY = contentY + (slot / columnCount) * CARD_ROW_HEIGHT;
            addContent(new ModuleCard(cardX, cardY, cardWidth, matching.get(index), config));
        }
        createScrollButtons();
    }

    private void createScrollButtons() {
        if (totalRows > visibleRows) {
            addContent(new FlatButton(panelX + panelWidth - 65, panelY + panelHeight - 25, 23, 18, "<", config,
                    () -> false, () -> scroll(-1))).active = firstVisibleRow > 0;
            addContent(new FlatButton(panelX + panelWidth - 38, panelY + panelHeight - 25, 23, 18, ">", config,
                    () -> false, () -> scroll(1))).active = firstVisibleRow < totalRows - visibleRows;
        }
    }

    private void scroll(int direction) {
        int next = Math.clamp(firstVisibleRow + direction, 0, Math.max(0, totalRows - visibleRows));
        if (next != firstVisibleRow) {
            firstVisibleRow = next;
            refreshContent();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (selectedCategory != GuiCategory.APPEARANCE && mouseX >= contentX && mouseX < contentX + contentWidth
                && mouseY >= contentY && mouseY < panelY + panelHeight - 30 && vertical != 0) {
            scroll(vertical > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!searchField.isFocused() && openGuiKey.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (openGuiKey.matchesMouse(event) && !searchField.isFocused()) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawPanel(graphics);
        drawHeader(graphics);
        drawContentHints(graphics);
        drawFooter(graphics);
    }

    private void drawPanel(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, width, height, 0xA0090A10);
        GuiTheme.drawRoundedRect(graphics, panelX - 3, panelY + 5, panelWidth + 6, panelHeight + 3, 11, 0x55000000);
        GuiTheme.drawRoundedRect(graphics, panelX, panelY, panelWidth, panelHeight, 9, GuiTheme.BORDER);
        GuiTheme.drawRoundedRect(graphics, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, 8, GuiTheme.PANEL);
        graphics.fill(panelX + sidebarWidth, panelY + 12, panelX + sidebarWidth + 1, panelY + panelHeight - 12, GuiTheme.BORDER);
    }

    private void drawHeader(GuiGraphicsExtractor graphics) {
        GuiTheme.drawRoundedRect(graphics, panelX + 12, panelY + 15, 23, 23, 6, config.accentColor());
        graphics.text(font, "P", panelX + 21, panelY + 23, GuiTheme.PANEL, false);
        if (sidebarWidth > 100) {
            graphics.text(font, "PINGPLUS", panelX + 41, panelY + 23, GuiTheme.TEXT, false);
        }
        graphics.text(font, "PVP CLIENT", panelX + 12, panelY + 46, GuiTheme.MUTED, false);
        graphics.text(font, selectedCategory.label(), contentX, panelY + 18, GuiTheme.TEXT, false);
        if (searchField.isVisible()) {
            GuiTheme.drawRoundedRect(graphics, contentX, panelY + 34, contentWidth, 25, 5, searchField.isFocused() ? GuiTheme.BORDER : GuiTheme.CARD);
        } else {
            graphics.text(font, "Make it yours.", contentX, panelY + 43, GuiTheme.MUTED, false);
        }
    }

    private void drawContentHints(GuiGraphicsExtractor graphics) {
        if (noResults) {
            graphics.text(font, "No modules found.", contentX + 8, contentY + 20, GuiTheme.TEXT, false);
            graphics.text(font, "Try another search.", contentX + 8, contentY + 36, GuiTheme.MUTED, false);
        }
        if (selectedCategory == GuiCategory.APPEARANCE && panelHeight >= 285) {
            graphics.text(font, "HUD PREVIEW", contentX, contentY + 132, GuiTheme.MUTED, false);
            ClientHud.drawChip(graphics, font, contentX, contentY + 148, "FPS  144", config);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics) {
        graphics.fill(contentX, panelY + panelHeight - 31, panelX + panelWidth - 14, panelY + panelHeight - 30, GuiTheme.BORDER);
        String footer = config.hasSaveFailed()
                ? "Save failed - check log"
                : config.enabledCount() + " enabled  /  " + ClientModule.values().length + " modules";
        int textWidth = contentWidth - (totalRows > visibleRows ? 60 : 0);
        int textColor = config.hasSaveFailed() ? 0xFFFFB86B : GuiTheme.MUTED;
        graphics.text(font, font.plainSubstrByWidth(footer, textWidth), contentX,
                panelY + panelHeight - 19, textColor, false);
        if (panelHeight >= 245) {
            graphics.text(font, "ESC to close", panelX + 12, panelY + panelHeight - 20, GuiTheme.MUTED, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parentScreen);
    }

    @Override
    public void removed() {
        // Includes closing, disconnecting, or being replaced by another screen.
        config.save();
    }
}
