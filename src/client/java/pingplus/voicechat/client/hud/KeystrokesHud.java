package pingplus.voicechat.client.hud;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pingplus.voicechat.client.VoicechatClient;
import pingplus.voicechat.client.gui.glass.GlassStyle;

/** Current Minecraft bindings, drawn as glass keycaps in local HUD coordinates. */
public final class KeystrokesHud {
    private static final int KEY_SIZE = 24, GAP = 4, WIDTH = KEY_SIZE * 3 + GAP * 2;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-keystrokes-hud.properties");
    public static final KeystrokesHud INSTANCE = new KeystrokesHud();
    public enum Key { FORWARD, LEFT, BACKWARD, RIGHT, ATTACK, USE, JUMP }
    private final float[] highlights = new float[Key.values().length];
    private boolean enabled = true, glass = true, edges = true, mouseButtons = true, spaceBar = true;
    private long lastFrame;

    private KeystrokesHud() {
        Properties values = new Properties();
        try (var reader = Files.newBufferedReader(FILE)) { values.load(reader); }
        catch (IOException | IllegalArgumentException ignored) { }
        enabled = value(values, "enabled");
        glass = value(values, "glass");
        edges = value(values, "edges");
        mouseButtons = value(values, "mouseButtons");
        spaceBar = value(values, "spaceBar");
    }

    public boolean isEnabled() { return enabled; }
    public boolean isGlass() { return glass; }
    public boolean isEdges() { return edges; }
    public boolean isMouseButtons() { return mouseButtons; }
    public boolean isSpaceBar() { return spaceBar; }
    public void toggle() { enabled = !enabled; Arrays.fill(highlights, 0); lastFrame = 0; save(); }
    public void toggleGlass() { glass = !glass; save(); }
    public void toggleEdges() { edges = !edges; save(); }
    public void toggleMouseButtons() { mouseButtons = !mouseButtons; save(); }
    public void toggleSpaceBar() { spaceBar = !spaceBar; save(); }
    public int width() { return WIDTH; }
    public int height() { return KEY_SIZE * 2 + GAP + (mouseButtons ? 24 : 0) + (spaceBar ? 22 : 0); }

    public boolean isPressed(Key key) {
        var client = Minecraft.getInstance();
        return enabled && gameplayInput(client) && binding(client, key).isDown();
    }

    public String label(Key key) {
        var binding = binding(Minecraft.getInstance(), key);
        return switch (binding.saveString()) {
            case "key.mouse.left" -> "LMB";
            case "key.mouse.right" -> "RMB";
            case "key.mouse.middle" -> "MMB";
            case "key.keyboard.space" -> "SPACE";
            case "key.keyboard.up" -> "↑";
            case "key.keyboard.left" -> "←";
            case "key.keyboard.down" -> "↓";
            case "key.keyboard.right" -> "→";
            default -> binding.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        };
    }

    public void render(GuiGraphicsExtractor graphics) {
        var client = Minecraft.getInstance();
        if (!enabled || client.player == null) return;
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : (float)Math.min(.1, (now - lastFrame) / 1e9);
        lastFrame = now;
        if (!gameplayInput(client)) Arrays.fill(highlights, 0);
        else for (Key key : Key.values()) {
            float target = isPressed(key) ? 1 : 0;
            highlights[key.ordinal()] += (target - highlights[key.ordinal()]) * (float)(1 - Math.exp(-24 * dt));
        }

        keycap(graphics, Key.FORWARD, KEY_SIZE + GAP, 0, KEY_SIZE, KEY_SIZE);
        keycap(graphics, Key.LEFT, 0, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE);
        keycap(graphics, Key.BACKWARD, KEY_SIZE + GAP, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE);
        keycap(graphics, Key.RIGHT, (KEY_SIZE + GAP) * 2, KEY_SIZE + GAP, KEY_SIZE, KEY_SIZE);
        int y = KEY_SIZE * 2 + GAP;
        if (mouseButtons) {
            y += GAP;
            keycap(graphics, Key.ATTACK, 0, y, (WIDTH - GAP) / 2, 20);
            keycap(graphics, Key.USE, (WIDTH + GAP) / 2, y, (WIDTH - GAP) / 2, 20);
            y += 20;
        }
        if (spaceBar) keycap(graphics, Key.JUMP, 0, y + GAP, WIDTH, 18);
    }

    private void keycap(GuiGraphicsExtractor graphics, Key key, int x, int y, int w, int h) {
        float highlight = highlights[key.ordinal()];
        if (glass) {
            graphics.nextStratum();
            GlassStyle.surface(graphics, x, y, w, h, .88f + .08f * highlight, .08f + .65f * highlight, edges);
            graphics.nextStratum();
        }
        var font = Minecraft.getInstance().font;
        var text = GlassStyle.label(label(key));
        float scale = Math.min(1, (w - 8f) / Math.max(1, font.width(text)));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + w / 2f, y + h / 2f);
        graphics.pose().scale(scale);
        graphics.text(font, text, -font.width(text) / 2, -font.lineHeight / 2,
                isPressed(key) ? GlassStyle.STATUS : GlassStyle.TEXT, !glass);
        graphics.pose().popMatrix();
    }

    private static boolean gameplayInput(Minecraft client) {
        return client.player != null && client.level != null && client.gui.screen() == null
                && client.gui.overlay() == null && !client.isPaused() && client.isWindowActive();
    }

    private static KeyMapping binding(Minecraft client, Key key) {
        return switch (key) {
            case FORWARD -> client.options.keyUp;
            case LEFT -> client.options.keyLeft;
            case BACKWARD -> client.options.keyDown;
            case RIGHT -> client.options.keyRight;
            case ATTACK -> client.options.keyAttack;
            case USE -> client.options.keyUse;
            case JUMP -> client.options.keyJump;
        };
    }

    private static boolean value(Properties values, String key) {
        return Boolean.parseBoolean(values.getProperty(key, "true"));
    }

    private void save() {
        Properties values = new Properties();
        values.setProperty("enabled", Boolean.toString(enabled));
        values.setProperty("glass", Boolean.toString(glass));
        values.setProperty("edges", Boolean.toString(edges));
        values.setProperty("mouseButtons", Boolean.toString(mouseButtons));
        values.setProperty("spaceBar", Boolean.toString(spaceBar));
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "voicechat-keystrokes-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) { values.store(writer, "Keystrokes HUD preferences"); }
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { VoicechatClient.LOG.warn("Could not save keystrokes HUD preferences", e); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }
}
