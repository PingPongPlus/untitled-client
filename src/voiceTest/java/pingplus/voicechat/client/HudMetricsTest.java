package pingplus.voicechat.client;

import pingplus.voicechat.client.hud.metrics.CpsCounter;
import pingplus.voicechat.client.hud.metrics.MovementSpeed;

public final class HudMetricsTest {
    public static void main(String[] args) {
        CpsCounter cps = new CpsCounter();
        cps.press(0, 0);
        cps.press(0, 100_000_000L);
        cps.press(1, 200_000_000L);
        cps.press(2, 200_000_000L);
        check(cps.left(999_999_999L) == 2 && cps.right(999_999_999L) == 1, "independent buttons; middle ignored");
        check(cps.left(1_000_000_000L) == 1, "exact one-second expiry");
        check(cps.left(1_100_000_000L) == 0 && cps.right(1_100_000_000L) == 1, "staggered expiry");
        check(cps.right(1_200_000_000L) == 0, "right expiry");
        cps.press(0, 2_000_000_000L);
        cps.press(1, 2_000_000_000L);
        cps.reset();
        check(cps.left(2_000_000_000L) == 0 && cps.right(2_000_000_000L) == 0, "world/reset clears both buttons");
        // nanoTime subtraction also works when the clock crosses Long.MAX_VALUE.
        cps.press(0, Long.MAX_VALUE - 500_000_000L);
        check(cps.left(Long.MIN_VALUE + 499_999_999L) == 0, "monotonic clock wraparound");

        MovementSpeed speed = new MovementSpeed();
        speed.sample(0, 0, 0);
        near(speed.blocksPerSecond(), 0, "initial sample");
        for (int i = 1; i <= 10; i++) speed.sample(i * .2, 0, i * 50_000_000L);
        near(speed.blocksPerSecond(), 4, "constant horizontal movement");
        speed.sample(2, 0, 500_000_000L);
        near(speed.blocksPerSecond(), 4, "duplicate tick cannot divide by zero");
        for (int i = 11; i <= 16; i++) speed.sample(2, 0, i * 50_000_000L);
        near(speed.blocksPerSecond(), 0, "stationary/vertical-only movement settles to zero");
        speed.reset();
        speed.sample(0, 0, 0);
        speed.sample(.15, .2, 50_000_000L);
        near(speed.blocksPerSecond(), 5, "diagonal uses horizontal distance");
        speed.sample(100, 100, 100_000_000L);
        near(speed.blocksPerSecond(), 0, "teleport is excluded");
        speed.sample(100.2, 100, 150_000_000L);
        near(speed.blocksPerSecond(), 4, "movement resumes after correction");
        speed.sample(101, 100, 1_000_000_000L);
        near(speed.blocksPerSecond(), 0, "long pause clears old samples");
        speed.sample(Double.NaN, 0, 1_050_000_000L);
        near(speed.blocksPerSecond(), 0, "invalid position resets");
        speed.sample(0, 0, 1_100_000_000L);
        speed.sample(0, .25, 1_150_000_000L);
        near(speed.blocksPerSecond(), 5, "fresh anchor after invalid position");
        speed.reset();
        near(speed.blocksPerSecond(), 0, "disconnect/reset");
        System.out.println("PASS: CPS channels/expiry/reset and speed movement/stopping/corrections/pauses");
    }
    private static void near(double actual, double expected, String label) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) < .000001, label + ": " + actual);
    }
    private static void check(boolean passed, String label) { if (!passed) throw new AssertionError(label); }
}
