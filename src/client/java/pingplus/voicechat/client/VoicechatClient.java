package pingplus.voicechat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoicechatClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("laby-voicechat");
    @Override public void onInitializeClient() {
        VoiceConnection voice = new VoiceConnection(Minecraft.getInstance(), VoiceSettings.load());
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("voicechat", "controls"));
        KeyMapping menu = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voicechat.settings", GLFW.GLFW_KEY_V, category));
        KeyMapping talk = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voicechat.talk", GLFW.GLFW_KEY_CAPS_LOCK, category));
        KeyMapping mute = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.voicechat.mute", GLFW.GLFW_KEY_M, category));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("voicechat", "status"), (graphics, delta) -> {
            var mc = Minecraft.getInstance();
            if (!voice.settings.enabled || mc.level == null) return;
            String label = !voice.connected() ? voice.status : voice.settings.deafened ? "Voice: deafened"
                : voice.settings.muted ? "Voice: microphone muted" : voice.transmitting()
                    ? (voice.inputPeak() > 0.002 ? "Voice: transmitting audio" : "Voice: sending silence (no microphone signal)") : voice.status;
            graphics.text(mc.font, label, 8, 8, voice.connected() ? 0xFF80DD99 : 0xFFFFCC80);
            int y = 20;
            for (var entry : voice.talking.entrySet()) {
                if (y > 68) break;
                if (System.currentTimeMillis() - entry.getValue() < 300 && voice.settings.volume(entry.getKey()) > 0 && !voice.settings.deafened) {
                    graphics.text(mc.font, "Speaking: " + voice.players.getOrDefault(entry.getKey(), entry.getKey().toString().substring(0, 8)), 8, y, 0xFFFFFFFF);
                    y += 12;
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menu.consumeClick()) mc.gui.setScreen(new VoiceScreen(mc.gui.screen(), voice));
            while (mute.consumeClick()) { voice.settings.muted = !voice.settings.muted; voice.settings.save(); voice.updateProperties(); }
            voice.pushToTalk = talk.isDown() && mc.gui.screen() == null && mc.isWindowActive();
            voice.tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> { if (voice.settings.enabled) voice.connect(); });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> voice.disconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> voice.disconnect());
    }
}
