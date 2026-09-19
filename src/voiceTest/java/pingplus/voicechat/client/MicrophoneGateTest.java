package pingplus.voicechat.client;

final class MicrophoneGateTest {
    static void run() {
        MicrophoneGate gate = new MicrophoneGate();
        byte[] quiet = frame(50);
        require(!gate.process(quiet, true, -40), "Quiet background noise must be blocked");
        require(VoiceInputTest.peak(quiet, quiet.length) == 0, "Closed gate must produce silence");
        byte[] speech = frame(5000);
        require(gate.process(speech, true, -40), "Speech must open gate immediately");
        require(speech[0] != frame(5000)[0], "Opening must fade in");
        require(VoiceInputTest.peak(speech, speech.length) > 0.15, "Speech reaches full level within frame");
        for (int i = 0; i < 9; i++) require(gate.process(frame(50), true, -40), "Hold preserves short speech pauses");
        require(gate.process(frame(50), true, -40), "Closing fade is delivered");
        require(!gate.process(frame(50), true, -40), "Gate closes after hold and fade");
        byte[] bypass = frame(50);
        require(gate.process(bypass, false, -40) && java.util.Arrays.equals(bypass, frame(50)), "Disabled gate preserves PCM exactly");
        gate.reset();
        require(!gate.process(frame(1000), true, -20), "Raised cutoff blocks quiet speech");
        require(gate.process(frame(1000), true, -50), "Lowered cutoff admits quiet speech");
        gate.reset();
        require(!gate.process(frame(50), true, -40), "Mute/PTT reset must clear previous hold");
        require(MicrophoneGate.levelDb(new byte[1920], 1920) == -96, "Silence level is finite");
        require(Math.abs(MicrophoneGate.levelDb(frame(32767), 1920)) < 0.01, "Full scale is approximately 0 dBFS");
        System.out.println("PASS: microphone noise gate, threshold, bypass, hold, fades, reset, and level metering");
    }
    private static byte[] frame(int amplitude) {
        byte[] pcm = new byte[1920];
        for (int i = 0; i < 960; i++) {
            int sample = (i & 1) == 0 ? amplitude : -amplitude;
            pcm[i * 2] = (byte)sample; pcm[i * 2 + 1] = (byte)(sample >> 8);
        }
        return pcm;
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
