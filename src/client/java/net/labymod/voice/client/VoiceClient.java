/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package net.labymod.voice.client;

import com.google.gson.JsonObject;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.labymod.voice.client.UdpClient;
import net.labymod.voice.client.auth.AuthenticationResponse;
import net.labymod.voice.client.auth.Authenticator;
import net.labymod.voice.client.listener.VoiceClientListener;
import net.labymod.voice.client.listener.VoiceClientListenerAdapter;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.packet.KeepAlivePacket;
import net.labymod.voice.protocol.packet.client.DisconnectPacket;
import net.labymod.voice.protocol.packet.client.audio.ClientAudioPacket;
import net.labymod.voice.protocol.packet.client.auth.HandshakePacket;
import net.labymod.voice.protocol.packet.client.channel.CreateChannelPacket;
import net.labymod.voice.protocol.packet.client.channel.DeleteChannelPacket;
import net.labymod.voice.protocol.packet.client.channel.MoveUserToChannelPacket;
import net.labymod.voice.protocol.packet.client.channel.RequestChannelResetPacket;
import net.labymod.voice.protocol.packet.client.channel.SubscribeChannelsPacket;
import net.labymod.voice.protocol.packet.client.channel.UpdateChannelPacket;
import net.labymod.voice.protocol.packet.client.channel.UpdatePropertiesPacket;
import net.labymod.voice.protocol.packet.client.moderation.MutePlayerPacket;
import net.labymod.voice.protocol.packet.client.moderation.ReportPlayerPacket;
import net.labymod.voice.protocol.packet.client.moderation.RequestPlayerMetaPacket;
import net.labymod.voice.protocol.packet.client.moderation.UnmutePlayerPacket;
import net.labymod.voice.protocol.packet.client.moderation.UpdateNotePlayerPacket;
import net.labymod.voice.protocol.packet.client.moderation.WarnPlayerPacket;
import net.labymod.voice.protocol.packet.client.world.SwitchServerPacket;
import net.labymod.voice.protocol.packet.client.world.UpdateVisiblePlayersPacket;
import net.labymod.voice.protocol.packet.server.audio.ServerAudioPacket;
import net.labymod.voice.protocol.packet.server.auth.HandshakeResponsePacket;
import net.labymod.voice.protocol.packet.server.auth.InvalidKeyPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelAlertPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelHidePacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelResetPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelShowPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelUpdatePacket;
import net.labymod.voice.protocol.packet.server.channel.UserHidePacket;
import net.labymod.voice.protocol.packet.server.channel.UserShowPacket;
import net.labymod.voice.protocol.packet.server.channel.UserSwitchChannelPacket;
import net.labymod.voice.protocol.packet.server.channel.UserUpdatePacket;
import net.labymod.voice.protocol.packet.server.moderation.KickPacket;
import net.labymod.voice.protocol.packet.server.moderation.PlayerUpdateMetaPacket;
import net.labymod.voice.protocol.packet.server.moderation.WarnPacket;
import net.labymod.voice.protocol.packet.server.world.PlayerAlivePacket;
import net.labymod.voice.protocol.packet.server.world.PlayerDeadPacket;
import net.labymod.voice.protocol.type.AuthenticationMethod;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.DisconnectType;
import net.labymod.voice.protocol.type.HandshakeResponse;
import net.labymod.voice.protocol.udp.session.NetworkSession;
import net.labymod.voice.protocol.util.HttpRequest;
import net.labymod.voice.protocol.util.properties.ChannelProperties;
import net.labymod.voice.protocol.util.properties.UserProperties;

