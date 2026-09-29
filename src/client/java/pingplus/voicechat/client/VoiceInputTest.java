package pingplus.voicechat.client;


/** Local-only input meter. Audio is neither saved nor sent to the voice service. */
final class VoiceInputTest implements AutoCloseable {
    private volatile boolean running = true;
    private volatile VoiceCapture line;
    private volatile double peak;
    private volatile double levelDb = -96;
    private volatile boolean gatePassing;
    private final java.util.function.BooleanSupplier gateEnabled;
    private final java.util.function.DoubleSupplier threshold;
    private final java.util.function.BooleanSupplier suppressionEnabled;
    private volatile String status = "Opening microphone...";
    VoiceInputTest(String selected, java.util.function.BooleanSupplier gateEnabled, java.util.function.DoubleSupplier threshold,
                   java.util.function.BooleanSupplier suppressionEnabled) {
        this.gateEnabled = gateEnabled; this.threshold = threshold; this.suppressionEnabled = suppressionEnabled;
        Thread.ofPlatform().daemon().name("Voice-Input-Test").start(() -> capture(selected));
    }
    double peak() { return peak; }
    double levelDb() { return levelDb; }
    boolean gatePassing() { return gatePassing; }
    String status() { return status; }
    private void capture(String selected) {
        try (NoiseSuppression suppression = new NoiseSuppression(); VoiceCapture input = open(selected)) {
            if (input == null) return;
            status = "Microphone opened — speak to test";
            byte[] pcm = new byte[1920];
            MicrophoneGate gate = new MicrophoneGate();
            long silentSince = System.nanoTime();
            boolean suppressionFailed = false;
            while (running) {
                int read = input.read(pcm);
                if (!running) break;
                peak = peak(pcm, read);
                if (read != pcm.length) continue;
                if (!suppressionFailed) {
                    try { suppression.process(pcm, suppressionEnabled.getAsBoolean()); }
                    catch (Exception | LinkageError e) {
                        suppressionFailed = true; suppression.close();
                        VoicechatClient.LOG.warn("RNNoise unavailable in microphone test", e);
                    }
                }
                levelDb = MicrophoneGate.levelDb(pcm, read);
                gatePassing = gate.process(pcm, gateEnabled.getAsBoolean(), threshold.getAsDouble());
                if (peak > 0.002) {
                    silentSince = System.nanoTime();
                    status = "Input detected (local test only)";
                } else if (System.nanoTime() - silentSince > 3_000_000_000L) {
                    status = "No signal — check headset mute and Windows input level";
                }
                if (suppressionFailed) status = "Noise suppression unavailable — testing raw input";
            }
        } catch (Exception | LinkageError e) {
            if (running) {
                status = "Cannot open microphone: " + e.getMessage();
                VoicechatClient.LOG.warn("Local microphone test failed", e);
            }
        } finally { peak = 0; line = null; }
    }
    private synchronized VoiceCapture open(String selected) throws java.io.IOException {
        if (!running) return null;
        line = VoiceCapture.open(selected);
        return line;
    }
    static double peak(byte[] pcm, int length) {
        int peak = 0;
        for (int i = 0; i + 1 < length; i += 2) {
            int sample = (short)((pcm[i] & 255) | pcm[i + 1] << 8);
            peak = Math.max(peak, Math.abs(sample));
        }
        return peak / 32768.0;
    }
    @Override public synchronized void close() {
        running = false;
        VoiceCapture current = line;
        if (current != null) current.close();
        peak = 0;
    }
}
