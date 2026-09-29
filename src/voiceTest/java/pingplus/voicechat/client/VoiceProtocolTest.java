package pingplus.voicechat.client;

import net.labymod.voice.protocol.*;
import net.labymod.voice.protocol.packet.client.channel.CreateChannelPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelShowPacket;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.udp.session.*;
import net.labymod.voice.protocol.util.properties.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** Exercises recovered generic properties and reliable transport without a socket. */
final class VoiceProtocolTest {
    static void run() throws Exception {
        int packets = 0;
        for (int id = 0; id < 256; id++) {
            if (!PacketRegistry.isValidPacket((byte)id)) continue;
            VoicePacket<?> packet = PacketRegistry.createPacket((byte)id);
            check(packet != null && PacketRegistry.getPacketId(packet) == (byte)id, "Packet registry ID " + id);
            packets++;
        }
        check(packets == 38, "All original packet types are included");
        UUID owner = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        ChannelProperties properties = new ChannelProperties();
        properties.setOwner(owner); properties.setName("Test channel");
        properties.setPasswordPlain("local test"); properties.setProximity(true);
        properties.setVisibility(ChannelVisibility.SERVER);
        CreateChannelPacket create = new CreateChannelPacket(properties);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); create.write(bytes, 6);
        CreateChannelPacket copy = new CreateChannelPacket(); copy.read(new ByteArrayInputStream(bytes.toByteArray()), 6);
        check(copy.getProperties().getOwner().equals(owner) && copy.getProperties().isPassword("local test")
            && copy.getProperties().isProximity(), "Channel property roundtrip");
        ChannelShowPacket show = new ChannelShowPacket(owner, properties);
        bytes.reset(); show.write(bytes, 6);
        check(!new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.ISO_8859_1).contains(properties.getProperty(ChannelProperties.Key.PASSWORD, "")),
            "Channel password hash is hidden from client payload");
        ChannelShowPacket shown = new ChannelShowPacket(); shown.read(new ByteArrayInputStream(bytes.toByteArray()), 6);
        check(shown.getChannels().size() == 1 && shown.getChannels().getFirst().getProperties().getName().equals("Test channel"),
            "Client-visible channel properties roundtrip");
        UserProperties user = new UserProperties(); user.setName("VoiceTest"); user.setInputMuted(true); user.setOutputMuted(true);
        bytes.reset(); user.write(bytes); UserProperties restored = new UserProperties(); restored.read(new ByteArrayInputStream(bytes.toByteArray()));
        check(restored.isInputMuted() && restored.isOutputMuted() && restored.getName().equals("VoiceTest"), "User properties roundtrip");

        Transport outbound = new Transport(), inbound = new Transport();
        NetworkSession sender = new NetworkSession(new InetSocketAddress("127.0.0.1", 1), outbound, NetworkVersion.V3);
        NetworkSession receiver = new NetworkSession(new InetSocketAddress("127.0.0.1", 2), inbound, NetworkVersion.V3);
        byte[] sharedKey = new byte[16]; new Random(9).nextBytes(sharedKey);
        sender.setSymmetricEncryption(new Encryption(sharedKey)); receiver.setSymmetricEncryption(new Encryption(sharedKey));
        byte[] payload = new byte[1800]; new Random(1).nextBytes(payload);
        sender.sendSecureTcpFrame(null, new byte[0], payload, EncryptType.SYM, null);
        check(outbound.datagrams.size() > 1, "Large secure frame fragmented");
        List<byte[]> fragments = new ArrayList<>(outbound.datagrams); Collections.reverse(fragments);
        for (byte[] fragment : fragments) receiver.receiveSegment(null, fragment, 0, fragment.length, null);
        check(inbound.frames.size() == 1 && Arrays.equals(inbound.frames.getFirst(), payload), "Out-of-order fragments reconstruct encrypted frame");
        for (byte[] ack : inbound.datagrams) sender.receiveSegment(null, ack, 0, ack.length, null);
        outbound.datagrams.clear(); sender.resendUnacknowledgedFrames(null);
        check(outbound.datagrams.isEmpty(), "Acknowledged fragments are not retransmitted");
        // UDP frame size is bounded by the runtime; use a normal voice-size payload.
        byte[] voice = Arrays.copyOf(payload, 100);
        outbound.datagrams.clear(); sender.sendSecureUdpFrame(null, new byte[]{1, 2, 3}, voice, EncryptType.SYM, null);
        byte[] udp = outbound.datagrams.getFirst(); receiver.receiveSegment(null, udp, 0, udp.length, null);
        check(Arrays.equals(inbound.frames.getLast(), voice), "UDP audio envelope roundtrip with identifier");
        System.out.println("PASS: all 38 packet registrations, channel/user properties, encrypted UDP and reliable fragment reassembly/acknowledgements");
    }
    private static final class Transport implements FrameProcessor {
        final List<byte[]> datagrams = new ArrayList<>(), frames = new ArrayList<>();
        public void onFrameReceived(NetworkSession session, byte[] data) { frames.add(data); }
        public int sendToSocket(NetworkSession session, DatagramSocket socket, byte[] data) { datagrams.add(data); return data.length; }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
