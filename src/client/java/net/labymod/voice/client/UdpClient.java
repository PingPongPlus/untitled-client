/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.labymod.voice.client;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.labymod.voice.protocol.Encryption;
import net.labymod.voice.protocol.ProtocolVersion;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.packet.KeepAlivePacket;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.DisconnectType;
import net.labymod.voice.protocol.udp.AbstractUdpNetworking;
import net.labymod.voice.protocol.udp.receiver.PacketReceiver;
import net.labymod.voice.protocol.udp.session.FrameProcessor;
import net.labymod.voice.protocol.udp.session.NetworkSession;
import net.labymod.voice.protocol.udp.session.NetworkVersion;
import org.jetbrains.annotations.Nullable;

public class UdpClient
extends AbstractUdpNetworking
implements FrameProcessor {
    private static final long KEEP_ALIVE_RATE_IN_SECONDS = 1L;
    private final NetworkSession networkSession;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "VoiceChat-UdpClient");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledFuture<?> timeoutTask;
    private final ScheduledFuture<?> resendTask;
    private final ServerVoicePacketHandler handler;
    private final AtomicLong lastReceived = new AtomicLong(System.currentTimeMillis());
    private ConnectionState state = ConnectionState.HANDSHAKE;
    private int remoteProtocolVersion = -1;
    private Consumer<DisconnectType> disconnectListener = null;

    public UdpClient(ServerVoicePacketHandler handler, InetSocketAddress address, String publicKey, int timeout) throws Exception {
        super(new DatagramSocket());
        this.handler = handler;
        this.socket.setSoTimeout(timeout);
        this.networkSession = new NetworkSession(address, this, NetworkVersion.V3);
        this.networkSession.setSymmetricEncryption(new Encryption());
        this.networkSession.setAsymmetricEncryption(new Encryption(publicKey));
        this.timeoutTask = this.scheduler.scheduleAtFixedRate(() -> {
            long lastPacketDuration = System.currentTimeMillis() - this.lastReceived.get();
            if (lastPacketDuration > (long)timeout) {
                this.stop(DisconnectType.TIMEOUT);
                return;
            }
            if (this.state == ConnectionState.CONNECTED) {
                this.sendPacket(new KeepAlivePacket());
            }
        }, 1L, 1L, TimeUnit.SECONDS);
        this.resendTask = this.scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!this.isRunning() || this.socket.isClosed()) {
                    return;
                }
                this.networkSession.resendUnacknowledgedFrames(this.socket);
            }
            catch (Throwable e) {
                if (!this.isRunning() || this.socket.isClosed()) {
                    return;
                }
                e.printStackTrace();
            }
        }, 0L, 300L, TimeUnit.MILLISECONDS);
    }

    public void startReceivingPackets() {
        PacketReceiver receiver = new PacketReceiver(this.socket, unused -> this.networkSession, this::onSocketException);
        this.registerPacketReceiver(receiver);
    }

    protected void handlePacket(NetworkSession session, VoicePacket packet) {
        this.lastReceived.set(System.currentTimeMillis());
        packet.handle(this.handler);
    }

    @Override
    public void onFrameReceived(NetworkSession session, byte[] data) throws Exception {
        this.onFrameReceived(session, data, ProtocolVersion.VERSION);
    }

    @Override
    protected void onSocketException(@Nullable NetworkSession networkSession, Throwable t) {
        if (!this.isRunning()) {
            return;
        }
        t.printStackTrace();
        if (t instanceof SocketTimeoutException) {
            if (this.isRunning()) {
                this.stop(DisconnectType.TIMEOUT);
            }
        } else {
            this.stop(DisconnectType.ERROR);
        }
    }

    public void sendPacket(VoicePacket<?> packet) {
        if (!this.isRunning() || this.socket.isClosed()) {
            return;
        }
        try {
            this.sendPacket(packet, this.networkSession, null, ProtocolVersion.VERSION);
        }
        catch (Exception e) {
            if (!this.isRunning() || this.socket.isClosed()) {
                return;
            }
            e.printStackTrace();
        }
    }

    @Override
    public void stop(DisconnectType type) {
        super.stop(type);
        this.timeoutTask.cancel(true);
        this.resendTask.cancel(true);
        this.scheduler.shutdownNow();
        if (this.disconnectListener != null && type != DisconnectType.KICK) {
            this.disconnectListener.accept(type);
        }
    }

    public void setDisconnectListener(Consumer<DisconnectType> disconnectListener) {
        this.disconnectListener = disconnectListener;
    }

    public ConnectionState getState() {
        return this.state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
    }

    public void setRemoteProtocolVersion(int remoteProtocolVersion) {
        this.remoteProtocolVersion = remoteProtocolVersion;
    }

    public NetworkSession getNetworkSession() {
        return this.networkSession;
    }
}
