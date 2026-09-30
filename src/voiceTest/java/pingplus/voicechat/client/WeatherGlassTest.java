package pingplus.voicechat.client;

import pingplus.voicechat.client.gui.glass.WeatherGlassClimate;
import pingplus.voicechat.client.gui.glass.WeatherGlassSettings;
import static pingplus.voicechat.client.gui.glass.WeatherGlassSettings.Mode.*;

public final class WeatherGlassTest {
    public static void main(String[] args) {
        var outside = new WeatherGlassClimate.Environment(true, true, false, 1);
        var cold = new WeatherGlassClimate.Environment(true, true, true, 1);
        var roof = new WeatherGlassClimate.Environment(true, false, false, 1);
        var menu = new WeatherGlassClimate.Environment(false, false, false, 0);
        check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(false, AUTOMATIC, false, 80), outside).rain() == 0, "Off by default / disabled bypass");
        check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(true, AUTOMATIC, false, 80), outside).rain() == .8f, "Rain responds to local precipitation and intensity");
        check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(true, AUTOMATIC, false, 80), cold).frost() == .8f, "Cold precipitation selects frost");
        for (var mode : WeatherGlassSettings.Mode.values()) {
            check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(true, mode, false, 80), roof).rain() == 0, "Shelter blocks rain");
            check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(true, mode, false, 80), menu).frost() == 0, "No automatic frost in menus");
        }
        check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(true, RAIN, true, 60), menu).rain() == .6f, "Forced rain works without a world");
        check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(true, FROST, true, 60), roof).frost() == .6f, "Forced frost ignores shelter and temperature");
        check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(true, AUTOMATIC, true, 60), menu).rain() == 0, "Automatic ignores Always active");
        check(WeatherGlassClimate.target(new WeatherGlassSettings.Values(true, RAIN, false, 60), cold).rain() == 0, "Snow is not liquid rain");
        var wet = new WeatherGlassClimate.Amounts(1, 1);
        var thirty = new WeatherGlassClimate(); var fast = new WeatherGlassClimate();
        WeatherGlassClimate.Amounts slow = null, quick = null;
        for (int i = 0; i < 300; i++) slow = thirty.advance(wet, 1.0 / 30);
        for (int i = 0; i < 1440; i++) quick = fast.advance(wet, 1.0 / 144);
        check(Math.abs(slow.rain() - quick.rain()) < .00001 && Math.abs(slow.frost() - quick.frost()) < .00001, "Transitions agree at 30 and 144 fps");
        var drying = thirty.advance(new WeatherGlassClimate.Amounts(0, 0), .1);
        check(drying.rain() > 0 && drying.rain() < slow.rain() && drying.frost() < slow.frost(), "Drying and thawing are gradual");
        check(new WeatherGlassSettings.Values(true, RAIN, true, 500).intensity() == 100, "Intensity clamps safely");
        check(new WeatherGlassSettings.Values(true, null, true, -5).mode() == AUTOMATIC, "Missing mode defaults safely");
        System.out.println("PASS: weather selection, shelter, cold biome, Always active, intensity bounds and frame-rate-independent transitions");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
