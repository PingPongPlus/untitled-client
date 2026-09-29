package pingplus.voicechat.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Device changes are staged until Apply, so Cancel leaves the active audio untouched. */
final class VoiceDevicesScreen extends Screen {
    private final Screen parent;
    private final VoiceConnection voice;
    private String input;
    private String output;
    private List<VoiceDevices.Device> inputs;
    private List<VoiceDevices.Device> outputs;
    private VoiceInputTest inputTest;
    private boolean audioSuspended;
    private volatile boolean noiseGateEnabled;
    private volatile boolean noiseSuppressionEnabled;
    private volatile double thresholdDb;
    VoiceDevicesScreen(Screen parent, VoiceConnection voice) {
        super(Component.literal("Voice audio devices"));
        this.parent = parent; this.voice = voice;
        input = VoiceDevices.normalizeInput(voice.settings.inputDevice); output = voice.settings.outputDevice;
        noiseGateEnabled = voice.settings.noiseGateEnabled;
        noiseSuppressionEnabled = voice.settings.noiseSuppressionEnabled;
        thresholdDb = voice.settings.microphoneThresholdDb;
    }
    @Override protected void init() {
        inputs = VoiceDevices.available(true); outputs = VoiceDevices.available(false);
        int x = width / 2 - 150;
        addRenderableWidget(Button.builder(label("Input", inputs, input), b -> {
            stopTest();
            input = next(inputs, input); rebuildWidgets();
        }).bounds(x, 44, 300, 20).tooltip(Tooltip.create(Component.literal(name(inputs, input) + " — click to cycle microphones"))).build());
        addRenderableWidget(Button.builder(label("Output", outputs, output), b -> {
            output = next(outputs, output); rebuildWidgets();
        }).bounds(x, 68, 300, 20).tooltip(Tooltip.create(Component.literal(name(outputs, output) + " — click to cycle speakers"))).build());
        addRenderableWidget(Button.builder(Component.literal("Noise gate: " + (noiseGateEnabled ? "On" : "Off")), b -> {
            noiseGateEnabled = !noiseGateEnabled; rebuildWidgets();
        }).bounds(x, 116, 300, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Noise suppression (RNNoise): " + (noiseSuppressionEnabled ? "On" : "Off")), b -> {
            noiseSuppressionEnabled = !noiseSuppressionEnabled; rebuildWidgets();
        }).bounds(x, 92, 300, 20).tooltip(Tooltip.create(Component.literal("Reduces background noise while speaking. Runs locally before the noise gate and Opus encoding."))).build());
        addRenderableWidget(new AbstractSliderButton(x, 140, 300, 20, Component.empty(), (thresholdDb + 60) / 45) {
            { updateMessage(); setTooltip(Tooltip.create(Component.literal("Raise the cutoff to block more background noise. Lower it if quiet speech is cut off. Use Test microphone to compare input against the marker."))); }
            @Override protected void updateMessage() { setMessage(Component.literal("Microphone cutoff: " + Math.round(-60 + value * 45) + " dB")); }
            @Override protected void applyValue() { thresholdDb = Math.round(-60 + value * 45); }
        });
        addRenderableWidget(Button.builder(Component.literal("Refresh devices"), b -> { stopTest(); rebuildWidgets(); }).bounds(x, 164, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal(inputTest == null ? "Test microphone" : "Stop test"), b -> {
            if (inputTest == null) {
                voice.suspendAudio(); audioSuspended = true;
                inputTest = new VoiceInputTest(input, () -> voice.settings.activation.usesGate(noiseGateEnabled), () -> thresholdDb, () -> noiseSuppressionEnabled);
            } else stopTest();
            rebuildWidgets();
        }).bounds(x + 155, 164, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(x, height - 28, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            voice.settings.inputDevice = input; voice.settings.outputDevice = output;
            voice.settings.noiseGateEnabled = noiseGateEnabled;
            voice.settings.noiseSuppressionEnabled = noiseSuppressionEnabled;
            voice.settings.microphoneThresholdDb = thresholdDb;
            voice.settings.save();
            stopTest();
            voice.restartAudio(); audioSuspended = false;
            onClose();
        }).bounds(x + 155, height - 28, 145, 20).build());
    }
    private Component label(String prefix, List<VoiceDevices.Device> choices, String selected) {
        return Component.literal(prefix + ": " + font.plainSubstrByWidth(name(choices, selected), 245));
    }
    private static String name(List<VoiceDevices.Device> choices, String selected) {
        return choices.stream().filter(d -> d.id().equals(selected)).map(VoiceDevices.Device::name).findFirst().orElse("Unavailable saved device");
    }
    private static String next(List<VoiceDevices.Device> choices, String selected) {
        for (int i = 0; i < choices.size(); i++) if (choices.get(i).id().equals(selected)) return choices.get((i + 1) % choices.size()).id();
        return "";
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(font, "Higher cutoff blocks more noise; Apply saves changes", width / 2, 28, 0xFFBBBBBB);
        if (inputTest != null) {
            int x = width / 2 - 150;
            int level = (int)(300 * Math.max(0, Math.min(1, (inputTest.levelDb() + 70) / 70)));
            graphics.fill(x, 190, x + 300, 196, 0xFF333333);
            graphics.fill(x, 190, x + level, 196, inputTest.gatePassing() ? 0xFF80DD99 : 0xFFFFCC80);
            if (voice.settings.activation.usesGate(noiseGateEnabled)) {
                int marker = x + (int)(300 * (thresholdDb + 70) / 70);
                graphics.fill(marker, 188, marker + 2, 198, 0xFFFFFFFF);
            }
            String gateState = !voice.settings.activation.usesGate(noiseGateEnabled) ? "Gate off" : inputTest.gatePassing() ? "Gate open" : "Noise blocked";
            graphics.centeredText(font, Math.round(inputTest.levelDb()) + " dB | " + gateState + " | local test", width / 2, 202, 0xFFFFFFFF);
            graphics.centeredText(font, font.plainSubstrByWidth(inputTest.status(), 300), width / 2, 216, 0xFFBBBBBB);
        } else {
            graphics.centeredText(font, "Test microphone: compare processed level to cutoff", width / 2, 194, 0xFFBBBBBB);
            graphics.centeredText(font, "RNNoise reduces noise during speech; gate blocks pauses", width / 2, 210, 0xFFBBBBBB);
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    private void stopTest() {
        if (inputTest != null) { inputTest.close(); inputTest = null; }
    }
    @Override public void removed() {
        stopTest();
        if (audioSuspended) { audioSuspended = false; voice.restartAudio(); }
        super.removed();
    }
}
