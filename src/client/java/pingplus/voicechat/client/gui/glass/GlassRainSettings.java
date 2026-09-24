package pingplus.voicechat.client.gui.glass;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Raindrop visual preference, independent of gameplay settings. */
public final class GlassRainSettings {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-glass-rain.properties");
    private static boolean enabled = load();

    public static boolean isEnabled() { return enabled; }

    public static boolean setEnabled(boolean value) {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(value));
        Path temp = null;
        try {
            Files.createDirectories(FILE.getParent());
            temp = Files.createTempFile(FILE.getParent(), "voicechat-glass-rain-", ".tmp");
            try (var writer = Files.newBufferedWriter(temp)) {
                properties.store(writer, "Glass raindrop effect");
            }
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            enabled = value;
            return true;
        } catch (IOException e) {
            LoggerFactory.getLogger(GlassRainSettings.class).warn("Could not save glass preference", e);
            return false;
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }

    private static boolean load() {
        if (!Files.exists(FILE)) return true;
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            return !"false".equalsIgnoreCase(properties.getProperty("enabled"));
        } catch (IOException | IllegalArgumentException e) {
            LoggerFactory.getLogger(GlassRainSettings.class).warn("Could not load glass preference; using default", e);
            return true;
        }
    }
    private GlassRainSettings() { }
}
