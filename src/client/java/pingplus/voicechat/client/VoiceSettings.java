package pingplus.voicechat.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public final class VoiceSettings {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("laby-voicechat.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public volatile boolean enabled = false;
    public volatile boolean muted = false;
    public volatile boolean deafened = false;
    public volatile double volume = 1;
    public volatile double microphoneGain = 1;
    public volatile boolean noiseGateEnabled = false;
    public volatile boolean noiseSuppressionEnabled = true;
    public volatile VoiceActivation activation = VoiceActivation.PUSH_TO_TALK;
    public volatile double microphoneThresholdDb = -45;
    public volatile double distance = 32;
    public volatile String inputDevice = "";
    public volatile String outputDevice = "";
    public Map<String, Double> playerVolumes = new ConcurrentHashMap<>();
    public static VoiceSettings load() {
        try {
            if (Files.exists(FILE)) {
                VoiceSettings s = GSON.fromJson(Files.readString(FILE), VoiceSettings.class);
                if (s != null) {
                    if (s.inputDevice == null) s.inputDevice = "";
                    if (s.outputDevice == null) s.outputDevice = "";
                    if (s.activation == null) s.activation = VoiceActivation.PUSH_TO_TALK;
                    s.volume = clamp(s.volume, 2); s.microphoneGain = clamp(s.microphoneGain, 2);
                    s.microphoneThresholdDb = Double.isFinite(s.microphoneThresholdDb)
                        ? Math.max(-60, Math.min(-15, s.microphoneThresholdDb)) : -45;
                    s.distance = Math.max(8, clamp(s.distance, 64));
                    s.playerVolumes = s.playerVolumes == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(s.playerVolumes);
                    s.playerVolumes.replaceAll((key, value) -> value == null ? 1 : clamp(value, 2));
                    return s;
                }
            }
        } catch (Exception e) { VoicechatClient.LOG.warn("Cannot load voice settings", e); }
        return new VoiceSettings();
    }
    private static double clamp(double value, double max) { return Double.isFinite(value) ? Math.max(0, Math.min(max, value)) : 1; }
    public double volume(UUID player) { return playerVolumes.getOrDefault(player.toString(), 1.0); }
    public synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { VoicechatClient.LOG.warn("Cannot save voice settings", e); }
    }
}
