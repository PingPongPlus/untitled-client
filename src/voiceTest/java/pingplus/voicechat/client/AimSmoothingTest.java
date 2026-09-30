package pingplus.voicechat.client;

public final class AimSmoothingTest {
    public static void main(String[] args) {
        float seam = AimSmoothing.step(179, -179, 180, 1.0/60);
        check(seam > 179 && seam <= 181, "short path across yaw seam");
        check(AimSmoothing.step(0, 90, 180, .05) <= 9.001f, "turn speed limit");
        check(AimSmoothing.step(0, 90, 180, 8) <= 9.001f, "no snap after stalled frame");
        check(AimSmoothing.step(42, 90, 180, 0) == 42, "zero time holds rotation");
        float slow = simulate(30), fast = simulate(144);
        check(Math.abs(slow-fast) < .01, "frame-rate independent easing");
        check(slow > 9.9f && slow <= 10, "converges without overshoot");
        check(AimSmoothing.step(42, Float.NaN, 180, .02) == 42, "invalid target cannot poison camera");
        System.out.println("PASS: smooth aim wraparound, speed cap, stall handling, frame rates and convergence");
    }
    private static float simulate(int frames) {
        float angle=0;
        for(int i=0;i<frames;i++) angle=AimSmoothing.step(angle,10,180,1.0/frames);
        return angle;
    }
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
