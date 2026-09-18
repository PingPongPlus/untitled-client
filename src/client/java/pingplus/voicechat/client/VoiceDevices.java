package pingplus.voicechat.client;

import javax.sound.sampled.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Selects Java Sound mixers by identity rather than their changing enumeration index. */
final class VoiceDevices {
    static final AudioFormat FORMAT = new AudioFormat(48000, 16, 1, true, false);
    record Device(String id, String name) {}
    static DataLine.Info lineInfo(boolean input) {
        return new DataLine.Info(input ? TargetDataLine.class : SourceDataLine.class, FORMAT);
    }
    static String id(Mixer.Info info) {
        String identity = String.join("\u0000", info.getName(), info.getVendor(), info.getVersion(), info.getDescription());
        return Base64.getEncoder().encodeToString(identity.getBytes(StandardCharsets.UTF_8));
    }
    static List<Device> available(boolean input) {
        List<Device> devices = new ArrayList<>();
        devices.add(new Device("", "System default"));
        if (input) {
            for (String name : VoiceCapture.names()) devices.add(new Device("openal:" + name, name));
            return List.copyOf(devices);
        }
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            try {
                if (AudioSystem.getMixer(info).isLineSupported(lineInfo(input))) devices.add(new Device(id(info), info.getName()));
            } catch (IllegalArgumentException | SecurityException ignored) {
                // A device may disappear while the settings screen is enumerating it.
            }
        }
        return List.copyOf(devices);
    }
    /** Migrate saved Java Sound identities only when the same named device is unambiguous. */
    static String normalizeInput(String selected) {
        if (selected == null || selected.isEmpty()) return "";
        if (selected.startsWith("openal:")) return selected;
        try {
            String identity = new String(Base64.getDecoder().decode(selected), StandardCharsets.UTF_8);
            int separator = identity.indexOf('\u0000');
            if (separator < 0) return selected;
            String name = identity.substring(0, separator);
            List<String> matches = VoiceCapture.names().stream()
                .filter(candidate -> candidate.equals(name) || candidate.equals("OpenAL Soft on " + name)).toList();
            if (matches.size() == 1) return "openal:" + matches.getFirst();
        } catch (IllegalArgumentException ignored) { }
        return selected;
    }
    static DataLine openLine(boolean input, String selected) throws LineUnavailableException {
        DataLine.Info line = lineInfo(input);
        if (selected == null || selected.isEmpty()) return (DataLine) AudioSystem.getLine(line);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (!id(info).equals(selected)) continue;
            Mixer mixer = AudioSystem.getMixer(info);
            if (!mixer.isLineSupported(line)) throw new LineUnavailableException("Selected device does not support voice audio");
            return (DataLine) mixer.getLine(line);
        }
        throw new LineUnavailableException("Selected device disconnected; select another device in Audio devices");
    }
}
