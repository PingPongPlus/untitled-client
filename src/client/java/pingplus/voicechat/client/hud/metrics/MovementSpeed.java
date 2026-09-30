package pingplus.voicechat.client.hud.metrics;

import java.util.ArrayDeque;

/** Horizontal distance per second, averaged over a quarter second to steady the readout. */
public final class MovementSpeed {
    private static final long WINDOW = 250_000_000L, MAX_GAP = 500_000_000L;
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private boolean initialized;
    private long previousTime;
    private double previousX, previousZ, distance;
    private long duration;

    public void sample(double x, double z, long now) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) { reset(); return; }
        if (!initialized) { anchor(x, z, now); return; }
        long elapsed = now - previousTime;
        if (elapsed <= 0) return;
        double moved = Math.hypot(x - previousX, z - previousZ);
        // A long pause or large position correction must not look like movement speed.
        if (elapsed > MAX_GAP || moved > 16) {
            reset();
            anchor(x, z, now);
            return;
        }
        samples.addLast(new Sample(now, elapsed, moved));
        duration += elapsed;
        distance += moved;
        while (!samples.isEmpty() && now - samples.peekFirst().time >= WINDOW) {
            Sample expired = samples.removeFirst();
            duration -= expired.duration;
            distance -= expired.distance;
        }
        anchor(x, z, now);
    }

    public double blocksPerSecond() {
        return duration == 0 ? 0 : Math.max(0, distance) * 1_000_000_000.0 / duration;
    }

    public void reset() {
        initialized = false;
        samples.clear();
        distance = 0;
        duration = 0;
    }

    private void anchor(double x, double z, long now) {
        initialized = true;
        previousX = x;
        previousZ = z;
        previousTime = now;
    }

    private record Sample(long time, long duration, double distance) {}
}
