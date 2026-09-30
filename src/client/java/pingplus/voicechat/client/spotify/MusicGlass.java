package pingplus.voicechat.client.spotify;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

/** Album palette cache and time-based tint, owned by the client thread. No audio capture. */
public final class MusicGlass {
    private static String artwork = "";
    private static int palette;
    private static float red, green, blue, strength;
    private static long lastFrame;
    private static boolean playing;

    public static void tick(boolean inWorld) {
        var track = SpotifyClient.INSTANCE.state().playback();
        playing = inWorld && SpotifySettings.musicGlass() && track != null && track.playing();
        String wanted = playing ? track.image() : "";
        if (!wanted.equals(artwork)) {
            artwork = wanted;
            palette = decode(wanted);
        }
    }

    /** Transparent overlay, capped at 28% even at maximum intensity to preserve contrast. */
    public static int tint() {
        long now = System.nanoTime();
        double seconds = lastFrame == 0 ? 0 : Math.clamp((now - lastFrame) / 1e9, 0, .1);
        lastFrame = now;
        float blend = (float)(1 - Math.exp(-seconds / .45));
        float target = playing && SpotifySettings.musicGlass() && palette != 0
                ? SpotifySettings.musicIntensity() / 100f * .28f : 0;
        if (palette != 0) {
            if (strength < .001f) {
                red = (palette >>> 16) & 255; green = (palette >>> 8) & 255; blue = palette & 255;
            } else {
                red += (((palette >>> 16) & 255) - red) * blend;
                green += (((palette >>> 8) & 255) - green) * blend;
                blue += ((palette & 255) - blue) * blend;
            }
        }
        strength += (target - strength) * blend;
        return (Math.round(strength * 255) << 24) | (Math.round(red) << 16) | (Math.round(green) << 8) | Math.round(blue);
    }

    static int decode(String data) {
        if (data.isEmpty() || data.length() > 2_666_672) return 0;
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return 0;
            var reader = readers.next();
            try {
                reader.setInput(input);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > 1024 || h > 1024) return 0;
                var params = reader.getDefaultReadParam();
                params.setSourceSubsampling(Math.max(1, w / 32), Math.max(1, h / 32), 0, 0);
                return palette(reader.read(0, params));
            } finally { reader.dispose(); }
        } catch (Exception ignored) { return 0; }
    }

    /** Select a dominant color family instead of averaging opposing colors into gray. */
    static int palette(BufferedImage image) {
        double[] weights = new double[64], rs = new double[64], gs = new double[64], bs = new double[64];
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int pixel = image.getRGB(x, y), a = pixel >>> 24;
            if (a < 128) continue;
            int r = (pixel >>> 16) & 255, g = (pixel >>> 8) & 255, b = pixel & 255;
            int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
            double weight = (a / 255.0) * (.15 + (max - min) / 255.0);
            int bin = ((r >> 6) << 4) | ((g >> 6) << 2) | (b >> 6);
            weights[bin] += weight; rs[bin] += r * weight; gs[bin] += g * weight; bs[bin] += b * weight;
        }
        int best = 0;
        for (int i = 1; i < weights.length; i++) if (weights[i] > weights[best]) best = i;
        if (weights[best] == 0) return 0;
        return 0xFF000000 | ((int)Math.round(rs[best] / weights[best]) << 16)
                | ((int)Math.round(gs[best] / weights[best]) << 8) | (int)Math.round(bs[best] / weights[best]);
    }

    private MusicGlass() {}
}
