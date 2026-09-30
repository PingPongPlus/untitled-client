package pingplus.voicechat.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pingplus.voicechat.client.VoiceConnection;

/** Voice connection status and recently audible speakers in local HUD coordinates. */
public final class VoiceHud {
    private final VoiceConnection voice;

    public VoiceHud(VoiceConnection voice) {
        this.voice = voice;
    }

    public boolean isEnabled() {
        return voice.settings.enabled;
    }

    public void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();

        if (!voice.settings.enabled || client.level == null) {
            return;
        }

        String label;

        if (!voice.connected()) {
            label = voice.status;
        } else if (voice.settings.deafened) {
            label = "Voice: deafened";
        } else if (voice.settings.muted) {
            label = "Voice: microphone muted";
        } else if (voice.transmitting()) {
            label = voice.inputPeak() > 0.002
                    ? "Voice: transmitting audio"
                    : "Voice: sending silence (no microphone signal)";
        } else {
            label = voice.status;
        }

        graphics.text(
                client.font,
                client.font.plainSubstrByWidth(label, 292),
                4,
                4,
                voice.connected() ? 0xFF80DD99 : 0xFFFFCC80
        );

        int y = 16;

        for (var entry : voice.talking.entrySet()) {
            if (y > 68) {
                break;
            }

            boolean recentlyTalking =
                    System.currentTimeMillis() - entry.getValue() < 300;

            boolean audible =
                    voice.settings.volume(entry.getKey()) > 0
                            && !voice.settings.deafened;

            if (recentlyTalking && audible) {
                String playerName = voice.players.getOrDefault(
                        entry.getKey(),
                        entry.getKey().toString().substring(0, 8)
                );

                graphics.text(
                        client.font,
                        client.font.plainSubstrByWidth("Speaking: " + playerName, 292),
                        4,
                        y,
                        0xFFFFFFFF
                );

                y += 12;
            }
        }
    }
}
