package pingplus.voicechat.client.gui;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pingplus.voicechat.client.gui.glass.GlassStyle;
import pingplus.voicechat.client.hud.HudMetrics;

/** CPS and speed share the existing glass styling and HUD editor. */
public final class MetricsHud {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-metrics-hud.properties");
    private static final Properties PREFERENCES = load();
    public static final MetricsHud CPS = new MetricsHud("cps", "CPS  00 L / 00 R");
    public static final MetricsHud SPEED = new MetricsHud("speed", "Speed  00.00 b/s");
    private final String key, minimumText;
    private boolean enabled, glass, edges;

    private MetricsHud(String key, String minimumText) {
        this.key = key;
        this.minimumText = minimumText;
        enabled = value(key);
        glass = value(key + "Glass");
        edges = value(key + "Edges");
    }

    public boolean isEnabled() { return enabled; }
    public boolean isGlass() { return glass; }
    public boolean isEdges() { return edges; }
    public void toggle() {
        enabled = !enabled;
        if (this == CPS) HudMetrics.resetCps(); else HudMetrics.resetSpeed();
        save();
    }
    public void toggleGlass() { glass = !glass; save(); }
    public void toggleEdges() { edges = !edges; save(); }

    public String text() {
        return this == CPS ? "CPS  " + HudMetrics.leftCps() + " L / " + HudMetrics.rightCps() + " R"
                : String.format(Locale.ROOT, "Speed  %.2f b/s", HudMetrics.speed());
    }
    public int width() {
        var font = Minecraft.getInstance().font;
        return Math.max(font.width(minimumText), font.width(text())) + 16;
    }
    public int height() { return Minecraft.getInstance().font.lineHeight + 10; }
    public void render(GuiGraphicsExtractor graphics) {
        var client = Minecraft.getInstance();
        if (!enabled || client.player == null) return;
        if (glass) {
            graphics.nextStratum();
            GlassStyle.surface(graphics, 0, 0, width(), height(), .88f, .08f, edges);
            graphics.nextStratum();
        }
        graphics.text(client.font, text(), 8, 5, GlassStyle.TEXT, !glass);
    }

    private static boolean value(String key) { return Boolean.parseBoolean(PREFERENCES.getProperty(key, "true")); }
    private static Properties load() {
        Properties values = new Properties();
        try (var reader = Files.newBufferedReader(FILE)) { values.load(reader); }
        catch (IOException | IllegalArgumentException ignored) { }
        return values;
    }
    private void save() {
        PREFERENCES.setProperty(key, Boolean.toString(enabled));
        PREFERENCES.setProperty(key + "Glass", Boolean.toString(glass));
        PREFERENCES.setProperty(key + "Edges", Boolean.toString(edges));
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "voicechat-metrics-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) { PREFERENCES.store(writer, "CPS and speed HUD preferences"); }
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { pingplus.voicechat.client.VoicechatClient.LOG.warn("Could not save metrics HUD preferences", e); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }
}
