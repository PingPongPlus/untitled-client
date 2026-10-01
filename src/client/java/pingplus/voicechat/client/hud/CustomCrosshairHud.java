package pingplus.voicechat.client.hud;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.slf4j.LoggerFactory;

/** A fixed-center crosshair; the vanilla HUD controls when it is visible. */
public final class CustomCrosshairHud {
    public enum Shape {
        CROSS("Cross"), DOT("Dot"), CIRCLE("Circle"), T("T");
        public final String label;
        Shape(String label) { this.label = label; }
    }

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-crosshair.properties");
    public static final CustomCrosshairHud INSTANCE = new CustomCrosshairHud();
    private boolean enabled, centerDot, cooldownRing;
    private boolean outline = true;
    private Shape shape = Shape.CROSS;
    private int size = 6, gap = 3, thickness = 1, color = 0xFFFFFF, opacity = 100;
    private Geometry geometry;

    private CustomCrosshairHud() {
        Properties values = new Properties();
        if (Files.exists(FILE)) {
            try (var reader = Files.newBufferedReader(FILE)) { values.load(reader); }
            catch (IOException | IllegalArgumentException e) {
                LoggerFactory.getLogger(CustomCrosshairHud.class).warn("Could not load crosshair preferences; using defaults", e);
                values.clear();
            }
        }
        enabled = bool(values, "enabled", false);
        centerDot = bool(values, "centerDot", false);
        cooldownRing = bool(values, "cooldownRing", false);
        outline = bool(values, "outline", true);
        try { shape = Shape.valueOf(values.getProperty("shape", "CROSS")); }
        catch (IllegalArgumentException ignored) { }
        size = integer(values, "size", 6, 1, 24);
        gap = integer(values, "gap", 3, 0, 12);
        thickness = integer(values, "thickness", 1, 1, 6);
        opacity = integer(values, "opacity", 100, 10, 100);
        String hex = values.getProperty("color", "FFFFFF");
        if (hex.matches("[0-9a-fA-F]{6}")) color = Integer.parseInt(hex, 16);
    }

    public boolean isEnabled() { return enabled; }
    public boolean isOutline() { return outline; }
    public boolean isCenterDot() { return centerDot; }
    public boolean isCooldownRing() { return cooldownRing; }
    public Shape shape() { return shape; }
    public int size() { return size; }
    public int gap() { return gap; }
    public int thickness() { return thickness; }
    public int color() { return color; }
    public int opacity() { return opacity; }
    public int red() { return (color >>> 16) & 255; }
    public int green() { return (color >>> 8) & 255; }
    public int blue() { return color & 255; }
    public void toggle() { enabled = !enabled; save(); }
    public void toggleOutline() { outline = !outline; save(); }
    public void toggleCenterDot() { centerDot = !centerDot; changed(); }
    public void toggleCooldownRing() { cooldownRing = !cooldownRing; save(); }
    public void cycleShape() { setShape(Shape.values()[(shape.ordinal() + 1) % Shape.values().length]); }
    public void setShape(Shape value) { if (shape != value && value != null) { shape = value; changed(); } }
    public void setSize(int value) { int next = Math.clamp(value, 1, 24); if (size != next) { size = next; changed(); } }
    public void setGap(int value) { int next = Math.clamp(value, 0, 12); if (gap != next) { gap = next; changed(); } }
    public void setThickness(int value) { int next = Math.clamp(value, 1, 6); if (thickness != next) { thickness = next; changed(); } }
    public void setOpacity(int value) { int next = Math.clamp(value, 10, 100); if (opacity != next) { opacity = next; save(); } }
    public void setColor(int value) { int next = value & 0xFFFFFF; if (color != next) { color = next; save(); } }
    public void setRed(int value) { channel(value, 16); }
    public void setGreen(int value) { channel(value, 8); }
    public void setBlue(int value) { channel(value, 0); }

    /** Reset appearance without changing whether the feature is enabled. */
    public void resetAppearance() {
        shape = Shape.CROSS;
        size = 6; gap = 3; thickness = 1; color = 0xFFFFFF; opacity = 100;
        outline = true; centerDot = false; cooldownRing = false;
        changed();
    }

    /** Also used by the settings preview, even while the gameplay toggle is off. */
    public void render(GuiGraphicsExtractor graphics, int x, int y, float attackStrength) {
        if (geometry == null) geometry = buildGeometry();
        int alpha = Math.round(opacity * 2.55f);
        graphics.nextStratum();
        if (outline) draw(graphics, geometry.outline, x, y, alpha << 24);
        draw(graphics, geometry.shape, x, y, (alpha << 24) | color);
        if (cooldownRing) {
            float progress = Float.isFinite(attackStrength) ? Math.clamp(attackStrength, 0, 1) : 0;
            for (ArcSpan span : geometry.ring) {
                int tint = span.progress <= progress ? (alpha << 24) | color : ((alpha / 3) << 24) | 0xFFFFFF;
                graphics.fill(x + span.x, y + span.y, x + span.x + 1, y + span.y + 1, tint);
            }
        }
    }

