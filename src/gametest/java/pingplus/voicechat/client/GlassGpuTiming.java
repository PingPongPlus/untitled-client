package pingplus.voicechat.client;

/** Render-thread-only measurements for the optional client GPU test. */
public final class GlassGpuTiming {
    public static boolean enabled;
    private static long total, maximum;
    private static int count;
    public static void begin() { total = maximum = 0; count = 0; enabled = true; }
    public static void record(long nanos) {
        if (!enabled) return;
        total += nanos;
        maximum = Math.max(maximum, nanos);
        count++;
    }
    public static void finish(String screen) {
        enabled = false;
        if (count < 5) throw new AssertionError("Not enough GPU timing samples for " + screen);
        System.out.printf(java.util.Locale.ROOT, "GLASS GPU %s: %d samples, mean %.3f ms, max %.3f ms%n",
                screen, count, total / (double)count / 1e6, maximum / 1e6);
    }
    private GlassGpuTiming() {}
}
