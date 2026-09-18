package pingplus.voicechat.client.gui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import pingplus.voicechat.client.PlayerSettings;
import net.minecraft.client.gui.components.EditBox;

import javax.swing.plaf.SliderUI;

/** A small settings screen opened with Right Shift. */
public final class ClickGuiScreen extends Screen {
    private final FpsHud fpsHud;
    private final KeyMapping openGuiKey;
    private final CoordinatesHud coordinatesHud;

    public ClickGuiScreen(FpsHud fpsHud, KeyMapping openGuiKey, CoordinatesHud coordinatesHud) {
        super(Component.literal("Manage Minecraft Rendering like a Boss"));
        this.fpsHud = fpsHud;
        this.openGuiKey = openGuiKey;
        this.coordinatesHud = coordinatesHud;
    }

    // Minecraft calls init() when the screen opens or the window is resized.
    @Override
    protected void init() {
        int buttonX = width / 2 - 100;
        int buttonY = height / 2 - 10;
        buttonY = buttonY - 90;

        // The code inside this callback runs when the FPS button is pressed.
        addRenderableWidget(Button.builder(fpsLabel(), button -> {
            fpsHud.toggle();
            button.setMessage(fpsLabel());
        }).bounds(buttonX, buttonY, 200, 20).build());


        addRenderableWidget(Button.builder(coordinatesLabel(), button -> {
            coordinatesHud.toggle();
            button.setMessage(coordinatesLabel());
        }).bounds(buttonX, buttonY + 30, 200, 20).build());

        EditBox widthInput = new EditBox(
                font,
                buttonX, buttonY + 60,
                66, 20,
                Component.literal("Player xScale")
        );

        widthInput.setValue(Float.toString(PlayerSettings.xScale));
        widthInput.setMaxLength(1000);

// Runs whenever the text changes.
        widthInput.setResponder(text -> {
            try {
                float number = Float.parseFloat(text);

                if (Float.isFinite(number) && number >= -100000.0F && number <= 100000.0F) {
                    PlayerSettings.xScale = number;
                }
            } catch (NumberFormatException ignored) {
                // Empty or unfinished input leaves the previous scale unchanged.
            }
        });
        addRenderableWidget(widthInput);

        EditBox yScaleInput = new EditBox(
                font,
                buttonX + 66, buttonY + 60,
                66, 20,
                Component.literal("Player yScale")
        );

        yScaleInput.setValue(Float.toString(PlayerSettings.yScale));
        yScaleInput.setMaxLength(1000);

// Runs whenever the text changes.
        yScaleInput.setResponder(text -> {
            try {
                float number = Float.parseFloat(text);

                if (Float.isFinite(number) && number >= -100000.0F && number <= 100000.0F) {
                    PlayerSettings.yScale = number;
                }
            } catch (NumberFormatException ignored) {
                // Empty or unfinished input leaves the previous scale unchanged.
            }
        });
        addRenderableWidget(yScaleInput);



        EditBox zScaleInput = new EditBox(
                font,
                buttonX + 132, buttonY + 60,
                66, 20,
                Component.literal("Player zScale")
        );

        zScaleInput.setValue(Float.toString(PlayerSettings.zScale));
        zScaleInput.setMaxLength(1000);

// Runs whenever the text changes.
        zScaleInput.setResponder(text -> {
            try {
                float number = Float.parseFloat(text);

                if (Float.isFinite(number) && number >= -100000.0F && number <= 100000.0F) {
                    PlayerSettings.zScale = number;
                }
            } catch (NumberFormatException ignored) {
                // Empty or unfinished input leaves the previous scale unchanged.
            }
        });
        addRenderableWidget(zScaleInput);

        EditBox headScale = new EditBox(
                font,
                buttonX, buttonY + 90, 200, 20, Component.literal("HeadScale")
        );
        headScale.setValue(Float.toString(PlayerSettings.xyzHeadscale));
        headScale.setMaxLength(1000);
        headScale.setResponder(text -> {
            try {
                float number = Float.parseFloat(text);

                if (Float.isFinite(number) && number >= -100000.0F && number <= 100000.0F) {
                    PlayerSettings.xyzHeadscale = number;
                }
            } catch (NumberFormatException ignored) {
                // Empty or unfinished input leaves the previous scale unchanged.
            }
        });
        addRenderableWidget(headScale);

        addRenderableWidget(Button.builder(Component.literal("x"), button -> {
            onClose();
        }).bounds(buttonX, buttonY + 150, 200, 20).build());

        addRenderableWidget(Button.builder(BodyLabel(), button -> {
            PlayerSettings.mainBodyPart = !PlayerSettings.mainBodyPart;
            button.setMessage(BodyLabel());
        }).bounds(buttonX, buttonY + 120, 200, 20).build());
    }

    private Component BodyLabel(){
        return Component.literal("Main Boddy Part: " + (PlayerSettings.mainBodyPart ? "ON" : "OFF"));
    }
    private Component coordinatesLabel(){
        return Component.literal("Coordinates: " +(coordinatesHud.isEnabled() ? "ON" : "OFF"));
    }


    private Component fpsLabel() {
        return Component.literal("FPS: " + (fpsHud.isEnabled() ? "ON" : "OFF"));
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
