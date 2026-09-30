package pingplus.voicechat.client.gui.glass;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Saved global glass effects; custom tint is opt-in. */
public final class GlassEffectSettings {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-glass-effects.properties");
    public static final int STEPS = 8, DEFAULT_STEP = 4;
    private static final Properties VALUES = load();
    private static int blur = readStep("blur"), shadow = readStep("shadow");
    private static boolean customTint = Boolean.parseBoolean(VALUES.getProperty("customTint", "false"));
    private static int tintColor = readColor(), tintIntensity = readInt("tintIntensity", 35, 100);
    public static int blurStep() { return blur; }
    public static int shadowStep() { return shadow; }
    public static float shadowScale() { return shadow * .25f; }
    public static boolean customTint() { return customTint; }
    public static int tintColor() { return tintColor; }
    public static int tintIntensity() { return tintIntensity; }
    public static int tintRed() { return (tintColor >>> 16) & 255; }
    public static int tintGreen() { return (tintColor >>> 8) & 255; }
    public static int tintBlue() { return tintColor & 255; }
    public static void toggleCustomTint() { customTint = !customTint; save(); }
    public static void setTintColor(int color) {
        int next = color & 0xFFFFFF;
        if (tintColor != next) { tintColor = next; save(); }
    }
    public static void setTintRed(int value) { setChannel(value, 16); }
    public static void setTintGreen(int value) { setChannel(value, 8); }
    public static void setTintBlue(int value) { setChannel(value, 0); }
    private static void setChannel(int value, int shift) {
        setTintColor((tintColor & ~(255 << shift)) | (Math.clamp(value, 0, 255) << shift));
    }
    public static void setTintIntensity(int value) {
        int next = Math.clamp(value, 0, 100);
        if (tintIntensity != next) { tintIntensity = next; save(); }
    }
    public static void setBlur(int value) {
        int next = Math.clamp(value, 0, STEPS);
        if (blur != next) { blur = next; save(); }
    }
    public static void setShadow(int value) {
        int next = Math.clamp(value, 0, STEPS);
        if (shadow != next) { shadow = next; save(); }
    }
    private static int readStep(String key) {
        return readInt(key, DEFAULT_STEP, STEPS);
    }
    private static int readInt(String key, int fallback, int max) {
        try { return Math.clamp(Integer.parseInt(VALUES.getProperty(key, Integer.toString(fallback))), 0, max); }
        catch (NumberFormatException e) { return fallback; }
    }
    private static int readColor() {
        String hex = VALUES.getProperty("tintColor", "7DAFFF");
        return hex.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(hex, 16) : 0x7DAFFF;
    }

    private static void save() {
        Properties properties = new Properties();
        properties.setProperty("blur", Integer.toString(blur));
        properties.setProperty("shadow", Integer.toString(shadow));
        properties.setProperty("customTint", Boolean.toString(customTint));
        properties.setProperty("tintColor", String.format(java.util.Locale.ROOT, "%06X", tintColor));
        properties.setProperty("tintIntensity", Integer.toString(tintIntensity));
        Path temp = null;
        try {
            Files.createDirectories(FILE.getParent());
            temp = Files.createTempFile(FILE.getParent(), "voicechat-glass-effects-", ".tmp");
            try (var writer = Files.newBufferedWriter(temp)) {
                properties.store(writer, "Glass effects: blur/shadow steps 0-8, tint RGB hex, tint intensity 0-100");
            }
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LoggerFactory.getLogger(GlassEffectSettings.class).warn("Could not save glass effect preferences", e);
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }

    private static Properties load() {
        Properties properties = new Properties();
        if (!Files.exists(FILE)) return properties;
        try (var reader = Files.newBufferedReader(FILE)) { properties.load(reader); }
        catch (IOException | IllegalArgumentException e) {
            LoggerFactory.getLogger(GlassEffectSettings.class).warn("Could not load glass effects; using defaults", e);
            properties.clear();
        }
        return properties;
    }
    private GlassEffectSettings() { }
}
