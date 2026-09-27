package pingplus.voicechat.client.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import pingplus.voicechat.client.PlayerSettings;
import pingplus.voicechat.client.gui.glass.GlassStyle;

/** Screen-facing glass gauges anchored to the right side of visible player bodies. */
public final class PlayerHealthBarRenderer {
    private static final Map<UUID, Float> FILLS = new HashMap<>();
    private static Object level;
    private static long lastFrame;

    public static void render(GuiGraphicsExtractor g) {
        Minecraft client = Minecraft.getInstance();
        if (level != client.level) {
            FILLS.clear();
            level = client.level;
            lastFrame = 0;
        }
        if (!PlayerSettings.playerHealthBar || client.level == null || client.player == null
                || client.gui.hud.isHidden()) {
            FILLS.clear();
            lastFrame = 0;
            return;
        }
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float step = 1 - (float) Math.exp(-14 * dt);
        var camera = client.gameRenderer.mainCamera();
        Vec3 forward = new Vec3(camera.forwardVector());
        var seen = new HashSet<UUID>();
        for (var player : client.level.players()) {
            boolean self = player == client.player;
            if (!player.isAlive() || player.isRemoved() || player.isSpectator()
                    || player.isInvisibleTo(client.player)
                    || (self && client.options.getCameraType().isFirstPerson())) continue;
            double distance = player.position().distanceTo(camera.position());
            if (distance > 48 || (!self && !client.player.hasLineOfSight(player))) continue;

            float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(
                    !client.level.tickRateManager().isEntityFrozen(player));
            Vec3 position = player.getPosition(partialTick);
            var box = player.getBoundingBox().move(position.subtract(player.position()));
            double left = Double.POSITIVE_INFINITY, right = Double.NEGATIVE_INFINITY;
            double top = Double.POSITIVE_INFINITY, bottom = Double.NEGATIVE_INFINITY;
            boolean clipped = false;
            // Project all eight body corners so the bar stays beside the silhouette as the camera turns.
            for (int corner = 0; corner < 8; corner++) {
                Vec3 point = new Vec3((corner & 1) == 0 ? box.minX : box.maxX,
                        (corner & 2) == 0 ? box.minY : box.maxY,
                        (corner & 4) == 0 ? box.minZ : box.maxZ);
                if (point.subtract(camera.position()).dot(forward) <= .05) { clipped = true; break; }
                Vec3 projected = client.gameRenderer.projectPointToScreen(point);
                if (!Double.isFinite(projected.x) || !Double.isFinite(projected.y)
                        || projected.z < -1 || projected.z > 1) { clipped = true; break; }
                double x = (projected.x * .5 + .5) * g.guiWidth();
                double y = (.5 - projected.y * .5) * g.guiHeight();
                left = Math.min(left, x); right = Math.max(right, x);
                top = Math.min(top, y); bottom = Math.max(bottom, y);
            }
            if (clipped || right < 0 || left > g.guiWidth() || bottom < 0 || top > g.guiHeight()) continue;
            int height = (int) Math.clamp(bottom - top, 18, 110);
            int width = (int) Math.clamp(height * .1, 6, 10);
            int x = (int) Math.ceil(right) + 5;
            int y = (int) Math.round((top + bottom - height) * .5);
            if (x + width > g.guiWidth() || y < 0 || y + height > g.guiHeight()) continue;

            float target = player.getMaxHealth() > 0 ? Math.clamp(player.getHealth() / player.getMaxHealth(), 0, 1) : 0;
            float previous = FILLS.getOrDefault(player.getUUID(), target);
            float fill = previous + (target - previous) * step;
            FILLS.put(player.getUUID(), fill);
            seen.add(player.getUUID());
            float opacity = (float) Math.clamp((48 - distance) / 12, 0, 1);
            draw(g, x, y, width, height, fill, opacity);
        }
        FILLS.keySet().retainAll(seen);
    }

    private static void draw(GuiGraphicsExtractor g, int x, int y, int width, int height,
                             float fill, float opacity) {
        g.nextStratum();
        GlassStyle.surface(g, x, y, width, height, .88f * opacity, .08f, true);
        g.nextStratum();
        int innerHeight = height - 4;
        int filled = Math.clamp(Math.round(innerHeight * fill), 0, innerHeight);
        if (filled == 0) return;
        int fillY = y + height - 2 - filled;
        // A translucent tint retains the same refracted scene and glass rim as the other HUD widgets.
        GlassStyle.round(g, x + 2, fillY, width - 4, filled, 3,
                GlassStyle.alpha(color(fill), .8f * opacity));
        if (filled > 3) {
            GlassStyle.round(g, x + 2, fillY + 1, 1, filled - 2, 1,
                    GlassStyle.alpha(0xFFFFFFFF, .38f * opacity));
        }
    }

    /** Requested order: red at zero, yellow at one third, orange at two thirds, green at full. */
    public static int color(float health) {
        float ratio = Float.isFinite(health) ? Math.clamp(health, 0, 1) : 0;
        int[] stops = {0xFFFF5058, 0xFFFFD75A, 0xFFFFA044, 0xFF67E89B};
        float scaled = ratio * 3;
        int index = Math.min(2, (int) scaled);
        float blend = scaled - index;
        int a = stops[index], b = stops[index + 1];
        int red = Math.round(((a >> 16) & 255) * (1 - blend) + ((b >> 16) & 255) * blend);
        int green = Math.round(((a >> 8) & 255) * (1 - blend) + ((b >> 8) & 255) * blend);
        int blue = Math.round((a & 255) * (1 - blend) + (b & 255) * blend);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private PlayerHealthBarRenderer() {}
}
