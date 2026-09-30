package pingplus.voicechat.client.spotify;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.IOException;
import java.util.Properties;

/** Relative coordinates preserve placement across window resizing and GUI scale changes. */
public final class SpotifySettings {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-spotify-hud.properties");
    private static boolean enabled = true;
    private static boolean edges = true;
    private static boolean musicGlass;
    private static int musicIntensity = 35;
    private static double horizontal = 1, vertical = 0;
    static {
        try (var reader = Files.newBufferedReader(FILE)) {
            Properties p = new Properties(); p.load(reader);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            edges = Boolean.parseBoolean(p.getProperty("edges", "true"));
            musicGlass = Boolean.parseBoolean(p.getProperty("musicGlass", "false"));
            musicIntensity = Math.clamp(Integer.parseInt(p.getProperty("musicIntensity", "35")), 0, 100);
            horizontal = fraction(Double.parseDouble(p.getProperty("x", "1")));
            vertical = fraction(Double.parseDouble(p.getProperty("y", "0")));
        } catch (IOException | IllegalArgumentException ignored) {}
    }
    private static double fraction(double n) { return Double.isFinite(n) ? Math.clamp(n, 0, 1) : 0; }
    public static boolean enabled() { return enabled; }
    public static void toggle() { enabled = !enabled; save(); }
    public static boolean edges() { return edges; }
    public static boolean musicGlass() { return musicGlass; }
    public static void toggleMusicGlass() { musicGlass = !musicGlass; save(); }
    public static int musicIntensity() { return musicIntensity; }
    public static void setMusicIntensity(int value) { musicIntensity = Math.clamp(value, 0, 100); save(); }
    public static void toggleEdges() { edges = !edges; save(); }
    public static int x(int sw, int pw) { return 8 + (int)Math.round(horizontal * Math.max(0, sw - pw - 16)); }
    public static int y(int sh, int ph) { return 8 + (int)Math.round(vertical * Math.max(0, sh - ph - 36)); }
    public static void position(int x, int y, int sw, int sh, int pw, int ph) {
        horizontal = fraction((x - 8.0) / Math.max(1, sw - pw - 16));
        vertical = fraction((y - 8.0) / Math.max(1, sh - ph - 36));
    }
    public static void save() {
        Properties p = new Properties(); p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("edges", Boolean.toString(edges));
        p.setProperty("musicGlass", Boolean.toString(musicGlass));
        p.setProperty("musicIntensity", Integer.toString(musicIntensity));
        p.setProperty("x", Double.toString(horizontal)); p.setProperty("y", Double.toString(vertical));
        try {
            Files.createDirectories(FILE.getParent());
            try (var out = Files.newBufferedWriter(FILE)) { p.store(out, "Spotify HUD visibility and position"); }
        } catch (IOException e) { pingplus.voicechat.client.VoicechatClient.LOG.warn("Could not save Spotify HUD settings"); }
    }
    private SpotifySettings() {}
}
