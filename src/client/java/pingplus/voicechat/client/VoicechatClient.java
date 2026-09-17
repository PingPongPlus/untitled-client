package pingplus.voicechat.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import pingplus.voicechat.client.gui.ClickGuiScreen;
import pingplus.voicechat.client.gui.CoordinatesHud;
import pingplus.voicechat.client.gui.FpsHud;

/** Client entry point: creates the FPS toggle and connects it to Fabric events. */
public class VoicechatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FpsHud fpsHud = new FpsHud();
        CoordinatesHud coordinatesHud = new CoordinatesHud();
        KeyMapping openGuiKey = registerOpenGuiKey();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                // Do not replace inventory, chat, or another mod's screen.
                if (client.gui.screen() == null && client.player != null) {
                    client.gui.setScreen(new ClickGuiScreen(fpsHud, openGuiKey));
                }
            }
        });

        // Attaching to a vanilla layer inherits the HUD visibility condition (F1).
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("voicechat", "client_hud"), fpsHud::extract);
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("voicechat", "coordinates_hud"),
                coordinatesHud::extract
        );

    }

    private KeyMapping registerOpenGuiKey() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("voicechat", "client"));
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.voicechat.click_gui", GLFW.GLFW_KEY_RIGHT_SHIFT, category));
    }
}
