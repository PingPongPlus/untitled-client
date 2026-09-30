package pingplus.voicechat.client;

/** Time-based angular easing with a speed limit and no overshoot at the +/-180 seam. */
public final class AimSmoothing {
    public static float step(float current, float target, float degreesPerSecond, double seconds) {
        if (!Float.isFinite(current) || !Float.isFinite(target) || !Float.isFinite(degreesPerSecond)
                || !Double.isFinite(seconds)) return Float.isFinite(current) ? current : 0;
        double dt = Math.clamp(seconds, 0, .05);
        float delta = ((target - current) % 360 + 540) % 360 - 180;
        double eased = delta * (1 - Math.exp(-10 * dt));
        double limit = Math.max(0, degreesPerSecond) * dt;
        return current + (float)Math.clamp(eased, -limit, limit);
    }
    private AimSmoothing() {}
}
