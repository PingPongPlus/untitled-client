package pingplus.voicechat.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Uses the native slider for dragging and keyboard input; only drawing is customized. */
public final class OpacitySlider extends AbstractSliderButton {
    private final ClientConfig config;

    public OpacitySlider(int x, int y, int width, ClientConfig config) {
        super(x, y, width, 42, Component.empty(), config.hudOpacity());
        this.config = config;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal("HUD background  " + Math.round(value * 100) + "%"));
    }

    @Override
    protected void applyValue() {
        // Update the live preview during dragging; save on release or screen removal.
        config.setHudOpacity(value);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        config.save();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        GuiTheme.drawRoundedRect(graphics, getX(), getY(), width, height, 6, isFocused() ? GuiTheme.BORDER : GuiTheme.CARD);
        graphics.text(Minecraft.getInstance().font, getMessage(), getX() + 10, getY() + 8, GuiTheme.TEXT, false);
        int thumbX = getX() + 4 + (int) (value * (width - 8));
        GuiTheme.drawRoundedRect(graphics, getX() + 4, getY() + 29, width - 8, 3, 1, GuiTheme.BORDER);
        graphics.fill(getX() + 4, getY() + 29, thumbX, getY() + 32, config.accentColor());
        GuiTheme.drawRoundedRect(graphics, thumbX - 3, getY() + 26, 6, 9, 3, GuiTheme.TEXT);
    }
}
