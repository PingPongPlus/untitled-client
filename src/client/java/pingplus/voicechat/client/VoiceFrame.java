package pingplus.voicechat.client;

import java.nio.ByteBuffer;

/** LabyMod wraps every Opus frame in an eight-byte, big-endian sequence number. */
final class VoiceFrame {
    static final int HEADER_BYTES = Long.BYTES;
    static final int MAX_OPUS_BYTES = 3828;
    final long sequence;
    final byte[] opus;
    private VoiceFrame(long sequence, byte[] opus) { this.sequence = sequence; this.opus = opus; }
    static byte[] encode(long sequence, byte[] opus) {
        if (sequence < 0 || opus.length == 0 || opus.length > MAX_OPUS_BYTES) throw new IllegalArgumentException("Invalid voice frame");
        return ByteBuffer.allocate(HEADER_BYTES + opus.length).putLong(sequence).put(opus).array();
    }
    static VoiceFrame decode(byte[] data) {
        if (data == null || data.length <= HEADER_BYTES || data.length > HEADER_BYTES + MAX_OPUS_BYTES) return null;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        long sequence = buffer.getLong();
        if (sequence < 0) return null;
        byte[] opus = new byte[buffer.remaining()]; buffer.get(opus);
        return new VoiceFrame(sequence, opus);
    }
}
