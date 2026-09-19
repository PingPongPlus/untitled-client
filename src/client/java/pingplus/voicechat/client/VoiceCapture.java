package pingplus.voicechat.client;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALUtil;
import java.io.IOException;
import java.util.List;

/** Matches the supplied addon's ALCapture: 48 kHz stereo16, downmixed to mono. */
final class VoiceCapture implements AutoCloseable {
    private long device;
    private final short[] stereo = new short[1920];
    static List<String> names() {
        List<String> names = ALUtil.getStringList(0, ALC11.ALC_CAPTURE_DEVICE_SPECIFIER);
        return names == null ? List.of() : List.copyOf(names);
    }
    static VoiceCapture open(String selected) throws IOException {
        String normalized = VoiceDevices.normalizeInput(selected);
        String name = null;
        if (normalized != null && !normalized.isEmpty()) {
            if (!normalized.startsWith("openal:") || !names().contains(normalized.substring(7)))
                throw new IOException("Selected microphone disconnected; select it again in Audio devices");
            name = normalized.substring(7);
        }
        VoiceCapture capture = new VoiceCapture();
        capture.device = ALC11.alcCaptureOpenDevice(name, 48000, AL10.AL_FORMAT_STEREO16, 1920);
        if (capture.device == 0) throw new IOException("Cannot open selected microphone through OpenAL");
        try {
            ALC11.alcCaptureStart(capture.device);
            capture.checkError();
            return capture;
        } catch (IOException e) { capture.close(); throw e; }
    }
    int read(byte[] mono) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (true) {
            synchronized (this) {
                if (device == 0) return 0;
                int available = ALC10.alcGetInteger(device, ALC11.ALC_CAPTURE_SAMPLES);
                checkError();
                if (available >= 960) {
                    ALC11.alcCaptureSamples(device, stereo, 960);
                    checkError();
                    downmix(stereo, mono);
                    return 1920;
                }
            }
            if (System.nanoTime() > deadline) throw new IOException("Microphone opened but supplied no audio frames");
            Thread.sleep(2);
        }
    }
    static void downmix(short[] stereo, byte[] mono) {
        for (int i = 0; i < stereo.length / 2; i++) {
            int sample = (stereo[i * 2] + stereo[i * 2 + 1]) / 2;
            mono[i * 2] = (byte)sample;
            mono[i * 2 + 1] = (byte)(sample >> 8);
        }
    }
    private void checkError() throws IOException {
        int error = ALC10.alcGetError(device);
        if (error != ALC10.ALC_NO_ERROR) throw new IOException("OpenAL capture error " + error);
    }
    @Override public synchronized void close() {
        if (device != 0) {
            ALC11.alcCaptureStop(device);
            ALC11.alcCaptureCloseDevice(device);
            device = 0;
        }
    }
}
