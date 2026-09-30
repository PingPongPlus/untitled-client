package pingplus.voicechat.client.hud;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import pingplus.voicechat.client.gui.glass.GlassStyle;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

/** Transparent AIR artwork on an optional glass panel, positioned by the shared HUD editor. */
public final class LogoHud {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-logo-hud.properties");
    public static final LogoHud INSTANCE = new LogoHud();
    public static final int WIDTH = 111, HEIGHT = 58;
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("voicechat", "textures/gui/air_hud.png");
    private boolean enabled = true, glass = true, edges = true;

    private LogoHud() {
        try (var reader = Files.newBufferedReader(FILE)) {
            Properties p = new Properties();
            p.load(reader);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            glass = Boolean.parseBoolean(p.getProperty("glass", "true"));
            edges = Boolean.parseBoolean(p.getProperty("edges", "true"));
        } catch (IOException | IllegalArgumentException ignored) { }
    }
    public boolean isEnabled() { return enabled; }
    public void toggle() { enabled = !enabled; save(); }
    public boolean isGlass() { return glass; }
    public void toggleGlass() { glass = !glass; save(); }
    public boolean isEdges() { return edges; }
    public void toggleEdges() { edges = !edges; save(); }

    public void render(GuiGraphicsExtractor g) {
        if (!enabled) return;
        g.nextStratum();
        if (glass) {
            GlassStyle.surface(g, 0, 0, WIDTH, HEIGHT, .88f, .12f, edges);
            g.nextStratum();
        }
        // Render at half the source size, keeping the exact aspect ratio and alpha.
        g.pose().pushMatrix();
        g.pose().translate(6f, 6.25f);
        g.pose().scale(.5f);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0, 0, 198, 91, 198, 91);
        g.pose().popMatrix();
    }

    private void save() {
        Properties p = new Properties();
        p.setProperty("enabled", Boolean.toString(enabled));
        p.setProperty("glass", Boolean.toString(glass));
        p.setProperty("edges", Boolean.toString(edges));
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "voicechat-logo-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) { p.store(writer, "AIR logo HUD preferences"); }
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            pingplus.voicechat.client.VoicechatClient.LOG.warn("Could not save logo HUD settings", e);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }
}
