package pingplus.voicechat.client;

import net.labymod.addons.voicechat.core.audio.rnnoise.RNNoiseFilter;
import java.util.Arrays;
import java.util.Random;

final class NoiseSuppressionTest {
    static void run() throws Exception {
        check(NoiseSuppression.resourcePath("Windows 11", "amd64").equals("/natives/windows-x64/rnnoise.dll"), "Windows native mapping");
        check(NoiseSuppression.resourcePath("Mac OS X", "arm64").equals("/natives/macos-aarch64/librnnoise.dylib"), "Apple Silicon native mapping");
        check(NoiseSuppression.resourcePath("Linux", "x86_64").equals("/natives/linux-x64/librnnoise.so"), "Linux native mapping");
        byte[] bypass = new byte[1920]; new Random(4).nextBytes(bypass);
        byte[] original = bypass.clone();
        try (NoiseSuppression suppression = new NoiseSuppression()) {
            suppression.process(bypass, false);
            check(Arrays.equals(bypass, original), "Disabled suppression leaves PCM unchanged");
            Random random = new Random(1234);
            double inputEnergy = 0, outputEnergy = 0;
            for (int frame = 0; frame < 150; frame++) {
                byte[] pcm = new byte[1920];
                for (int i = 0; i < 960; i++) {
                    short sample = (short)(random.nextGaussian() * 700);
                    pcm[i * 2] = (byte)sample; pcm[i * 2 + 1] = (byte)(sample >> 8);
                    if (frame >= 50) inputEnergy += (double)sample * sample;
                }
                suppression.process(pcm, true);
                if (frame >= 50) for (int i = 0; i < 960; i++) {
                    short sample = (short)((pcm[i * 2] & 255) | (pcm[i * 2 + 1] << 8));
                    outputEnergy += (double)sample * sample;
                }
            }
            check(outputEnergy < inputEnergy * 0.6, "RNNoise reduces stationary background noise after warmup");
            suppression.process(bypass, false);
            check(Arrays.equals(bypass, original), "Toggle off bypasses processing");
            suppression.process(new byte[1920], true);
        }
        RNNoiseFilter filter = new RNNoiseFilter();
        filter.apply(new short[480]); filter.close(); filter.close();
        check(filter.isClosed(), "Filter close is idempotent");
        try { filter.apply(new short[480]); throw new AssertionError("Closed filter must reject processing"); }
        catch (IllegalStateException expected) { }
        System.out.println("PASS: bundled native RNNoise, background noise reduction, bypass, toggles, and safe cleanup");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
