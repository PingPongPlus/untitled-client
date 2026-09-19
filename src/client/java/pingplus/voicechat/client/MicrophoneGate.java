package pingplus.voicechat.client;

/** Noise gate for 960-sample / 20 ms PCM frames, measured before microphone gain. */
final class MicrophoneGate {
    private static final int HOLD_FRAMES = 10;
    private static final double FADE_STEP = 1.0 / 240; // 5 ms at 48 kHz
    private int hold;
    private double gain;
    private boolean wasEnabled;

    static double levelDb(byte[] pcm, int length) {
        double energy = 0;
        int samples = Math.min(length, pcm.length) / 2;
        if (samples == 0) return -96;
        for (int i = 0; i < samples; i++) {
            double sample = (short)((pcm[i * 2] & 255) | pcm[i * 2 + 1] << 8) / 32768.0;
            energy += sample * sample;
        }
        return Math.max(-96, 10 * Math.log10(Math.max(1e-12, energy / samples)));
    }

    boolean process(byte[] pcm, boolean enabled, double thresholdDb) {
        if (!enabled) { reset(); return true; }
        if (!wasEnabled) { reset(); wasEnabled = true; }
        double level = levelDb(pcm, pcm.length);
        // Hysteresis plus a 200 ms hold keeps the ends of words from chattering.
        if (level >= thresholdDb - (hold > 0 ? 3 : 0)) hold = HOLD_FRAMES;
        else if (hold > 0) hold--;
        boolean open = hold > 0;
        boolean audible = open || gain > 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            gain = open ? Math.min(1, gain + FADE_STEP) : Math.max(0, gain - FADE_STEP);
            int sample = (short)((pcm[i] & 255) | pcm[i + 1] << 8);
            int scaled = (int)(sample * gain);
            pcm[i] = (byte)scaled; pcm[i + 1] = (byte)(scaled >> 8);
        }
        return audible;
    }
    void reset() { hold = 0; gain = 0; wasEnabled = false; }
}
