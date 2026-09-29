package net.labymod.addons.voicechat.core.audio.rnnoise;

/** JNI class name, pointer field and native signatures must match the bundled library. */
public final class RNNoiseFilter implements AutoCloseable {
    private long pointer;

    public RNNoiseFilter() {
        pointer = create0();
        if (pointer == 0) throw new IllegalStateException("RNNoise allocation failed");
    }

    public synchronized void apply(short[] samples) {
        if (isClosed()) throw new IllegalStateException("RNNoise filter is closed");
        if (samples.length != 480) throw new IllegalArgumentException("RNNoise needs a 10 ms / 480 sample frame");
        short[] processed = process0(samples);
        if (processed == null || processed.length != samples.length) {
            throw new IllegalStateException("Invalid RNNoise output");
        }
        System.arraycopy(processed, 0, samples, 0, samples.length);
    }

    @Override public synchronized void close() {
        if (pointer != 0) { destroy0(); pointer = 0; }
    }

    public synchronized boolean isClosed() { return pointer == 0; }
    protected static native long create0();
    protected native short[] process0(short[] samples);
    protected native long destroy0();
}
