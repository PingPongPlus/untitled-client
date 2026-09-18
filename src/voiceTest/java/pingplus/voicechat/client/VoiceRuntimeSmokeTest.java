package pingplus.voicechat.client;

import net.labymod.opus.OpusCodec;
import net.labymod.voice.protocol.Encryption;
import net.labymod.voice.protocol.ProtocolVersion;
import net.labymod.voice.protocol.packet.client.auth.HandshakePacket;
import net.labymod.voice.protocol.packet.client.audio.ClientAudioPacket;
import net.labymod.voice.protocol.packet.client.world.SwitchServerPacket;
import net.labymod.voice.protocol.type.AuthenticationMethod;
import java.io.*;
import java.util.Arrays;

/** Runs without a microphone, Minecraft account, or external network connection. */
public final class VoiceRuntimeSmokeTest {
    public static void main(String[] args) throws Exception {
        check(VoiceInputTest.peak(new byte[1920], 1920) == 0, "Microphone meter detects silence");
        check(VoiceInputTest.peak(new byte[]{0, (byte)128, 0, 0}, 4) == 1, "Microphone meter handles negative full-scale PCM");
        check(VoiceInputTest.peak(new byte[]{0, 64, 0, (byte)128}, 2) == 0.5, "Microphone meter respects bytes read");
        byte[] mono = new byte[8];
        VoiceCapture.downmix(new short[]{10000, 0, 0, 10000, -32768, -32768, 32767, 32767}, mono);
        check(Arrays.equals(mono, new byte[]{(byte)0x88, 0x13, (byte)0x88, 0x13, 0, (byte)0x80, (byte)0xff, 0x7f}),
            "LabyMod stereo capture preserves either headset channel and does not overflow");
        try {
            VoiceCapture.open("openal:nonexistent-device-for-regression-test");
            throw new AssertionError("Missing OpenAL microphone must not fall back");
        } catch (IOException expected) { check(expected.getMessage().contains("disconnected"), "Missing OpenAL microphone error"); }
        var inputs = VoiceDevices.available(true);
        var outputs = VoiceDevices.available(false);
        check(inputs.getFirst().id().isEmpty() && outputs.getFirst().id().isEmpty(), "System default is always selectable");
        for (boolean input : new boolean[]{true, false}) {
            try {
                VoiceDevices.openLine(input, "nonexistent-device-for-regression-test");
                throw new AssertionError("Missing device must not silently fall back to system default");
            } catch (javax.sound.sampled.LineUnavailableException expected) {
                check(expected.getMessage().contains("disconnected"), "Missing device has actionable error");
            }
        }
        System.out.println("PASS: audio device enumeration and missing-device handling (no devices opened)");
        check(ProtocolVersion.VERSION == 6, "Supplied runtime must use voice protocol 6");
        HandshakePacket handshake = new HandshakePacket();
        handshake.setMethod(AuthenticationMethod.MOJANG);
        handshake.setString("VoiceTest");
        handshake.setSymKey(new byte[16]);
        var buffer = new ByteArrayOutputStream();
        handshake.write(buffer, ProtocolVersion.VERSION);
        HandshakePacket restored = new HandshakePacket();
        restored.read(new ByteArrayInputStream(buffer.toByteArray()), ProtocolVersion.VERSION);
        check(restored.getMethod() == AuthenticationMethod.MOJANG && "VoiceTest".equals(restored.getString()), "Mojang handshake roundtrip");
        check(restored.getProtocolVersion() == 6, "Handshake version");
        SwitchServerPacket server = new SwitchServerPacket();
        server.setServer("voice-test.invalid"); server.setPort(25566);
        buffer.reset(); server.write(buffer, 6);
        SwitchServerPacket serverCopy = new SwitchServerPacket(); serverCopy.read(new ByteArrayInputStream(buffer.toByteArray()), 6);
        check(serverCopy.getPort() == 25566 && server.getServer().equals(serverCopy.getServer()), "Server identity roundtrip");
        Encryption encryption = new Encryption();
        byte[] message = buffer.toByteArray();
        check(Arrays.equals(message, encryption.decrypt(encryption.encrypt(message))), "AES packet roundtrip");
        check(VoiceAudio.clip(40000) == 32767 && VoiceAudio.clip(-40000) == -32768, "PCM clips instead of wrapping");
        byte[] fixture = VoiceFrame.encode(0x0102030405060708L, new byte[]{42, 43});
        check(Arrays.equals(fixture, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 42, 43}), "Audio sequence is an eight-byte big-endian prefix");
        check(VoiceFrame.decode(null) == null && VoiceFrame.decode(new byte[8]) == null, "Truncated audio rejected");
        check(VoiceFrame.decode(new byte[VoiceFrame.MAX_OPUS_BYTES + 9]) == null, "Oversized audio rejected");
        OpusCodec.setupWithTemporaryFolder();
        OpusCodec encoder = OpusCodec.newBuilder().withBitrate(48000).build();
        OpusCodec decoder = OpusCodec.newBuilder().withBitrate(48000).build();
        try {
            long energy = 0;
            for (int frame = 0; frame < 20; frame++) {
                byte[] pcm = new byte[1920];
                for (int i = 0; i < 960; i++) {
                    short sample = (short)(Math.sin((frame * 960 + i) * 2 * Math.PI * 440 / 48000) * 10000);
                    pcm[i * 2] = (byte)sample; pcm[i * 2 + 1] = (byte)(sample >> 8);
                }
                byte[] encoded = encoder.encodeFrame(pcm);
                check(encoded.length > 0 && encoded.length < pcm.length, "Opus compresses a frame");
                ClientAudioPacket audio = new ClientAudioPacket(); audio.setData(VoiceFrame.encode(frame, encoded));
                buffer.reset(); audio.write(buffer, 6);
                ClientAudioPacket copy = new ClientAudioPacket(); copy.read(new ByteArrayInputStream(buffer.toByteArray()), 6);
                VoiceFrame voice = VoiceFrame.decode(copy.getData());
                check(voice != null && voice.sequence == frame && Arrays.equals(encoded, voice.opus), "Sequenced audio packet roundtrip");
                byte[] decoded = decoder.decodeFrame(voice.opus);
                check(decoded.length == 1920, "Decoded 20 ms PCM size");
                for (int i = 0; i < 960; i++) energy += Math.abs((short)((decoded[i * 2] & 255) | decoded[i * 2 + 1] << 8));
            }
            check(energy > 1000000, "Decoded audio is non-silent");
        } finally { encoder.destroy(); decoder.destroy(); }
        System.out.println("PASS: protocol 6 handshake, server identity, AES, sequenced audio packets, malformed frame rejection, PCM clipping, native Opus encode/decode");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
