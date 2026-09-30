package pingplus.voicechat.client.hud.metrics;

import java.util.ArrayDeque;

/** Physical presses in the preceding second, using a monotonic clock. */
public final class CpsCounter {
    private static final long SECOND = 1_000_000_000L;
    private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();

    public void press(int button, long now) {
        if (button == 0) left.addLast(now);
        else if (button == 1) right.addLast(now);
        expire(left, now);
        expire(right, now);
    }

    public int left(long now) { expire(left, now); return left.size(); }
    public int right(long now) { expire(right, now); return right.size(); }
    public void reset() { left.clear(); right.clear(); }

    private static void expire(ArrayDeque<Long> presses, long now) {
        while (!presses.isEmpty() && now - presses.peekFirst() >= SECOND) presses.removeFirst();
    }
}
