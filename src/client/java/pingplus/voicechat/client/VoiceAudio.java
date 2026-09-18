package pingplus.voicechat.client;

import net.labymod.opus.OpusCodec;
import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** 48 kHz mono, 20 ms Opus frames, matching the supplied LabyMod runtime. */
public final class VoiceAudio implements AutoCloseable {
    private static final AudioFormat FORMAT = VoiceDevices.FORMAT;
    private final String inputDevice;
    private final String outputDevice;
    private final VoiceSettings settings;
    private final BooleanSupplier transmit;
    private final Consumer<byte[]> sender;
    private final Consumer<String> error;
    private final LongSupplier sequence;
    private final Map<UUID, Speaker> speakers = new HashMap<>();
    private final BlockingQueue<Frame> incoming = new ArrayBlockingQueue<>(100);
    private volatile boolean running;
    private volatile VoiceCapture microphone;
    private volatile double inputPeak;
    private volatile SourceDataLine output;
    private volatile long lastTransmission;
    private volatile Map<UUID, Double> attenuation = Map.of();
    private static boolean nativeLoaded;
    public VoiceAudio(VoiceSettings settings, BooleanSupplier transmit, Consumer<byte[]> sender, Consumer<String> error, LongSupplier sequence) {
        this.settings = settings; this.transmit = transmit; this.sender = sender; this.error = error;
        inputDevice = settings.inputDevice; outputDevice = settings.outputDevice;
        this.sequence = sequence;
    }
    private static synchronized void loadNative() throws Exception {
        if (!nativeLoaded) { OpusCodec.setupWithTemporaryFolder(); nativeLoaded = true; }
    }
    public void start() throws Exception {
        loadNative(); running = true;
        Thread.ofPlatform().daemon().name("Voice-Capture").start(this::capture);
        Thread.ofPlatform().daemon().name("Voice-Playback").start(this::playback);
    }
    public void positions(Map<UUID, Double> values) { attenuation = Map.copyOf(values); }
    public void receive(UUID id, byte[] data) {
        if (!running || settings.deafened) return;
        VoiceFrame frame = VoiceFrame.decode(data);
        if (frame != null) incoming.offer(new Frame(id, frame));
    }
    public boolean transmitting() { return running && System.nanoTime() - lastTransmission < TimeUnit.MILLISECONDS.toNanos(200); }
    public double inputPeak() { return inputPeak; }
    private static OpusCodec codec() { return OpusCodec.newBuilder().withBitrate(48000).build(); }
    private void capture() {
        OpusCodec encoder = codec();
        try (VoiceCapture line = openMicrophone()) {
            if (line == null) return;
            byte[] pcm = new byte[1920];
            while (running) {
                int read = line.read(pcm);
                if (!running) break;
                inputPeak = VoiceInputTest.peak(pcm, read);
                if (read == pcm.length && transmit.getAsBoolean() && !settings.muted && !settings.deafened) {
                    for (int i = 0; i < pcm.length; i += 2) {
                        int sample = (short)((pcm[i] & 255) | (pcm[i + 1] << 8));
                        int scaled = clip(sample * settings.microphoneGain);
                        pcm[i] = (byte)scaled; pcm[i + 1] = (byte)(scaled >> 8);
                    }
                    sender.accept(VoiceFrame.encode(sequence.getAsLong(), encoder.encodeFrame(pcm)));
                    lastTransmission = System.nanoTime();
                }
            }
        } catch (Exception | LinkageError e) {
            if (running) { VoicechatClient.LOG.warn("Voice microphone could not be opened or read", e); error.accept("Microphone unavailable: " + e.getMessage()); }
        }
        finally { microphone = null; inputPeak = 0; encoder.destroy(); }
    }
    private synchronized VoiceCapture openMicrophone() throws java.io.IOException {
        if (!running) return null;
        microphone = VoiceCapture.open(inputDevice);
        return microphone;
    }
    private void playback() {
        try (SourceDataLine line = (SourceDataLine) VoiceDevices.openLine(false, outputDevice)) {
            synchronized (this) {
                if (!running) return;
                output = line; line.open(FORMAT, 7680); line.start();
            }
            byte[] mixed = new byte[1920];
            while (running) {
                Frame frame;
                while ((frame = incoming.poll()) != null) {
                    if (!attenuation.containsKey(frame.id) || settings.volume(frame.id) == 0 || settings.deafened) continue;
                    Speaker speaker = speakers.get(frame.id);
                    if (speaker == null && speakers.size() < 128) { speaker = new Speaker(); speakers.put(frame.id, speaker); }
                    if (speaker == null) continue;
                    try {
                        // A sender can restart its microphone and reset the sequence counter.
                        if (System.nanoTime() - speaker.last > TimeUnit.SECONDS.toNanos(1)) {
                            speaker.frames.clear(); speaker.lastSequence = -1;
                        }
                        if (frame.data.sequence <= speaker.lastSequence) continue;
                        speaker.frames.put(frame.data.sequence, frame.data.opus);
                        while (speaker.frames.size() > 5) speaker.frames.pollFirstEntry();
                        speaker.last = System.nanoTime();
                    } catch (RuntimeException e) { VoicechatClient.LOG.debug("Discarded invalid audio frame", e); }
                }
                int[] sum = new int[960];
                Iterator<Map.Entry<UUID, Speaker>> iterator = speakers.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next(); Speaker speaker = entry.getValue();
                    if (System.nanoTime() - speaker.last > TimeUnit.SECONDS.toNanos(5)) { speaker.decoder.destroy(); iterator.remove(); continue; }
                    var next = speaker.frames.pollFirstEntry();
                    if (next == null) continue;
                    speaker.lastSequence = next.getKey();
                    byte[] pcm;
                    try { pcm = speaker.decoder.decodeFrame(next.getValue()); }
                    catch (RuntimeException e) { VoicechatClient.LOG.debug("Discarded invalid Opus frame", e); continue; }
                    double gain = settings.deafened ? 0 : settings.volume * settings.volume(entry.getKey()) * attenuation.getOrDefault(entry.getKey(), 0.0);
                    for (int i = 0; i < Math.min(960, pcm.length / 2); i++) sum[i] += (int)((short)((pcm[i * 2] & 255) | pcm[i * 2 + 1] << 8) * gain);
                }
                for (int i = 0; i < sum.length; i++) { int sample = clip(sum[i]); mixed[i * 2] = (byte)sample; mixed[i * 2 + 1] = (byte)(sample >> 8); }
                line.write(mixed, 0, mixed.length);
            }
        } catch (Exception | LinkageError e) {
            if (running) { VoicechatClient.LOG.warn("Voice output could not be opened or written", e); error.accept("Speakers unavailable: " + e.getMessage()); }
        }
        finally { output = null; speakers.values().forEach(s -> s.decoder.destroy()); speakers.clear(); }
    }
    static int clip(double value) { return (int)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value)); }
    @Override public synchronized void close() {
        running = false; incoming.clear();
        VoiceCapture in = microphone; if (in != null) in.close();
        SourceDataLine out = output; if (out != null) out.close();
    }
    private record Frame(UUID id, VoiceFrame data) {}
    private static final class Speaker {
        final OpusCodec decoder = codec();
        final NavigableMap<Long, byte[]> frames = new TreeMap<>();
        long last = System.nanoTime();
        long lastSequence = -1;
    }
}
