/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package net.labymod.voice.protocol.udp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import net.labymod.voice.protocol.PacketRegistry;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.packet.KeepAlivePacket;
import net.labymod.voice.protocol.packet.server.audio.ServerAudioPacket;
import net.labymod.voice.protocol.type.DisconnectType;
import net.labymod.voice.protocol.type.TransportType;
import net.labymod.voice.protocol.udp.receiver.PacketReceiver;
import net.labymod.voice.protocol.udp.session.NetworkSession;
import net.labymod.voice.protocol.udp.session.NetworkVersion;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractUdpNetworking {
    private static final boolean DEBUG = System.getenv("DEBUG_VOICE_PACKETS") != null;
    protected final DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<PacketReceiver> packetReceivers = new ArrayList<PacketReceiver>();

    protected AbstractUdpNetworking(DatagramSocket socket) {
        this.socket = socket;
        this.running.set(true);
    }

    protected void registerPacketReceiver(PacketReceiver receiver) {
        this.packetReceivers.add(receiver);
    }

    public void onFrameReceived(NetworkSession session, byte[] data, int protocolVersion) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        byte packetId = (byte)inputStream.read();
        VoicePacket packet = PacketRegistry.createPacket(packetId);
        if (packet == null) {
            throw new IllegalStateException("Unknown packet id " + packetId);
        }
        if (DEBUG && !(packet instanceof KeepAlivePacket) && !(packet instanceof ServerAudioPacket)) {
            System.out.println("[IN] " + packet.getClass().getSimpleName());
        }
        packet.read(inputStream, protocolVersion);
        this.handlePacket(session, packet);
    }

    protected abstract void handlePacket(NetworkSession var1, VoicePacket<?> var2);

    protected abstract void onSocketException(@NotNull NetworkSession var1, Throwable var2);

    public <T extends VoicePacket<?>> int sendPacket(T packet, NetworkSession session, Cipher cipher, int protocolVersion) throws Exception {
        if (!this.isRunning()) {
            throw new RuntimeException("Can't send packet while socket is closed");
        }
        if (session.getNetworkVersion() == NetworkVersion.UNIDENTIFIED) {
            throw new RuntimeException("Network version not set");
        }
        if (DEBUG && !(packet instanceof KeepAlivePacket) && !(packet instanceof ServerAudioPacket)) {
            System.out.println("[OUT] " + packet.getClass().getSimpleName());
        }
        ByteArrayOutputStream packetBuffer = new ByteArrayOutputStream();
        byte packetId = PacketRegistry.getPacketId(packet);
        packetBuffer.write(packetId);
        packet.write(packetBuffer, protocolVersion);
        if (session.getNetworkVersion() == NetworkVersion.V1) {
            return session.sendLegacySecureUdpFrame(this.socket, packetBuffer.toByteArray(), packet.getEncryptType(), cipher);
        }
        if (packet.getTransportType() == TransportType.UDP) {
            return session.sendSecureUdpFrame(this.socket, packet.getExposedIdentifier(), packetBuffer.toByteArray(), packet.getEncryptType(), cipher);
        }
        return session.sendSecureTcpFrame(this.socket, packet.getExposedIdentifier(), packetBuffer.toByteArray(), packet.getEncryptType(), cipher);
    }

    public boolean isRunning() {
        return this.running.get();
    }

    public void stop(DisconnectType type) {
        for (PacketReceiver receiver : this.packetReceivers) {
            receiver.kill();
        }
        this.running.set(false);
        this.socket.close();
    }
}
