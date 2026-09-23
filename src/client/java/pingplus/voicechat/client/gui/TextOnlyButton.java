package pingplus.voicechat.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class TextOnlyButton extends Button {
    public TextOnlyButton(
            int x, int y, int width, int height,
            Component label, OnPress onPress
    ) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float delta
    ) {
        // The shared button mixin replaces this sprite with glass.
        extractDefaultSprite(graphics);

        // Draw the text on top.
        extractDefaultLabel(graphics.textRenderer());
    }
}
