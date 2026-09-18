package pingplus.voicechat.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
    VoiceDevicesScreen(Screen parent, VoiceConnection voice) {
        super(Component.literal("Voice audio devices"));
        this.parent = parent; this.voice = voice;
        input = VoiceDevices.normalizeInput(voice.settings.inputDevice); output = voice.settings.outputDevice;
    }
    @Override protected void init() {
        inputs = VoiceDevices.available(true); outputs = VoiceDevices.available(false);
        int x = width / 2 - 150;
        addRenderableWidget(Button.builder(label("Input", inputs, input), b -> {
            stopTest();
            input = next(inputs, input); rebuildWidgets();
        }).bounds(x, 64, 300, 20).tooltip(Tooltip.create(Component.literal(name(inputs, input) + " — click to cycle microphones"))).build());
        addRenderableWidget(Button.builder(label("Output", outputs, output), b -> {
            output = next(outputs, output); rebuildWidgets();
        }).bounds(x, 100, 300, 20).tooltip(Tooltip.create(Component.literal(name(outputs, output) + " — click to cycle speakers"))).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh devices"), b -> { stopTest(); rebuildWidgets(); }).bounds(x, 132, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal(inputTest == null ? "Test microphone" : "Stop test"), b -> {
            if (inputTest == null) {
                voice.suspendAudio(); audioSuspended = true;
                inputTest = new VoiceInputTest(input);
            } else stopTest();
            rebuildWidgets();
        }).bounds(x + 155, 132, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(x, height - 28, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            voice.settings.inputDevice = input; voice.settings.outputDevice = output;
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
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.centeredText(font, "Click a device to cycle through available choices", width / 2, 36, 0xFFBBBBBB);
        if (inputTest != null) {
            graphics.fill(width / 2 - 150, 158, width / 2 + 150, 164, 0xFF333333);
            graphics.fill(width / 2 - 150, 158, width / 2 - 150 + (int)(300 * Math.sqrt(inputTest.peak())), 164, 0xFF80DD99);
            graphics.textWithWordWrap(font, Component.literal(inputTest.status()), width / 2 - 150, 170, 300, 0xFFFFFFFF);
        } else {
            graphics.centeredText(font, "Test input locally without transmitting", width / 2, 160, 0xFFBBBBBB);
            graphics.centeredText(font, "Apply switches devices without reconnecting voice", width / 2, 176, 0xFFBBBBBB);
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
