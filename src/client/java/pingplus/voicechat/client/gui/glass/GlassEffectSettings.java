package pingplus.voicechat.client.gui.glass;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Global glass blur and shadow settings; step 4 preserves the original effects. */
public final class GlassEffectSettings {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-glass-effects.properties");
    public static final int STEPS = 8, DEFAULT_STEP = 4;
    private static final Properties VALUES = load();
    private static int blur = readStep("blur"), shadow = readStep("shadow");
    public static int blurStep() { return blur; }
    public static int shadowStep() { return shadow; }
    public static float shadowScale() { return shadow * .25f; }
    public static void setBlur(int value) {
        int next = Math.clamp(value, 0, STEPS);
        if (blur != next) { blur = next; save(); }
    }
    public static void setShadow(int value) {
        int next = Math.clamp(value, 0, STEPS);
        if (shadow != next) { shadow = next; save(); }
    }
    private static int readStep(String key) {
        try { return Math.clamp(Integer.parseInt(VALUES.getProperty(key, "4")), 0, STEPS); }
        catch (NumberFormatException e) { return DEFAULT_STEP; }
    }

    private static void save() {
        Properties properties = new Properties();
        properties.setProperty("blur", Integer.toString(blur));
        properties.setProperty("shadow", Integer.toString(shadow));
        Path temp = null;
        try {
            Files.createDirectories(FILE.getParent());
            temp = Files.createTempFile(FILE.getParent(), "voicechat-glass-effects-", ".tmp");
            try (var writer = Files.newBufferedWriter(temp)) {
                properties.store(writer, "Glass effect steps (0 = off, 4 = default, 8 = 200%)");
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