    private Geometry buildGeometry() {
        int radius = shape == Shape.DOT ? (size + 1) / 2 : size + gap;
        int bounds = radius + thickness + 4, width = bounds * 2 + 1;
        boolean[][] pixels = new boolean[width][width];
        int low = -thickness / 2, high = low + thickness;
        for (int y = -bounds; y <= bounds; y++) for (int x = -bounds; x <= bounds; x++) {
            boolean marked = switch (shape) {
                case CROSS, T -> (y >= low && y < high && Math.abs(x) >= gap && Math.abs(x) < radius)
                        || (x >= low && x < high && Math.abs(y) >= gap && Math.abs(y) < radius
                            && (shape != Shape.T || y >= 0));
                case DOT -> x >= -size / 2 && x < -size / 2 + size && y >= -size / 2 && y < -size / 2 + size;
                case CIRCLE -> {
                    double distance = Math.hypot(x, y);
                    yield distance >= Math.max(0, radius - thickness + .5) && distance < radius + .5;
                }
            };
            if (centerDot && x >= low && x < high && y >= low && y < high) marked = true;
            pixels[y + bounds][x + bounds] = marked;
        }
        boolean[][] border = new boolean[width][width];
        for (int y = 1; y < width - 1; y++) for (int x = 1; x < width - 1; x++) {
            if (!pixels[y][x]) continue;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++)
                if (!pixels[y + dy][x + dx]) border[y + dy][x + dx] = true;
        }
        List<ArcSpan> ring = new ArrayList<>();
        int ringRadius = radius + 4;
        for (int y = -ringRadius; y <= ringRadius; y++) for (int x = -ringRadius; x <= ringRadius; x++) {
            double distance = Math.hypot(x, y);
            if (distance < ringRadius - .5 || distance >= ringRadius + .5) continue;
            double angle = Math.atan2(x, -y);
            if (angle < 0) angle += Math.PI * 2;
            ring.add(new ArcSpan(x, y, (float)(angle / (Math.PI * 2))));
        }
        return new Geometry(spans(pixels, bounds), spans(border, bounds), List.copyOf(ring));
    }

    private static List<Span> spans(boolean[][] pixels, int offset) {
        List<Span> result = new ArrayList<>();
        for (int y = 0; y < pixels.length; y++) {
            int x = 0;
            while (x < pixels.length) {
                if (!pixels[y][x]) { x++; continue; }
                int start = x++;
                while (x < pixels.length && pixels[y][x]) x++;
                result.add(new Span(start - offset, y - offset, x - start));
            }
        }
        return List.copyOf(result);
    }

    private static void draw(GuiGraphicsExtractor graphics, List<Span> spans, int x, int y, int color) {
        for (Span span : spans) graphics.fill(x + span.x, y + span.y, x + span.x + span.width, y + span.y + 1, color);
    }
    private record Span(int x, int y, int width) { }
    private record ArcSpan(int x, int y, float progress) { }
    private record Geometry(List<Span> shape, List<Span> outline, List<ArcSpan> ring) { }

    private void channel(int value, int shift) { setColor((color & ~(255 << shift)) | (Math.clamp(value, 0, 255) << shift)); }
    private void changed() { geometry = null; save(); }
    private static boolean bool(Properties values, String key, boolean fallback) {
        String value = values.getProperty(key);
        return "true".equalsIgnoreCase(value) || (!"false".equalsIgnoreCase(value) && fallback);
    }
    private static int integer(Properties values, String key, int fallback, int min, int max) {
        try { return Math.clamp(Integer.parseInt(values.getProperty(key, Integer.toString(fallback))), min, max); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private void save() {
        Properties values = new Properties();
        values.setProperty("enabled", Boolean.toString(enabled));
        values.setProperty("shape", shape.name());
        values.setProperty("size", Integer.toString(size));
        values.setProperty("gap", Integer.toString(gap));
        values.setProperty("thickness", Integer.toString(thickness));
        values.setProperty("color", String.format(Locale.ROOT, "%06X", color));
        values.setProperty("opacity", Integer.toString(opacity));
        values.setProperty("outline", Boolean.toString(outline));
        values.setProperty("centerDot", Boolean.toString(centerDot));
        values.setProperty("cooldownRing", Boolean.toString(cooldownRing));
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "voicechat-crosshair-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) { values.store(writer, "Custom crosshair preferences"); }
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LoggerFactory.getLogger(CustomCrosshairHud.class).warn("Could not save crosshair preferences", e);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }
}
