package pingplus.voicechat.client.gui.glass;

/** Time-based material accumulation, independent of frame rate and Minecraft ticks. */
public final class WeatherGlassClimate {
    public record Environment(boolean inWorld, boolean exposed, boolean cold, float rain) { }
    public record Amounts(float rain, float frost) { }
    private double wet, frozen;
    public static Amounts target(WeatherGlassSettings.Values settings, Environment environment) {
        if (!settings.enabled() || settings.intensity() == 0) return new Amounts(0, 0);
        double intensity = settings.intensity() / 100.0;
        boolean force = settings.alwaysActive() && settings.mode() != WeatherGlassSettings.Mode.AUTOMATIC;
        if (!force && (!environment.inWorld() || !environment.exposed())) return new Amounts(0, 0);
        float rain = Float.isFinite(environment.rain()) ? Math.clamp(environment.rain(), 0, 1) : 0;
        boolean cold = environment.cold();
        return switch (settings.mode()) {
            case AUTOMATIC -> cold ? new Amounts(0, (float)(intensity * (0.65 + rain * .35)))
                : new Amounts((float)(intensity * rain), 0);
            case RAIN -> new Amounts((float)(intensity * (force ? 1 : cold ? 0 : rain)), 0);
            case FROST -> new Amounts(0, (float)(intensity * (force ? 1 : cold ? .65 + rain * .35 : 0)));
        };
    }
    public Amounts advance(Amounts target, double seconds) {
        double dt = Double.isFinite(seconds) ? Math.clamp(seconds, 0, 1) : 0;
        wet += (target.rain() - wet) * -Math.expm1(-dt / (target.rain() > wet ? 1.8 : 4.5));
        frozen += (target.frost() - frozen) * -Math.expm1(-dt / (target.frost() > frozen ? 3.8 : 5.5));
        return new Amounts((float)wet, (float)frozen);
    }
}
