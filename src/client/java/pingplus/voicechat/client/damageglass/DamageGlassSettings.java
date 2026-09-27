package pingplus.voicechat.client.damageglass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

public final class DamageGlassSettings {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-damage-glass.properties");
    private static final Properties VALUES = load();
    private static boolean enabled = read("enabled"), players = read("players"), mobs = read("mobs");
    private static int reflectivity = readReflectivity();
    private static boolean impactRipple = read("impactRipple");
    private static boolean deathWave = Boolean.parseBoolean(VALUES.getProperty("deathWave", "false"));
    public static boolean deathWave() { return deathWave; }
    public static void toggleDeathWave() { deathWave = !deathWave; save(); }
    private static GlassPreset preset = GlassPreset.parse(VALUES.getProperty("preset"));
    public static boolean impactRipple() { return impactRipple; }
    public static void toggleImpactRipple() { impactRipple = !impactRipple; save(); }
    public static GlassPreset preset() { return preset; }
    public static void setPreset(GlassPreset value) {
        preset = java.util.Objects.requireNonNull(value);
        reflectivity = value.reflectivity;
        save();
    }
    public static void cyclePreset() { setPreset(GlassPreset.values()[(preset.ordinal() + 1) % GlassPreset.values().length]); }
    private static boolean alwaysOn = Boolean.parseBoolean(VALUES.getProperty("alwaysOn", "false"));
    public static boolean alwaysOn() { return alwaysOn; }
    public static void toggleAlwaysOn() { alwaysOn = !alwaysOn; save(); }
    public static int reflectivity() { return reflectivity; }
    public static void setReflectivity(int value) {
        int next = Math.clamp(value, 0, 100);
        if (next != reflectivity) { reflectivity = next; save(); }
    }
    private static int readReflectivity() {
        try { return Math.clamp(Integer.parseInt(VALUES.getProperty("reflectivity", "0")), 0, 100); }
        catch (NumberFormatException e) { return 0; }
    }
    public static boolean enabled() { return enabled; }
    public static boolean players() { return players; }
    public static boolean mobs() { return mobs; }
    public static void toggleEnabled() { enabled = !enabled; save(); }
    public static void togglePlayers() { players = !players; save(); }
    public static void toggleMobs() { mobs = !mobs; save(); }
    private static boolean read(String key) { return Boolean.parseBoolean(VALUES.getProperty(key, "true")); }
    private static Properties load() {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (var reader = Files.newBufferedReader(FILE)) { p.load(reader); }
            catch (IOException | IllegalArgumentException e) {
                LoggerFactory.getLogger(DamageGlassSettings.class).warn("Could not load damage glass settings", e);
                p.clear();
            }
        }
        return p;
    }
    private static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("players", Boolean.toString(players));
        p.setProperty("mobs", Boolean.toString(mobs));
        p.setProperty("reflectivity", Integer.toString(reflectivity));
        p.setProperty("alwaysOn", Boolean.toString(alwaysOn));
        p.setProperty("impactRipple", Boolean.toString(impactRipple));
        p.setProperty("deathWave", Boolean.toString(deathWave));
        p.setProperty("preset", preset.name());
        Path temp = null;
        try {
            Files.createDirectories(FILE.getParent());
            temp = Files.createTempFile(FILE.getParent(), "damage-glass-", ".tmp");
            try (var writer = Files.newBufferedWriter(temp)) { p.store(writer, "Liquid glass damage effect"); }
            Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LoggerFactory.getLogger(DamageGlassSettings.class).warn("Could not save damage glass settings", e);
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }
    private DamageGlassSettings() {}
}
