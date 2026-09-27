package pingplus.voicechat.client;

import net.minecraft.client.Minecraft;

/** Static zoom: instantly applies the FOV divisor while the zoom key is held. */
public final class ZoomFeature {
    private static boolean zoomed;

    public static void tick(Minecraft client, boolean down) {
        zoomed = PlayerSettings.zoom && down && client.player != null && client.gui.screen() == null;
    }

    public static float progress() { return zoomed ? 1 : 0; }

    public static double fovFactor() { return 1 + progress() * Math.max(0.1f, PlayerSettings.zoomStrength); }

    public static double sensitivityFactor() { return 1 / fovFactor(); }

    private ZoomFeature() {}
}
