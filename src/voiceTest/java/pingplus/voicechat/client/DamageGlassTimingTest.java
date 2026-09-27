package pingplus.voicechat.client;

import pingplus.voicechat.client.damageglass.DamageGlassTiming;

public final class DamageGlassTimingTest {
    public static void main(String[] args) {
        for (int flags = 0; flags < 8; flags++) {
            boolean master = (flags & 1) != 0, players = (flags & 2) != 0, mobs = (flags & 4) != 0;
            check(DamageGlassTiming.eligible(master, players, mobs, true, false, false, false) == (master && players), "player toggle matrix");
            check(DamageGlassTiming.eligible(master, players, mobs, false, true, false, false) == (master && mobs), "mob toggle matrix");
            check(!DamageGlassTiming.eligible(master, players, mobs, true, false, true, false), "invisible player");
            check(!DamageGlassTiming.eligible(master, players, mobs, true, false, false, true), "first-person self");
            check(!DamageGlassTiming.eligible(master, players, mobs, false, false, false, false), "non-living decorations");
        }
        check(DamageGlassTiming.pack(0, 10, .5f) == 0, "healthy entity has no effect");
        check(DamageGlassTiming.pack(10, 0, .5f) == 0, "invalid duration");
        check((DamageGlassTiming.pack(8, 10, 0) & 255) == 255, "peak glass");
        int previous = 255;
        for (int i = 50; i <= 100; i++) {
            int tick = i / 10;
            int strength = DamageGlassTiming.pack(10 - tick, 10, (i % 10) / 10f) & 255;
            check(strength <= previous, "smooth release");
            previous = strength;
        }
        int first = DamageGlassTiming.pack(8, 10, .5f);
        int second = DamageGlassTiming.pack(2, 10, .5f);
        check(first != second && DamageGlassTiming.pack(8, 10, .5f) == first, "simultaneous independent hurt timers");
        check((DamageGlassTiming.pack(9, 10, .5f) & 255) > (second & 255), "repeated hit refreshes effect");
        var envelope = new DamageGlassTiming.Envelope();
        int beforeHit = envelope.update(3, 10, .5f) & 255;
        check((envelope.update(10, 10, 0) & 255) == beforeHit, "repeat starts at existing strength without flash");
        check((envelope.update(8, 10, 0) & 255) == 255, "repeat returns to peak");
        check(envelope.update(0, 10, 0) == 0, "timer end resets envelope");
        check((envelope.update(10, 10, 0) & 255) == 1, "new hit after recovery starts a fresh attack");
        System.out.println("PASS: damage glass toggle matrix, visibility, fade, repeated and simultaneous hits");
    }
    private static void check(boolean passed, String label) { if (!passed) throw new AssertionError(label); }
}
