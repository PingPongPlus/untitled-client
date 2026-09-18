package pingplus.voicechat.client;


/** Local-only input meter. Audio is neither saved nor sent to the voice service. */
final class VoiceInputTest implements AutoCloseable {
    private volatile boolean running = true;
    private volatile VoiceCapture line;
    private volatile double peak;
    private volatile String status = "Opening microphone...";
    VoiceInputTest(String selected) {
        Thread.ofPlatform().daemon().name("Voice-Input-Test").start(() -> capture(selected));
    }
    double peak() { return peak; }
    String status() { return status; }
    private void capture(String selected) {
        try (VoiceCapture input = open(selected)) {
            if (input == null) return;
            status = "Microphone opened — speak to test";
            byte[] pcm = new byte[1920];
            long silentSince = System.nanoTime();
            while (running) {
                int read = input.read(pcm);
                if (!running) break;
                peak = peak(pcm, read);
                if (peak > 0.002) {
                    silentSince = System.nanoTime();
                    status = "Input detected (local test only)";
                } else if (System.nanoTime() - silentSince > 3_000_000_000L) {
                    status = "No signal — check headset mute and Windows input level";
                }
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
