package pingplus.voicechat.client;

import net.labymod.addons.voicechat.core.audio.rnnoise.RNNoiseFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Per-microphone RNNoise state. Process continuously so its model stays warm between speech. */
final class NoiseSuppression implements AutoCloseable {
    private static boolean loaded;
    private RNNoiseFilter filter;
    private final short[] samples = new short[480];

    static synchronized void loadNative() throws IOException {
        if (loaded) return;
        String resource = resourcePath(System.getProperty("os.name"), System.getProperty("os.arch"));
        try (var stream = NoiseSuppression.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("No bundled RNNoise library for " + resource);
            Path directory = Files.createTempDirectory("pingplus-rnnoise-");
            directory.toFile().deleteOnExit();
            Path library = directory.resolve(resource.substring(resource.lastIndexOf('/') + 1));
            Files.copy(stream, library);
            library.toFile().deleteOnExit();
            System.load(library.toAbsolutePath().toString());
            loaded = true;
        }
    }

    static String resourcePath(String os, String architecture) throws IOException {
        String name = os.toLowerCase(Locale.ROOT);
        String platform = name.contains("win") && !name.contains("darwin") ? "windows"
            : name.contains("mac") || name.contains("darwin") ? "macos"
            : name.contains("linux") ? "linux" : "";
        String arch = switch (architecture.toLowerCase(Locale.ROOT)) {
            case "amd64", "x86_64", "x86-64", "x64" -> "x64";
            case "i386", "i486", "i586", "i686", "x86", "x86_32" -> "x86";
            case "aarch64", "arm64" -> "aarch64";
            default -> throw new IOException("Unsupported RNNoise architecture: " + architecture);
        };
        if (platform.isEmpty()) throw new IOException("Unsupported RNNoise platform: " + os);
        return "/natives/" + platform + "-" + arch + "/" + (platform.equals("windows") ? "rnnoise.dll"
            : platform.equals("macos") ? "librnnoise.dylib" : "librnnoise.so");
    }

    void process(byte[] pcm, boolean enabled) throws IOException {
        if (!enabled) { close(); return; }
        if (pcm.length != 1920) throw new IllegalArgumentException("Expected 20 ms mono PCM at 48 kHz");
        if (filter == null) { loadNative(); filter = new RNNoiseFilter(); }
        // RNNoise consumes 480 samples, while the wire codec consumes 960.
        for (int offset = 0; offset < pcm.length; offset += 960) {
            for (int i = 0; i < samples.length; i++) {
                int index = offset + i * 2;
                samples[i] = (short)((pcm[index] & 255) | (pcm[index + 1] << 8));
            }
            filter.apply(samples);
            for (int i = 0; i < samples.length; i++) {
                int index = offset + i * 2;
                pcm[index] = (byte)samples[i]; pcm[index + 1] = (byte)(samples[i] >> 8);
            }
        }
    }

    @Override public void close() {
        if (filter != null) { filter.close(); filter = null; }
    }
}
