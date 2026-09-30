package pingplus.voicechat.client;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pingplus.voicechat.client.gui.glass.GlassStyle;

/** A compact, non-pausing editor for the same per-player gain used by the voice menu. */
public final class PlayerVoiceVolumeScreen extends Screen {
    private static final int HEIGHT = 128;
    private final UUID playerId;
    private final Component playerName;
    private final VoiceSettings settings;
    private int panelX, panelY, panelWidth;
    private VolumeSlider slider;
    private Button mute;

    public PlayerVoiceVolumeScreen(UUID playerId, Component playerName, VoiceSettings settings) {
        super(GlassStyle.label("Voice volume"));
        this.playerId = playerId;
        this.playerName = playerName.copy();
        this.settings = settings;
    }

    @Override protected void init() {
        panelWidth = Math.min(240, width - 16);
        panelX = (width - panelWidth) / 2;
        panelY = Math.max(8, (height - HEIGHT) / 2);
        int x = panelX + 12, contentWidth = panelWidth - 24, half = (contentWidth - 8) / 2;
        slider = addRenderableWidget(new VolumeSlider(x, panelY + 48, contentWidth));
        mute = addRenderableWidget(Button.builder(muteLabel(), b -> changeVolume(settings.volume(playerId) == 0 ? 1 : 0))
                .bounds(x, panelY + 74, half, 20).build());
        addRenderableWidget(Button.builder(GlassStyle.label("Reset"), b -> changeVolume(1))
                .bounds(x + half + 8, panelY + 74, contentWidth - half - 8, 20).build());
        addRenderableWidget(Button.builder(GlassStyle.label("Done"), b -> onClose())
                .bounds(x, panelY + 100, contentWidth, 20).build());
    }

    private Component muteLabel() { return GlassStyle.label(settings.volume(playerId) == 0 ? "Unmute" : "Mute"); }
    private void changeVolume(double level) {
        settings.setVolume(playerId, level);
        slider.setLevel(settings.volume(playerId));
        mute.setMessage(muteLabel());
    }

    private final class VolumeSlider extends AbstractSliderButton {
        VolumeSlider(int x, int y, int width) {
            super(x, y, width, 20, Component.empty(), settings.volume(playerId) / 2);
            updateMessage();
        }
        void setLevel(double level) { value = level / 2; updateMessage(); }
        @Override protected void updateMessage() { setMessage(GlassStyle.label("Volume: " + Math.round(value * 200) + "%")); }
        @Override protected void applyValue() {
            settings.setVolume(playerId, value * 2);
            mute.setMessage(muteLabel());
        }
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x30000000);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        GlassStyle.surface(graphics, panelX, panelY, panelWidth, HEIGHT, .94f, .12f);
        graphics.nextStratum();
        graphics.centeredText(font, title, width / 2, panelY + 12, GlassStyle.TEXT);
        graphics.centeredText(font, GlassStyle.label(font.plainSubstrByWidth(playerName.getString(), panelWidth - 24)),
                width / 2, panelY + 28, GlassStyle.MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(null); }
}