public class VoiceClient
implements ServerVoicePacketHandler {
    private static final String URL_PUBLIC_KEY = "https://dl.labymod.net/labyconnect/voice/key.pub";
    private static final String BRAND_NAME = "laby";
    private final AtomicBoolean isConnectingState = new AtomicBoolean(false);
    private final Map<AuthenticationMethod, Authenticator> authenticators = new HashMap<AuthenticationMethod, Authenticator>();
    private final Set<UUID> remoteVisiblePlayers = new HashSet<UUID>();
    private UdpClient udpClient;
    private VoiceClientListener listener = new VoiceClientListenerAdapter();
    private boolean isAdmin;
    private int timeout = 20000;

    public void connect(InetSocketAddress serverAddress, AuthenticationMethod method) {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "VoiceChat-Connect");
            thread.setDaemon(true);
            return thread;
        });
        executor.execute(() -> {
            try {
                if (this.isConnectingState.getAndSet(true)) {
                    return;
                }
                try {
                    AuthenticationResponse response;
                    byte[] sharedKey;
                    block16: {
                        String pubKey;
                        if (this.udpClient != null && this.udpClient.isRunning()) {
                            this.udpClient.stop(DisconnectType.DISCONNECT);
                        }
                        if ((pubKey = HttpRequest.getRequest(URL_PUBLIC_KEY)) == null) {
                            return;
                        }
                        this.udpClient = new UdpClient(this, serverAddress, pubKey, this.timeout);
                        NetworkSession networkSession = this.udpClient.getNetworkSession();
                        sharedKey = networkSession.getSymmeticEncryption().getShareKey().getEncoded();
                        byte[] publicKey = networkSession.getAsymmetricEncryption().getPublicKey().getEncoded();
                        MessageDigest sha = MessageDigest.getInstance("SHA-1");
                        sha.update(BRAND_NAME.getBytes(StandardCharsets.ISO_8859_1));
                        sha.update(sharedKey);
                        sha.update(publicKey);
                        String hash = URLEncoder.encode(new BigInteger(sha.digest()).toString(16), "UTF-8");
                        Authenticator authenticator = this.authenticators.get((Object)method);
                        if (authenticator == null) {
                            throw new RuntimeException("No authenticator for " + String.valueOf((Object)method) + " defined!");
                        }
                        response = authenticator.request(hash);
                        if (response != null) break block16;
                        this.udpClient.stop(DisconnectType.AUTHENTICATION_FAILED);
                        return;
                    }
                    try {
                        this.udpClient.startReceivingPackets();
                        HandshakePacket handshake = new HandshakePacket();
                        handshake.setMethod(method);
                        handshake.setSymKey(sharedKey);
                        handshake.setUniqueId(response.getPlayer());
                        handshake.setString(response.getString());
                        this.udpClient.setDisconnectListener(reason -> this.listener.onDisconnected((DisconnectType)((Object)((Object)reason)), "Disconnected"));
                        for (int i = 0; i < 5 && this.udpClient.isRunning(); ++i) {
                            if (this.udpClient.getState() != ConnectionState.HANDSHAKE) {
                                break;
                            }
                            this.udpClient.sendPacket(handshake);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        this.udpClient.stop(DisconnectType.AUTHENTICATION_FAILED);
                    }
                }
                finally {
                    this.isConnectingState.set(false);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
        executor.shutdown();
    }

    @Override
    public void handleHandshakeResponse(HandshakeResponsePacket packet) {
        this.udpClient.setRemoteProtocolVersion(packet.getProtocolVersion());
        this.remoteVisiblePlayers.clear();
        if (packet.getResponse() == HandshakeResponse.SUCCESS) {
            this.udpClient.setState(ConnectionState.CONNECTED);
            this.isAdmin = packet.isStaff();
            this.listener.onAuthenticated(this.isAdmin);
        } else {
            this.udpClient.stop(DisconnectType.of(packet.getResponse()));
        }
    }

    @Override
    public void handleKick(KickPacket packet) {
        this.udpClient.stop(DisconnectType.KICK);
        this.listener.onDisconnected(DisconnectType.KICK, packet.getReason());
        this.remoteVisiblePlayers.clear();
    }

    @Override
    public void handleWarn(WarnPacket packet) {
        this.listener.onWarn(packet.getReason());
    }

    @Override
    public void handlePlayerAlive(PlayerAlivePacket packet) {
        Iterator<UUID> iterator = packet.getUniqueIds().iterator();
        while (iterator.hasNext()) {
            UUID uniqueId;
            JsonObject meta = packet.getMeta(uniqueId = iterator.next());
            this.listener.onPlayerDiscovered(uniqueId, meta != null ? meta : new JsonObject());
        }
    }

    @Override
    public void handlePlayerMetaUpdate(PlayerUpdateMetaPacket packet) {
        this.listener.onUpdateMeta(packet.getUniqueId(), packet.getMeta());
    }

    @Override
    public void handleChannelShow(ChannelShowPacket packet) {
        for (ChannelShowPacket.ChannelEntry channel : packet.getChannels()) {
            this.listener.onChannelShow(channel.getChannelId(), channel.getProperties());
        }
    }

    @Override
    public void handleChannelUpdate(ChannelUpdatePacket packet) {
        this.listener.onChannelUpdate(packet.getChannelId(), packet.getProperties());
    }

    @Override
    public void handleChannelHide(ChannelHidePacket packet) {
        for (UUID channelId : packet.getChannels()) {
            this.listener.onChannelHide(channelId);
        }
    }

    @Override
    public void handleChannelReset(ChannelResetPacket packet) {
        this.listener.onChannelReset();
    }

    @Override
    public void handleUserShow(UserShowPacket packet) {
        for (UserShowPacket.UserEntry user : packet.getUsers()) {
            this.listener.onUserShow(user.getUserId(), user.getChannelId(), user.getProperties());
        }
    }

    @Override
    public void handleUserSwitchChannel(UserSwitchChannelPacket packet) {
        this.listener.onUserSwitchChannel(packet.getUserId(), packet.getPreviousChannelId(), packet.getNewChannelId(), packet.getProperties());
    }

    @Override
    public void handleUserUpdateProperties(UserUpdatePacket packet) {
        this.listener.onChannelUserUpdateProperties(packet.getUserId(), packet.getProperties());
    }

    @Override
    public void handleUserHide(UserHidePacket packet) {
        for (UserHidePacket.UserEntry user : packet.getUsers()) {
            this.listener.onUserHide(user.getUserId(), user.getChannelId());
        }
    }

    @Override
    public void handleChannelAlert(ChannelAlertPacket packet) {
        this.listener.onChannelAlert(packet.getMessageId(), packet.getArguments());
    }

    @Override
    public void handlePlayerDead(PlayerDeadPacket packet) {
        for (UUID uniqueId : packet.getUniqueIds()) {
            this.listener.onPlayerDisappeared(uniqueId);
        }
    }

    @Override
    public void handleServerAudio(ServerAudioPacket packet) {
        this.listener.onAudioReceived(packet.getUniqueId(), packet.getData());
    }

    @Override
    public void handleInvalidKey(InvalidKeyPacket packet) {
        this.udpClient.stop(DisconnectType.AUTHENTICATION_FAILED);
        this.listener.onDisconnected(DisconnectType.AUTHENTICATION_FAILED, "Invalid encryption");
    }

    @Override
    public void handleKeepAlive(KeepAlivePacket packet) {
    }

    public void requestMetaUpdate(UUID uniqueId) {
        this.udpClient.sendPacket(new RequestPlayerMetaPacket(uniqueId));
    }

    public void sendDisconnect(String reason) {
        if (this.udpClient != null) {
            this.udpClient.sendPacket(new DisconnectPacket(reason));
        }
    }

    public void sendVisiblePlayers(UUID ... visible) {
        HashSet<UUID> visibleSet = new HashSet<UUID>(Arrays.asList(visible));
        ArrayList<UUID> added = new ArrayList<UUID>();
        ArrayList<UUID> removed = new ArrayList<UUID>();
        for (UUID uniqueId : visible) {
            if (this.remoteVisiblePlayers.contains(uniqueId)) continue;
            added.add(uniqueId);
        }
        for (UUID uniqueId : this.remoteVisiblePlayers) {
            if (visibleSet.contains(uniqueId)) continue;
            removed.add(uniqueId);
        }
        int totalChanges = added.size() + removed.size();
        if (totalChanges == 0) {
            return;
        }
        if (totalChanges > 10) {
            this.remoteVisiblePlayers.clear();
            this.remoteVisiblePlayers.addAll(visibleSet);
            UpdateVisiblePlayersPacket packet = new UpdateVisiblePlayersPacket();
            packet.setAction(UpdateVisiblePlayersPacket.Action.UPDATE);
            packet.getUniqueIds().addAll(visibleSet);
            this.udpClient.sendPacket(packet);
        } else {
            Object packet;
            this.remoteVisiblePlayers.addAll(added);
            for (UUID uniqueId : removed) {
                this.remoteVisiblePlayers.remove(uniqueId);
            }
            if (!added.isEmpty()) {
                packet = new UpdateVisiblePlayersPacket();
                ((UpdateVisiblePlayersPacket)packet).setAction(UpdateVisiblePlayersPacket.Action.ADD);
                ((UpdateVisiblePlayersPacket)packet).getUniqueIds().addAll(added);
                this.udpClient.sendPacket((VoicePacket<?>)packet);
            }
            if (!removed.isEmpty()) {
                packet = new UpdateVisiblePlayersPacket();
                ((UpdateVisiblePlayersPacket)packet).setAction(UpdateVisiblePlayersPacket.Action.REMOVE);
                ((UpdateVisiblePlayersPacket)packet).getUniqueIds().addAll(removed);
                this.udpClient.sendPacket((VoicePacket<?>)packet);
            }
        }
    }

    public void sendUnmutePlayer(UUID uniqueId) {
        UnmutePlayerPacket packet = new UnmutePlayerPacket();
        packet.setUniqueId(uniqueId);
        this.udpClient.sendPacket(packet);
    }

    public void sendMutePlayer(UUID uniqueId, String reason, long duration, int relatedReportId) {
        MutePlayerPacket mutePlayer = new MutePlayerPacket();
        mutePlayer.setUniqueId(uniqueId);
        mutePlayer.setReason(reason);
        mutePlayer.setDuration(duration);
        mutePlayer.setRelatedReportId(relatedReportId);
        this.udpClient.sendPacket(mutePlayer);
    }

    public void sendReportPlayer(UUID uniqueId, String reason) {
        ReportPlayerPacket reportPlayer = new ReportPlayerPacket();
        reportPlayer.setUniqueId(uniqueId);
        reportPlayer.setReason(reason);
        this.udpClient.sendPacket(reportPlayer);
    }

    public void sendWarnPlayer(UUID uniqueId, String reason) {
        WarnPlayerPacket packet = new WarnPlayerPacket();
        packet.setUniqueId(uniqueId);
        packet.setReason(reason);
        this.udpClient.sendPacket(packet);
    }

    public void sendAddNotePlayer(UUID uniqueId, String note) {
        this.udpClient.sendPacket(UpdateNotePlayerPacket.add(uniqueId, note));
    }

    public void sendUpdateNotePlayer(UUID uniqueId, int noteId, String note) {
        this.udpClient.sendPacket(UpdateNotePlayerPacket.update(uniqueId, noteId, note));
    }

    public void sendRemoveNotePlayer(UUID uniqueId, int noteId) {
        this.udpClient.sendPacket(UpdateNotePlayerPacket.remove(uniqueId, noteId));
    }

    public void sendSwitchServer(String server, int port) {
        SwitchServerPacket switchServer = new SwitchServerPacket();
        switchServer.setServer(server);
        switchServer.setPort(port);
        this.udpClient.sendPacket(switchServer);
    }

    public void sendAudioChunk(byte[] data) {
        ClientAudioPacket voiceContainer = new ClientAudioPacket();
        voiceContainer.setData(data);
        this.udpClient.sendPacket(voiceContainer);
    }

    public void sendSubscribeChannels() {
        this.udpClient.sendPacket(new SubscribeChannelsPacket());
    }

    public void sendRequestChannelReset(boolean critical, String reason) {
        this.udpClient.sendPacket(new RequestChannelResetPacket(critical, reason));
    }

    public void sendUpdateProperties(UserProperties properties) {
        this.udpClient.sendPacket(new UpdatePropertiesPacket(properties));
    }

    public void sendMovePlayerToChannel(UUID userId, UUID channelId) {
        this.sendMovePlayerToChannel(userId, channelId, null);
    }

    public void sendMovePlayerToChannel(UUID userId, UUID channelId, String password) {
        this.udpClient.sendPacket(new MoveUserToChannelPacket(userId, channelId, password));
    }

    public void sendCreateChannel(ChannelProperties properties) {
        this.udpClient.sendPacket(new CreateChannelPacket(properties));
    }

    public void sendUpdateChannel(UUID channelId, ChannelProperties properties) {
        this.udpClient.sendPacket(new UpdateChannelPacket(channelId, properties));
    }

    public void sendDeleteChannel(UUID channelId) {
        this.udpClient.sendPacket(new DeleteChannelPacket(channelId));
    }

    public void sendPacket(VoicePacket<?> packet) {
        this.udpClient.sendPacket(packet);
    }

    public void setAuthenticator(AuthenticationMethod method, Authenticator authenticator) {
        this.authenticators.put(method, authenticator);
    }

    public boolean isConnected() {
        return this.udpClient != null && this.udpClient.isRunning();
    }

    public void stop(String reason) {
        this.sendDisconnect(reason);
        if (this.udpClient != null) {
            this.udpClient.stop(DisconnectType.DISCONNECT);
        }
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setListener(VoiceClientListener listener) {
        this.listener = listener;
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }

    public boolean isAuthenticated() {
        return this.udpClient.getState() == ConnectionState.CONNECTED;
    }
}
