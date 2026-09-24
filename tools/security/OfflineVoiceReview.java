import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.labymod.voice.protocol.*;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.packet.server.moderation.WarnPacket;
import net.labymod.voice.protocol.type.*;
import net.labymod.voice.protocol.udp.session.*;

/** Offline checks against the supplied binary. Never creates sockets or sends traffic.
 * PASS means the documented behavior was reproduced, NOT that the library is secure. */
public class OfflineVoiceReview {
    static int passed;
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS: " + message); passed++;
    }
    static Map<?, ?> frames(NetworkSession s, String name) throws Exception {
        Field f = NetworkSession.class.getDeclaredField(name); f.setAccessible(true);
        return (Map<?, ?>) f.get(s);
    }
    static byte[] fragment(int id, int index, int count, byte[] payload) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); out.write(1);
        for (int value : new int[]{id, index, count, payload.length}) VoicePacket.writeShort((short)value, out);
        out.write(payload); return out.toByteArray();
    }
    static void receive(NetworkSession s, byte[] data) throws Exception {
        s.receiveSegment(null, data, 0, data.length, null);
    }
    public static void main(String[] args) throws Exception {
        AtomicInteger delivered = new AtomicInteger();
        ServerVoicePacketHandler handler = (ServerVoicePacketHandler) java.lang.reflect.Proxy.newProxyInstance(
            OfflineVoiceReview.class.getClassLoader(), new Class<?>[]{ServerVoicePacketHandler.class},
            (proxy, method, params) -> { if (method.getName().equals("handleWarn")) delivered.incrementAndGet(); return null; });
        FrameProcessor processor = new FrameProcessor() {
            @SuppressWarnings("unchecked") public void onFrameReceived(NetworkSession s, byte[] bytes) throws Exception {
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                VoicePacket<ServerVoicePacketHandler> packet = PacketRegistry.createPacket((byte)in.read());
                packet.read(in, ProtocolVersion.VERSION); packet.handle(handler);
            }
            public int sendToSocket(NetworkSession s, DatagramSocket unused, byte[] bytes) { return bytes.length; }
        };
        NetworkSession s = new NetworkSession(new InetSocketAddress("127.0.0.1", 1), processor, NetworkVersion.V3);
        Encryption encryption = new Encryption(); s.setSymmetricEncryption(encryption);
        WarnPacket warning = new WarnPacket("offline test");
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(PacketRegistry.getPacketId(warning)); warning.write(payload, ProtocolVersion.VERSION);
        byte[] clear = s.encodeSecureFrame(new byte[0], payload.toByteArray(), EncryptType.NONE, null);
        check(warning.getEncryptType() == EncryptType.SYM && Arrays.equals(s.decodeSecureFrame(clear, null), payload.toByteArray()),
            "SYM packet payload accepted with NONE envelope despite configured key");
        byte[] datagram = new byte[clear.length + 1]; System.arraycopy(clear, 0, datagram, 1, clear.length);
        receive(s, datagram); receive(s, datagram);
        check(delivered.get() == 2, "identical plaintext frames delivered twice (offline dispatch stub)");
        byte[] secure = s.encodeSecureFrame(new byte[0], payload.toByteArray(), EncryptType.SYM, null);
        check(Arrays.equals(s.decodeSecureFrame(secure, null), s.decodeSecureFrame(secure, null)), "encrypted envelope can be decoded repeatedly");
        byte[] repeated = new byte[32]; Arrays.fill(repeated, (byte)65);
        byte[] ciphertext = encryption.encrypt(repeated);
        check(Arrays.equals(Arrays.copyOfRange(ciphertext, 0, 16), Arrays.copyOfRange(ciphertext, 16, 32)), "identical plaintext blocks produce identical ciphertext blocks (ECB behavior)");
        check(Arrays.equals(ciphertext, encryption.encrypt(repeated)), "encryption is deterministic across messages");
        try { receive(s, fragment(1, -1, 1, new byte[]{1})); throw new AssertionError("negative index accepted"); }
        catch (ArrayIndexOutOfBoundsException expected) { check(true, "negative fragment index raises unchecked array exception"); }
        try { receive(s, fragment(2, 1, 1, new byte[]{1})); throw new AssertionError("large index accepted"); }
        catch (ArrayIndexOutOfBoundsException expected) { check(true, "fragment index equal to count raises unchecked array exception"); }
        for (int i = 10; i < 138; i++) receive(s, fragment(i, 0, 1000, new byte[]{1}));
        check(frames(s, "framesIn").size() >= 128, "128 incomplete frames retained before authentication (bounded check)");
        s.sendTcpFrame(null, new byte[]{1});
        check(!frames(s, "framesOut").isEmpty(), "outgoing frame queued in memory without network I/O");
        receive(s, fragment(200, 0, 1, new byte[0]));
        check(frames(s, "framesIn").isEmpty() && frames(s, "framesOut").isEmpty(), "unauthenticated empty reset clears incoming and outgoing queues");
        ByteArrayOutputStream truncated = new ByteArrayOutputStream(); VoicePacket.writeInt(4, truncated); truncated.write(65);
        String value = VoicePacket.readString(new ByteArrayInputStream(truncated.toByteArray()));
        check(value.length() == 4 && value.charAt(1) == 0, "truncated string accepted with zero-filled suffix");
        System.out.println("Completed " + passed + " offline checks. No sockets created; no network traffic sent.");
    }
}
