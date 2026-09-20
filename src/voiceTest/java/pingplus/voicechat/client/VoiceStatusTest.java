package pingplus.voicechat.client;

import javax.imageio.ImageIO;

public final class VoiceStatusTest {
    public static void run() throws Exception {
        expect(VoiceStatus.NOT_CONNECTED, false, null, 1000);
        expect(VoiceStatus.CONNECTED, true, null, 1000);
        expect(VoiceStatus.SPEAKING, true, 1000L, 1000);
        expect(VoiceStatus.SPEAKING, true, 1000L, 1299);
        expect(VoiceStatus.CONNECTED, true, 1000L, 1300);
        expect(VoiceStatus.NOT_CONNECTED, false, 1000L, 1001);
        expect(VoiceStatus.CONNECTED, true, null, 1001); // Reconnect without stale activity.
        expect(VoiceStatus.CONNECTED, true, 1000L, 999); // Wall-clock correction.
        try (var input = VoiceStatusTest.class.getResourceAsStream("/assets/voicechat/textures/font/voice_status.png")) {
            if (input == null) throw new AssertionError("Missing nametag icon atlas");
            var atlas = ImageIO.read(input);
            if (atlas.getWidth() != 36 || atlas.getHeight() != 9) throw new AssertionError("Invalid icon atlas dimensions");
            for (int icon = 0; icon < 3; icon++) {
                int opaque = 0;
                for (int y = 0; y < 9; y++) {
                    for (int x = icon * 12; x < (icon + 1) * 12; x++) {
                        if ((atlas.getRGB(x, y) >>> 24) != 0) opaque++;
                    }
                }
                if (opaque == 0 || opaque == 108) throw new AssertionError("Icon must have both pixels and transparent background");
            }
        }
        System.out.println("PASS: voice nametag presence, speaking timeout, disconnect, reconnect, and icon atlas");
    }

    private static void expect(VoiceStatus expected, boolean connected, Long last, long now) {
        if (VoiceStatus.resolve(connected, last, now) != expected) {
            throw new AssertionError("Unexpected voice status: expected " + expected);
        }
    }
}
