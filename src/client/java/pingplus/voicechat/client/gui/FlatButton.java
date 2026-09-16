package pingplus.voicechat.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;

/** A themed button that keeps Minecraft's focus, hit testing, sound, and narration. */
public class FlatButton extends AbstractWidget {
    private static final double HOVER_RESPONSE = 18.0;
    private static final double MAX_FRAME_SECONDS = 0.1;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final Runnable action;
    private final BooleanSupplier selected;
    private double hoverProgress;
    private long lastFrameNanos = System.nanoTime();

    public FlatButton(int x, int y, int width, int height, String label,
                      BooleanSupplier selected, Runnable action) {
        super(x, y, width, height, Component.literal(label));
        this.selected = selected;
        this.action = action;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        updateHoverAnimation();
        boolean selected = isSelected();
        double accentStrength = (selected ? 0.10 : 0) + hoverProgress * 0.09;
        int background = GuiTheme.blendColors(GuiTheme.CARD, GuiTheme.ACCENT, accentStrength);

        GuiTheme.drawRoundedRect(graphics, getX(), getY(), width, height, 6, borderColor(selected));
        GuiTheme.drawRoundedRect(graphics, getX() + 1, getY() + 1, width - 2, height - 2, 5, background);
        drawContents(graphics, selected);
    }

    protected boolean isSelected() {
        return selected.getAsBoolean();
    }

    protected void drawContents(GuiGraphicsExtractor graphics, boolean selected) {
        var font = Minecraft.getInstance().font;
        String label = font.plainSubstrByWidth(getMessage().getString(), width - 16);
        int textColor = selected ? GuiTheme.ACCENT : GuiTheme.TEXT;
        graphics.text(font, label, getX() + 8, getY() + (height - 8) / 2, textColor, false);
    }

    private int borderColor(boolean selected) {
        if (isFocused()) {
            return GuiTheme.ACCENT;
        }
        if (selected) {
            return GuiTheme.blendColors(GuiTheme.BORDER, GuiTheme.ACCENT, 0.4);
        }
        return GuiTheme.BORDER;
    }

    private void updateHoverAnimation() {
        long now = System.nanoTime();
        double elapsedSeconds = Math.min(MAX_FRAME_SECONDS, (now - lastFrameNanos) / NANOS_PER_SECOND);
        lastFrameNanos = now;
        double target = isHoveredOrFocused() ? 1 : 0;

        // Exponential interpolation keeps the transition speed independent of FPS.
        double blend = 1 - Math.exp(-elapsedSeconds * HOVER_RESPONSE);
        hoverProgress += (target - hoverProgress) * blend;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        action.run();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean activationKey = event.key() == GLFW.GLFW_KEY_ENTER
                || event.key() == GLFW.GLFW_KEY_KP_ENTER
                || event.key() == GLFW.GLFW_KEY_SPACE;
        if (active && visible && isFocused() && activationKey) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            action.run();
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
