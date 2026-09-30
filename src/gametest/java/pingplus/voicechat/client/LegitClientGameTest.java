package pingplus.voicechat.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import pingplus.voicechat.client.gui.*;
import pingplus.voicechat.client.gui.hud.HudEditor;
import pingplus.voicechat.client.gui.hud.HudEditorScreen;

/** Confirms the reduced client starts without removed feature classes and retains its HUD. */
public final class LegitClientGameTest {
    public static void run(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.gui.overlay() == null);
            context.runOnClient(client -> {
                for (String removed : new String[]{
                        "pingplus.voicechat.client.KillAuraFeature", "pingplus.voicechat.client.HandSwapFeature",
                        "pingplus.voicechat.client.AutoToolsFeature", "pingplus.voicechat.client.FastPlaceFeature",
                        "pingplus.voicechat.client.XrayFeature", "pingplus.voicechat.client.StorageEspFeature",
                        "pingplus.voicechat.client.ProjectilePreviewRenderer", "pingplus.voicechat.client.gui.PlayerHealthBarRenderer",
                        "pingplus.voicechat.client.gui.MobHealthBarRenderer", "pingplus.voicechat.client.slayer.SlayerOutlineHook",
                        "pingplus.voicechat.mixin.client.FullbrightMixin", "pingplus.voicechat.client.gui.NotificationHud" }) {
                    try { Class.forName(removed); throw new AssertionError("Removed feature is loadable: " + removed); }
                    catch (ClassNotFoundException expected) { }
                }
                client.getWindow().setWindowed(1440, 900); client.options.guiScale().set(2);
                check(PingHud.INSTANCE.text().equals("Ping  Local"), "ping remains available");
                check(!PlayerSettings.hideHealth && !PlayerSettings.hideHunger, "normal survival HUD remains available");
            });
            var fps = new FpsHud(); var coords = new CoordinatesHud(); var arraylist = new ArraylistHud(fps, coords);
            context.setScreen(() -> new ClickGuiScreen(fps, VoicechatClient.openGuiKey(), coords, arraylist));
            context.waitTicks(5);
            context.runOnClient(client -> {
                var labels = client.gui.screen().children().stream().filter(child -> child instanceof Button)
                        .map(child -> ((Button) child).getMessage().getString()).toList();
                for (String removed : new String[]{"Player HP bars", "KillAura", "Hand swap", "Projectile preview",
                        "Auto Tools", "Fast Place", "Storage ESP", "Xray",
                        "Fullbright", "Slayer outline", "Boss lines", "Player outline", "Glass notifications", "Killaura: Z"})
                    check(!labels.contains(removed), "removed menu control: " + removed);
                check(labels.contains("Ping") && labels.contains("Spotify") && labels.contains("HUD editor: G"), "retained controls");
                check(labels.contains("All off") && labels.contains("Glass style") && labels.contains("FPS options"), "shared UI controls");
            });
            context.takeScreenshot("legit-control-center");
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                var ping = screen.children().stream().filter(child -> child instanceof Button).map(child -> (Button)child)
                        .filter(button -> button.getMessage().getString().equals("Ping")).findFirst().orElseThrow();
                boolean enabled = PingHud.INSTANCE.isEnabled();
                var down = new MouseButtonEvent((ping.getX() + 5) * ClickGuiScreen.UI_SCALE,
                        (ping.getY() + 5) * ClickGuiScreen.UI_SCALE, new MouseButtonInfo(0, 0));
                check(screen.mouseClicked(down, false), "ping toggle click"); screen.mouseReleased(down);
                check(PingHud.INSTANCE.isEnabled() != enabled, "ping toggles in reduced menu");
                PingHud.INSTANCE.toggle();
            });
            context.setScreen(HudEditorScreen::new);
            context.waitTicks(5); context.takeScreenshot("legit-hud-editor");
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                var ping = HudEditor.bounds("ping", screen.width, screen.height);
                check(ping.width() > 0 && ping.height() > 0, "ping registered in HUD editor");
                check(HudEditor.bounds("spotify", screen.width, screen.height).width() > 0, "Spotify retained in HUD editor");
                screen.onClose();
            });
            context.waitTicks(5); context.takeScreenshot("legit-gameplay");
        }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
