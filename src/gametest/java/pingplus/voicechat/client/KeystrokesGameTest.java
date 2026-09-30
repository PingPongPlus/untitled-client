package pingplus.voicechat.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import pingplus.voicechat.client.gui.ClickGuiScreen;
import pingplus.voicechat.client.hud.ArraylistHud;
import pingplus.voicechat.client.hud.CoordinatesHud;
import pingplus.voicechat.client.hud.FpsHud;
import pingplus.voicechat.client.hud.KeystrokesHud;
import pingplus.voicechat.client.hud.KeystrokesHud.Key;
import pingplus.voicechat.client.hud.editor.HudEditor;
import pingplus.voicechat.client.hud.editor.HudEditorScreen;

/** Real key/mouse events, rebinding, options, saved preferences and HUD editing. */
final class KeystrokesGameTest {
    static void run(ClientGameTestContext context) {
        var hud = KeystrokesHud.INSTANCE;
        var saved = context.computeOnClient(client -> new Saved(hud.isEnabled(), hud.isGlass(), hud.isEdges(),
                hud.isMouseButtons(), hud.isSpaceBar(), client.options.keyUp.saveString()));
        try {
            context.runOnClient(client -> {
                restore(hud, new Saved(true, true, true, true, true, saved.forward));
                client.player.setXRot(-60);
            });
            context.setScreen(() -> null);
            for (var control : controls()) {
                context.getInput().holdKey(control.mapping);
                context.waitTicks(2);
                context.runOnClient(client -> check(hud.isPressed(control.key), "Held key not highlighted: " + control.key));
                context.getInput().releaseKey(control.mapping);
                context.runOnClient(client -> check(!hud.isPressed(control.key), "Released key stayed highlighted: " + control.key));
            }
            context.runOnClient(client -> {
                client.options.keyUp.setKey(InputConstants.getKey("key.keyboard.r"));
                KeyMapping.resetMapping();
                check(hud.label(Key.FORWARD).equals("R"), "Rebound movement label did not update");
            });
            context.getInput().holdKey(options -> options.keyUp);
            context.getInput().holdMouse(0);
            context.getInput().holdMouse(1);
            context.waitTicks(5);
            context.runOnClient(client -> check(hud.isPressed(Key.FORWARD) && hud.isPressed(Key.ATTACK) && hud.isPressed(Key.USE),
                    "Actual rebound key and mouse buttons must highlight together"));
            context.takeScreenshot("keystrokes-pressed");
            context.getInput().releaseKey(options -> options.keyUp);
            context.getInput().releaseMouse(0);
            context.getInput().releaseMouse(1);
            context.runOnClient(client -> {
                client.options.keyUp.setKey(InputConstants.getKey(saved.forward));
                KeyMapping.resetMapping();
                for (Key key : Key.values()) check(!hud.isPressed(key), "Released input stayed pressed: " + key);
            });

            var fps = new FpsHud();
            var coords = new CoordinatesHud();
            context.setScreen(() -> new ClickGuiScreen(fps, VoicechatClient.openGuiKey(), coords, new ArraylistHud(fps, coords)));
            press(context, "Keystrokes options");
            context.waitTicks(3);
            context.takeScreenshot("keystrokes-options");
            press(context, "Keystrokes glass");
            press(context, "Keystrokes edges");
            press(context, "Mouse buttons");
            press(context, "Space bar");
            context.runOnClient(client -> {
                check(hud.isEnabled() && !hud.isGlass() && !hud.isEdges() && !hud.isMouseButtons() && !hud.isSpaceBar(),
                        "Gear toggles must update options without disabling the widget");
                checkSaved(true, false, false, false, false);
            });
            context.setScreen(() -> null);
            context.waitTicks(5);
            context.takeScreenshot("keystrokes-transparent");

            context.setScreen(() -> new ClickGuiScreen(fps, VoicechatClient.openGuiKey(), coords, new ArraylistHud(fps, coords)));
            press(context, "Keystrokes options");
            for (String option : List.of("Keystrokes glass", "Keystrokes edges", "Mouse buttons", "Space bar")) press(context, option);
            press(context, "Close Keystrokes options");
            press(context, "Keystrokes");
            context.runOnClient(client -> {
                check(!hud.isEnabled(), "Main toggle did not disable keystrokes");
                checkSaved(false, true, true, true, true);
            });
            press(context, "Keystrokes");

            context.setScreen(HudEditorScreen::new);
            context.waitTicks(3);
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                var before = HudEditor.bounds("keystrokes", screen.width, screen.height);
                var down = pointer(before.x() + 8, before.y() + 8);
                check(screen.mouseClicked(down, false), "Keystrokes editor must accept dragging");
                var move = pointer(down.x() + 70, down.y() + 25);
                screen.mouseDragged(move, 70, 25);
                screen.mouseReleased(move);
                var moved = HudEditor.bounds("keystrokes", screen.width, screen.height);
                check(Math.abs(moved.x() - before.x() - 70) < 1 && Math.abs(moved.y() - before.y() - 25) < 1,
                        "Keystrokes widget did not move");
                screen.mouseScrolled(moved.x() + 8, moved.y() + 8, 0, 1);
                check(HudEditor.bounds("keystrokes", screen.width, screen.height).scale() > moved.scale(),
                        "Keystrokes widget did not resize");
                screen.mouseClicked(new MouseButtonEvent(moved.x() + 8, moved.y() + 8, new MouseButtonInfo(1, 0)), false);
                client.options.keyUp.setDown(true);
                check(!hud.isPressed(Key.FORWARD), "HUD editor input must not highlight gameplay keys");
                client.options.keyUp.setDown(false);
            });
            context.takeScreenshot("keystrokes-editor");
            System.out.println("PASS: keystrokes keyboard/mouse hold/release, rebinding, menu suppression, saved gear options, toggle, drag and resize");
        } finally {
            for (var control : controls()) context.getInput().releaseKey(control.mapping);
            context.getInput().releaseMouse(0);
            context.getInput().releaseMouse(1);
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                client.options.keyUp.setKey(InputConstants.getKey(saved.forward));
                KeyMapping.resetMapping();
                restore(hud, saved);
                client.player.setXRot(7);
            });
        }
    }

    private static List<Control> controls() {
        return List.of(new Control(Key.FORWARD, options -> options.keyUp), new Control(Key.LEFT, options -> options.keyLeft),
                new Control(Key.BACKWARD, options -> options.keyDown), new Control(Key.RIGHT, options -> options.keyRight),
                new Control(Key.JUMP, options -> options.keyJump));
    }
    private record Control(Key key, Function<Options, KeyMapping> mapping) {}
    private record Saved(boolean enabled, boolean glass, boolean edges, boolean mouse, boolean space, String forward) {}
    private static void restore(KeystrokesHud hud, Saved saved) {
        if (hud.isEnabled() != saved.enabled) hud.toggle();
        if (hud.isGlass() != saved.glass) hud.toggleGlass();
        if (hud.isEdges() != saved.edges) hud.toggleEdges();
        if (hud.isMouseButtons() != saved.mouse) hud.toggleMouseButtons();
        if (hud.isSpaceBar() != saved.space) hud.toggleSpaceBar();
    }
    private static void press(ClientGameTestContext context, String label) {
        context.runOnClient(client -> {
            var screen = client.gui.screen();
            var button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(b -> b.getMessage().getString().equals(label)).findFirst().orElseThrow();
            var event = pointer((button.getX() + button.getWidth() / 2.0) * ClickGuiScreen.UI_SCALE,
                    (button.getY() + button.getHeight() / 2.0) * ClickGuiScreen.UI_SCALE);
            check(screen.mouseClicked(event, false), "Control did not handle click: " + label);
            screen.mouseReleased(event);
        });
    }
    private static MouseButtonEvent pointer(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)); }
    private static void checkSaved(boolean... expected) {
        var values = new Properties();
        try (var reader = Files.newBufferedReader(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                .resolve("voicechat-keystrokes-hud.properties"))) { values.load(reader); }
        catch (java.io.IOException e) { throw new AssertionError(e); }
        var keys = List.of("enabled", "glass", "edges", "mouseButtons", "spaceBar");
        for (int i = 0; i < keys.size(); i++) check(Boolean.toString(expected[i]).equals(values.getProperty(keys.get(i))),
                "Keystrokes preference was not saved: " + keys.get(i));
    }
    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
}
