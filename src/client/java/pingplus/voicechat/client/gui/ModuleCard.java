package pingplus.voicechat.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** A module toggle with its own contents, sharing button input and hover behavior. */
public final class ModuleCard extends FlatButton {
    public static final int HEIGHT = 66;
    private final ClientModule module;

    public ModuleCard(int x, int y, int width, ClientModule module, ClientConfig config) {
        super(x, y, width, HEIGHT, module.title, config, () -> config.isEnabled(module), () -> {
            config.toggleModule(module);
            config.save();
        });
        this.module = module;
        setTooltip(Tooltip.create(Component.literal(module.title + ": " + module.description)));
    }

    @Override
    protected void drawContents(GuiGraphicsExtractor graphics, boolean enabled) {
        var font = Minecraft.getInstance().font;
        int accent = config.accentColor();
        int iconBackground = GuiTheme.blendColors(GuiTheme.CARD, accent, 0.18);
        GuiTheme.drawRoundedRect(graphics, getX() + 10, getY() + 10, 28, 25, 5, iconBackground);
        graphics.text(font, module.icon, getX() + 24 - font.width(module.icon) / 2, getY() + 18, accent, false);

        String title = font.plainSubstrByWidth(module.title, width - 89);
        String description = font.plainSubstrByWidth(module.description, width - 20);
        graphics.text(font, title, getX() + 46, getY() + 13, GuiTheme.TEXT, false);
        graphics.text(font, enabled ? "ENABLED" : "DISABLED", getX() + 46, getY() + 26,
                enabled ? accent : GuiTheme.MUTED, false);
        graphics.text(font, description, getX() + 10, getY() + 47, GuiTheme.MUTED, false);
        drawToggle(graphics, enabled);
    }

    private void drawToggle(GuiGraphicsExtractor graphics, boolean enabled) {
        int trackColor = enabled ? config.accentColor() : 0xFF484653;
        int thumbX = getRight() - (enabled ? 20 : 32);
        GuiTheme.drawRoundedRect(graphics, getRight() - 34, getY() + 14, 24, 12, 6, trackColor);
        GuiTheme.drawRoundedRect(graphics, thumbX, getY() + 16, 8, 8, 4, GuiTheme.TEXT);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        String state = isSelected() ? ", enabled" : ", disabled";
        return Component.literal(module.title + state);
    }
}
