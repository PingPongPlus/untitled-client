package pingplus.voicechat.client.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** A small settings screen opened with Right Shift. */
public final class ClickGuiScreen extends Screen {
    private final FpsHud fpsHud;
    private final KeyMapping openGuiKey;
    private final CoordinatesHud coordinatesHud;

    public ClickGuiScreen(FpsHud fpsHud, KeyMapping openGuiKey, CoordinatesHud coordinatesHud) {
        super(Component.literal("PingPlus"));
        this.fpsHud = fpsHud;
        this.openGuiKey = openGuiKey;
        this.coordinatesHud = coordinatesHud;
    }

    // Minecraft calls init() when the screen opens or the window is resized.
    @Override
    protected void init() {
        int buttonX = width / 2 - 100;
        int buttonY = height / 2 - 10;

        // The code inside this callback runs when the FPS button is pressed.
        addRenderableWidget(Button.builder(fpsLabel(), button -> {
            fpsHud.toggle();
            button.setMessage(fpsLabel());
        }).bounds(buttonX, buttonY, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("x"), button -> {
            onClose();
        }).bounds(buttonX, buttonY + 60, 200, 20).build());

        addRenderableWidget(Button.builder(coordinatesLabel(), button -> {
            coordinatesHud.toggle();
            button.setMessage(coordinatesLabel());
        }).bounds(buttonX, buttonY + 30, 200, 20).build());
    }
    private Component coordinatesLabel(){
        return Component.literal("Coordinates: " +(coordinatesHud.isEnabled() ? "ON" : "OFF"));
    }


    private Component fpsLabel() {
        return Component.literal("FPS: " + (fpsHud.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int centerY = height / 2;

        // Colors use 0xAARRGGBB: alpha, red, green, blue.
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.fill(centerX - 120, centerY - 50, centerX + 120, centerY + 90, 0xFF17171F);
        graphics.centeredText(font, title, centerX, centerY - 32, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (openGuiKey.matches(event)) {
            onClose();
            return true;
        }
        // Minecraft handles Escape and keyboard navigation.
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Also support the opening key being rebound to a mouse button.
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
