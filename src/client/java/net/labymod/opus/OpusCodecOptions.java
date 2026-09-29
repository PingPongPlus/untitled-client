/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.opus;

public class OpusCodecOptions {
    private final int frameSize;
    private final int sampleRate;
    private final int channels;
    private final int bitrate;
    private final int maxFrameSize;
    private final int maxPacketSize;

    private OpusCodecOptions(int frameSize, int sampleRate, int channels, int bitrate, int maxFrameSize, int maxPacketSize) {
        this.frameSize = frameSize;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitrate = bitrate;
        this.maxFrameSize = maxFrameSize;
        this.maxPacketSize = maxPacketSize;
    }

    public int getFrameSize() {
        return this.frameSize;
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public int getChannels() {
        return this.channels;
    }

    public int getBitrate() {
        return this.bitrate;
    }

    public int getMaxFrameSize() {
        return this.maxFrameSize;
    }

    public int getMaxPacketSize() {
        return this.maxPacketSize;
    }

    protected static OpusCodecOptions of(int frameSize, int sampleRate, int channels, int bitrate, int maxFrameSize, int maxPacketSize) {
        return new OpusCodecOptions(frameSize, sampleRate, channels, bitrate, maxFrameSize, maxPacketSize);
    }
}
