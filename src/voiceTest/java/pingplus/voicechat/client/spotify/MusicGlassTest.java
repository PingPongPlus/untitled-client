package pingplus.voicechat.client.spotify;

import java.awt.image.BufferedImage;

public final class MusicGlassTest {
    public static void main(String[] args) throws Exception {
        check(MusicGlass.decode("") == 0, "missing cover has no tint");
        check(MusicGlass.decode("not valid base64!") == 0, "malformed artwork is ignored");
        check(MusicGlass.decode("dGV4dA==") == 0, "non-image artwork is ignored");
        var cover = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        check(MusicGlass.palette(cover) == 0, "transparent artwork has no tint");
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) cover.setRGB(x, y, 0xFF9040D0);
        check(MusicGlass.palette(cover) == 0xFF9040D0, "purple artwork preserves its channels");
        var encoded = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(cover, "png", encoded);
        check(MusicGlass.decode(java.util.Base64.getEncoder().encodeToString(encoded.toByteArray())) == 0xFF9040D0,
                "encoded album artwork decodes to its palette");
        encoded.reset();
        javax.imageio.ImageIO.write(new BufferedImage(1025, 1, BufferedImage.TYPE_INT_ARGB), "png", encoded);
        check(MusicGlass.decode(java.util.Base64.getEncoder().encodeToString(encoded.toByteArray())) == 0,
                "oversized artwork is rejected");
        for (int y = 0; y < 4; y++) cover.setRGB(0, y, 0xFF00CC00);
        check(MusicGlass.palette(cover) == 0xFF9040D0, "minority complementary color does not muddy the palette");
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) cover.setRGB(x, y, 0xFF000000);
        check(MusicGlass.palette(cover) == 0xFF000000, "black cover stays neutral and is distinct from missing artwork");
        System.out.println("PASS: music glass image validation, transparency, color channels, dominant palette and neutral artwork");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
