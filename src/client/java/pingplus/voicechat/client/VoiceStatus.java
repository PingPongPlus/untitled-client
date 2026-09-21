package pingplus.voicechat.client;

/** Presence is authoritative; recent packets alone cannot keep a departed player connected. */
public enum VoiceStatus {
    NOT_CONNECTED("\uE000", 0xFF5555),
    CONNECTED("\uE001", 0xFFFFFF),
    SPEAKING("\uE002", 0x55FF88);

    public static final long SPEAKING_HOLD_MILLIS = 300;
    public final String glyph;
    public final int color;

    VoiceStatus(String glyph, int color) {
        this.glyph = glyph;
        this.color = color;
    }

    public static VoiceStatus resolve(boolean connected, Long lastAudio, long now) {
        if (!connected) return NOT_CONNECTED;
        return lastAudio != null && now >= lastAudio && now - lastAudio < SPEAKING_HOLD_MILLIS
                ? SPEAKING : CONNECTED;
    }
}
