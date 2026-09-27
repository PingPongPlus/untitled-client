package pingplus.voicechat.client.damageglass;

public final class DamageGlassTiming {
    /** Full material strength with the impact ripple finished. */
    public static final int ALWAYS_ON = 255 | (255 << 16);
    public static boolean eligible(boolean enabled, boolean players, boolean mobs,
                                   boolean player, boolean mob, boolean invisible, boolean firstPersonSelf) {
        return enabled && !invisible && !firstPersonSelf && ((player && players) || (mob && mobs));
    }

    /** Overlay UV channels carry the strength and hurt progress into each model's vertices. */
    public static int pack(int hurtTime, int duration, float partialTick) {
        return new Envelope().update(hurtTime, duration, partialTick);
    }
    /** A renewed hurt timer starts at the current material strength, avoiding a normal-texture flash. */
    public static final class Envelope {
        private float lastProgress = -1, lastStrength, attackFrom;
        public int update(int hurtTime, int duration, float partialTick) {
            if (hurtTime <= 0 || duration <= 0) {
                lastProgress = -1; lastStrength = attackFrom = 0;
                return 0;
            }
            float progress = Math.clamp((duration - hurtTime + partialTick) / duration, 0, 1);
            if (progress < lastProgress) attackFrom = lastStrength;
            float attack = smooth(Math.clamp(progress / .12f, 0, 1));
            float release = smooth(Math.clamp((1 - progress) / .55f, 0, 1));
            lastStrength = (attackFrom + (1 - attackFrom) * attack) * release;
            lastProgress = progress;
            // Keep replacement active at the endpoints to avoid a one-frame red flash.
            int strength = Math.max(1, Math.round(lastStrength * 255));
            return strength | Math.round(progress * 255) << 16;
        }
    }
    private static float smooth(float value) { return value * value * (3 - 2 * value); }
    private DamageGlassTiming() {}
}
