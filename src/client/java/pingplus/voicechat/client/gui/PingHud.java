package pingplus.voicechat.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pingplus.voicechat.client.gui.glass.GlassStyle;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

/** Uses the server's tab-list latency; never sends extra ping requests. */
public final class PingHud {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("voicechat-info-hud.properties");
    public static final PingHud INSTANCE = new PingHud();
    private boolean enabled = true, glass = true, edges = true;
    private PingHud() {
        try (var reader = Files.newBufferedReader(FILE)) {
            Properties values = new Properties(); values.load(reader);
            enabled = Boolean.parseBoolean(values.getProperty("ping", "true"));
            glass = Boolean.parseBoolean(values.getProperty("pingGlass", "true"));
            edges = Boolean.parseBoolean(values.getProperty("pingEdges", "true"));
        } catch (IOException | IllegalArgumentException ignored) { }
    }

    public boolean isEnabled() { return enabled; }
    public void toggle() { enabled = !enabled; save(); }
    public boolean isGlass() { return glass; }
    public void toggleGlass() { glass = !glass; save(); }
    public boolean isEdges() { return edges; }
    public void toggleEdges() { edges = !edges; save(); }

    private void save() {
        Properties values = new Properties();
        values.setProperty("ping", Boolean.toString(enabled));
        values.setProperty("pingGlass", Boolean.toString(glass));
        values.setProperty("pingEdges", Boolean.toString(edges));
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "voicechat-ping-", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) { values.store(writer, "Ping HUD preferences"); }
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { pingplus.voicechat.client.VoicechatClient.LOG.warn("Could not save ping HUD preferences", e); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }

    public Integer latency() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null || client.isLocalServer()) return null;
        var info = client.getConnection().getPlayerInfo(client.player.getUUID());
        return info == null || info.getLatency() < 0 ? null : info.getLatency();
    }
    public String text() {
        if (Minecraft.getInstance().isLocalServer()) return "Ping  Local";
        Integer latency = latency();
        return "Ping  " + (latency == null ? "--" : latency) + " ms";
    }
    public int width() { return Minecraft.getInstance().font.width(text()) + 16; }
    public int height() { return Minecraft.getInstance().font.lineHeight + 10; }

    public void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || client.player == null) return;
        if (isGlass()) {
            graphics.nextStratum();
            GlassStyle.surface(graphics, 0, 0, width(), height(), .88f, .08f, isEdges());
            graphics.nextStratum();
        }
        Integer latency = latency();
        int color = latency == null ? GlassStyle.MUTED : latency < 100 ? GlassStyle.STATUS
                : latency < 200 ? 0xFFFFCC80 : 0xFFFF9B99;
        graphics.text(client.font, text(), 8, 5, color, !isGlass());
    }
}
