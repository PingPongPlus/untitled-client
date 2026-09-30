package pingplus.voicechat.client.gui.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import pingplus.voicechat.client.VoicechatClient;

/** Dedicated HUD layout screen; chat is no longer used as the editor. */
public final class HudEditorScreen extends Screen {
    public HudEditorScreen() {
        super(Component.literal("Edit HUD"));
    }

    @Override protected void init() {
        HudEditor.finish();
        HudEditor.controls().forEach(this::addWidget);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        HudEditor.render(graphics, mouseX, mouseY, delta, true);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (VoicechatClient.hudEditorKey() != null && VoicechatClient.hudEditorKey().matchesMouse(event)) {
            onClose();
            return true;
        }
        return HudEditor.click(this, event, doubleClick) || super.mouseClicked(event, doubleClick);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return HudEditor.drag(event.x(), event.y()) || super.mouseDragged(event, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return HudEditor.finish() || super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return HudEditor.scroll(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (VoicechatClient.hudEditorKey() != null && VoicechatClient.hudEditorKey().matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public void onClose() {
        HudEditor.finish();
        HudEditor.controls().forEach(control -> control.setFocused(false));
        minecraft.gui.setScreen(null);
    }

    @Override protected void updateNarrationState(NarrationElementOutput output) {
        super.updateNarrationState(output);
        output.add(NarratedElementType.HINT, Component.literal(
                "HUD editing. Drag a widget to move it. Drag its bottom right corner or scroll to resize. "
                + "Right click to reset. Press Escape to close."));
    }

    @Override public boolean isPauseScreen() { return false; }
}
