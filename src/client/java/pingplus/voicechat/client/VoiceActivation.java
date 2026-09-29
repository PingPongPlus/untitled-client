package pingplus.voicechat.client;

/** The three microphone activation modes supported by the original addon. */
public enum VoiceActivation {
    PUSH_TO_TALK("Push to talk"),
    VOICE_ACTIVATION("Voice activation"),
    CONTINUOUS("Continuous");

    private final String label;
    VoiceActivation(String label) { this.label = label; }
    public String label() { return label; }
    public VoiceActivation next() { return values()[(ordinal() + 1) % values().length]; }
    boolean permitsInput(boolean talkKeyHeld) { return this != PUSH_TO_TALK || talkKeyHeld; }
    boolean usesGate(boolean gateEnabled) { return this == VOICE_ACTIVATION || gateEnabled; }
}
