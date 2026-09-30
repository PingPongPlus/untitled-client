package pingplus.voicechat.client.hud;

import net.minecraft.client.Minecraft;
import pingplus.voicechat.client.gui.MetricsHud;

/** Client-thread sampling shared by both HUD widgets; rendering never changes gameplay. */
public final class HudMetrics {
    private static final CpsCounter CLICKS = new CpsCounter();
    private static final MovementSpeed SPEED = new MovementSpeed();
    private static Object player, level;

    private HudMetrics() {}

    public static void tick(Minecraft client) {
        if (!prepare(client)) return;
        if (client.gui.screen() != null || client.gui.overlay() != null || !MetricsHud.CPS.isEnabled()) resetCps();
        if (MetricsHud.SPEED.isEnabled()) SPEED.sample(client.player.getX(), client.player.getZ(), System.nanoTime());
        else resetSpeed();
    }

    public static void mousePressed(Minecraft client, int button) {
        if (!prepare(client) || client.gui.screen() != null || client.gui.overlay() != null || !MetricsHud.CPS.isEnabled()) return;
        CLICKS.press(button, System.nanoTime());
    }

    private static boolean prepare(Minecraft client) {
        if (client.player == null || client.level == null || client.isPaused()) { reset(); return false; }
        if (player != client.player || level != client.level) {
            reset();
            player = client.player;
            level = client.level;
        }
        return true;
    }

    private static boolean clicksReady() {
        var client = Minecraft.getInstance();
        if (!prepare(client) || client.gui.screen() != null || client.gui.overlay() != null || !MetricsHud.CPS.isEnabled()) {
            resetCps();
            return false;
        }
        return true;
    }
    public static int leftCps() { return clicksReady() ? CLICKS.left(System.nanoTime()) : 0; }
    public static int rightCps() { return clicksReady() ? CLICKS.right(System.nanoTime()) : 0; }
    public static double speed() {
        // Pause state can change between ticks; do not leave the last moving value on screen.
        return prepare(Minecraft.getInstance()) && MetricsHud.SPEED.isEnabled() ? SPEED.blocksPerSecond() : 0;
    }
    public static void resetCps() { CLICKS.reset(); }
    public static void resetSpeed() { SPEED.reset(); }
    public static void reset() { resetCps(); resetSpeed(); player = null; level = null; }
}
