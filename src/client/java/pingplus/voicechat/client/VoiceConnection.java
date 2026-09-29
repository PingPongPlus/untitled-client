package pingplus.voicechat.client;

import com.google.gson.JsonObject;
import net.labymod.voice.client.VoiceClient;
import net.labymod.voice.client.auth.AuthenticationResponse;
import net.labymod.voice.client.listener.VoiceClientListenerAdapter;
import net.labymod.voice.protocol.type.*;
import net.labymod.voice.protocol.util.properties.UserProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class VoiceConnection {
    private final Minecraft minecraft;
    public final VoiceSettings settings;
    private volatile VoiceClient client;
    private volatile VoiceAudio audio;
    private volatile boolean authenticated;
    public volatile boolean pushToTalk;
    public volatile String status = "Voice chat disabled";
    public final Map<UUID, String> players = new ConcurrentHashMap<>();
    public final Map<UUID, Long> talking = new ConcurrentHashMap<>();
    private long connectingSince;
    private int ticks;
    private String server = "";
    private int audioGeneration;
    private final AtomicLong audioSequence = new AtomicLong();
    public VoiceConnection(Minecraft minecraft, VoiceSettings settings) { this.minecraft = minecraft; this.settings = settings; }
    public boolean connected() { return authenticated; }
    public VoiceStatus statusFor(UUID id) {
        if (id.equals(minecraft.getUser().getProfileId())) {
            return !authenticated ? VoiceStatus.NOT_CONNECTED
                    : transmitting() ? VoiceStatus.SPEAKING : VoiceStatus.CONNECTED;
        }
        return VoiceStatus.resolve(authenticated && players.containsKey(id),
                talking.get(id), System.currentTimeMillis());
    }
    public boolean transmitting() { VoiceAudio device = audio; return device != null && device.transmitting(); }
    public double inputPeak() { VoiceAudio device = audio; return device == null ? 0 : device.inputPeak(); }
    public void connect() {
        disconnect();
        if (minecraft.level == null || minecraft.getCurrentServer() == null) { status = "Join a multiplayer server first"; return; }
        settings.enabled = true; settings.save();
        var session = minecraft.getUser();
        var service = minecraft.services().sessionService();
        VoiceClient attempt = new VoiceClient(); client = attempt;
        status = "Connecting to LabyMod voice..."; connectingSince = System.currentTimeMillis();
        attempt.setAuthenticator(AuthenticationMethod.MOJANG, hash -> {
            if (client != attempt) return null;
            try {
                service.joinServer(session.getProfileId(), session.getAccessToken(), hash);
                return client == attempt ? AuthenticationResponse.createMojang(session.getName()) : null;
            } catch (Exception e) {
                if (client == attempt) status = "Minecraft session authentication failed; sign in again";
                VoicechatClient.LOG.warn("Voice session authentication failed ({})", e.getClass().getSimpleName());
                return null;
            }
        });
        attempt.setListener(new VoiceClientListenerAdapter() {
            @Override public void onAuthenticated(boolean staff) {
                minecraft.execute(() -> {
                    if (client != attempt) { attempt.stop("Cancelled"); return; }
                    authenticated = true; status = "Connected to LabyMod voice"; server = "";
                    updateProperties();
                    audioSequence.set(0);
                    restartAudio();
                });
            }
            @Override public void onDisconnected(DisconnectType type, String reason) {
                minecraft.execute(() -> { if (client == attempt) { disconnect(); status = "Disconnected: " + type; } });
            }
            @Override public void onPlayerDiscovered(UUID id, JsonObject meta) {
                minecraft.execute(() -> {
                    if (client == attempt) players.putIfAbsent(id, id.toString().substring(0, 8));
                });
            }
            @Override public void onPlayerDisappeared(UUID id) {
                minecraft.execute(() -> {
                    if (client == attempt) { players.remove(id); talking.remove(id); }
                });
            }
            @Override public void onAudioReceived(UUID id, byte[] data) {
                if (client != attempt || !authenticated || VoiceFrame.decode(data) == null) return;
                long receivedAt = System.currentTimeMillis();
                minecraft.execute(() -> {
                    if (client == attempt && authenticated && players.containsKey(id)) {
                        talking.put(id, receivedAt);
                    }
                });
                VoiceAudio device = audio;
                if (client == attempt && authenticated && device != null) device.receive(id, data);
            }
            @Override public void onWarn(String reason) { if (client == attempt) status = "Voice server: " + reason; }
        });
        Thread.ofPlatform().daemon().name("Voice-Resolve").start(() -> {
            InetSocketAddress address = new InetSocketAddress("voice.labymod.net", 8066);
            if (client == attempt) attempt.connect(address, AuthenticationMethod.MOJANG);
        });
    }
    public void disconnect() {
        VoiceClient previous = client; client = null; authenticated = false; pushToTalk = false;
        suspendAudio();
        if (previous != null) previous.stop("Leaving voice chat");
        players.clear(); talking.clear(); server = ""; status = "Disconnected";
    }
    public void suspendAudio() {
        audioGeneration++;
        VoiceAudio previous = audio; audio = null;
        if (previous != null) previous.close();
    }
    /** Device changes keep the authenticated UDP session and its sequence counter. */
    public void restartAudio() {
        suspendAudio();
        VoiceClient current = client;
        if (!authenticated || current == null) return;
        int generation = audioGeneration;
        status = "Connected to LabyMod voice";
        VoiceAudio device = new VoiceAudio(settings, () -> authenticated && client == current && settings.activation.permitsInput(pushToTalk),
            data -> { if (client == current && authenticated) current.sendAudioChunk(data); },
            message -> minecraft.execute(() -> { if (client == current && audioGeneration == generation) status = message; }),
            audioSequence::getAndIncrement);
        audio = device;
        try { device.start(); }
        catch (Exception | LinkageError e) {
            VoicechatClient.LOG.warn("Voice audio initialization failed", e);
            status = "Opus unavailable: " + e.getClass().getSimpleName(); device.close();
        }
    }
    public void updateProperties() {
        VoiceClient current = client;
        if (!authenticated || current == null) return;
        UserProperties properties = new UserProperties();
        properties.setInputMuted(settings.muted || settings.deafened); properties.setOutputMuted(settings.deafened);
        current.sendUpdateProperties(properties);
    }
    public void tick() {
        if (client != null && !authenticated && System.currentTimeMillis() - connectingSince > 30000) {
            String previousStatus = status; disconnect();
            status = previousStatus.startsWith("Connecting") ? "Connection timed out; press Reconnect to retry" : previousStatus;
        }
        if (!authenticated || client == null || minecraft.level == null || minecraft.player == null || ++ticks % 10 != 0) return;
        var currentServer = minecraft.getCurrentServer();
        if (currentServer == null) return;
        if (!server.equals(currentServer.ip)) {
            server = currentServer.ip; ServerAddress address = ServerAddress.parseString(server);
            client.sendSwitchServer(address.getHost(), address.getPort());
        }
        Map<UUID, Double> positions = new HashMap<>();
        for (var player : minecraft.level.players()) {
            if (player == minecraft.player) continue;
            UUID id = player.getUUID();
            double distance = minecraft.player.distanceTo(player);
            positions.put(id, Math.max(0, 1 - distance / settings.distance));
            if (players.containsKey(id)) players.put(id, player.getName().getString());
        }
        client.sendVisiblePlayers(positions.keySet().toArray(UUID[]::new));
        VoiceAudio device = audio; if (device != null) device.positions(positions);
        talking.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue() > 2000);
    }
}
