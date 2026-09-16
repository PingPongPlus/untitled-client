package pingplus.voicechat.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Properties;

/**
 * Shared settings for the screen and HUD. Changes update memory immediately;
 * callers save after a completed interaction instead of writing every frame.
 */
public final class ClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("pingplus");
    private static final double DEFAULT_OPACITY = 0.75;
    private static final Accent[] ACCENTS = {
            new Accent("Violet", 0xFF9C8CFF),
            new Accent("Mint", 0xFF56DDB5),
            new Accent("Sky", 0xFF65BBFF),
            new Accent("Amber", 0xFFFFB86B)
    };

    private final Path path;
    private final EnumSet<ClientModule> enabledModules = EnumSet.of(ClientModule.FPS);
    private int accentIndex;
    private double hudOpacity = DEFAULT_OPACITY;
    private boolean animationsEnabled = true;
    private boolean dirty;
    private boolean saveFailed;

    private record Accent(String name, int color) {}

    public ClientConfig() {
        this(FabricLoader.getInstance().getConfigDir().resolve("pingplus-client.properties"));
    }

    /** Accepting a path also allows persistence checks without launching Minecraft. */
    public ClientConfig(Path path) {
        this.path = path;
        load();
    }

    public int accentColor() {
        return ACCENTS[accentIndex].color();
    }

    public String accentName() {
        return ACCENTS[accentIndex].name();
    }

    public double hudOpacity() {
        return hudOpacity;
    }

    public boolean animationsEnabled() {
        return animationsEnabled;
    }

    public boolean isEnabled(ClientModule module) {
        return enabledModules.contains(module);
    }

    public int enabledCount() {
        return enabledModules.size();
    }

    public boolean hasSaveFailed() {
        return saveFailed;
    }

    public void toggleModule(ClientModule module) {
        if (enabledModules.contains(module)) {
            enabledModules.remove(module);
        } else {
            enabledModules.add(module);
        }
        dirty = true;
    }

    public void cycleAccent() {
        accentIndex = (accentIndex + 1) % ACCENTS.length;
        dirty = true;
    }

    public void toggleAnimations() {
        animationsEnabled = !animationsEnabled;
        dirty = true;
    }

    public void setHudOpacity(double opacity) {
        hudOpacity = sanitizeOpacity(opacity);
        dirty = true;
    }

    private static double sanitizeOpacity(double opacity) {
        return Double.isFinite(opacity) ? Math.clamp(opacity, 0.0, 1.0) : DEFAULT_OPACITY;
    }

    private void load() {
        if (!Files.exists(path)) {
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
            loadModules(properties);
            accentIndex = Math.floorMod(Integer.parseInt(properties.getProperty("accent", "0")), ACCENTS.length);
            hudOpacity = sanitizeOpacity(Double.parseDouble(properties.getProperty("opacity", "0.75")));
            animationsEnabled = Boolean.parseBoolean(properties.getProperty("animations", "true"));
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Could not read ClickGUI settings; using available defaults", exception);
        }
    }

    private void loadModules(Properties properties) {
        for (ClientModule module : ClientModule.values()) {
            String defaultValue = Boolean.toString(isEnabled(module));
            boolean enabled = Boolean.parseBoolean(properties.getProperty(module.name(), defaultValue));
            if (enabled) {
                enabledModules.add(module);
            } else {
                enabledModules.remove(module);
            }
        }
    }

    private Properties toProperties() {
        Properties properties = new Properties();
        for (ClientModule module : ClientModule.values()) {
            // Enum names are persistent keys. Renaming one requires a config migration.
            properties.setProperty(module.name(), Boolean.toString(isEnabled(module)));
        }
        properties.setProperty("accent", Integer.toString(accentIndex));
        properties.setProperty("opacity", Double.toString(hudOpacity));
        properties.setProperty("animations", Boolean.toString(animationsEnabled));
        return properties;
    }

    public void save() {
        if (!dirty) {
            return;
        }

        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            // Finish writing before replacing the previous settings file.
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                toProperties().store(writer, "PingPlus client settings");
            }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
            saveFailed = false;
        } catch (IOException exception) {
            // Keep dirty true so closing the screen can retry a failed save.
            saveFailed = true;
            LOGGER.warn("Could not save ClickGUI settings", exception);
        }
    }
}
