/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.opus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.NoSuchElementException;
import net.labymod.opus.OpusCodecOptions;

public class OpusCodec {
    private final OpusCodecOptions opusOptions;
    private boolean encoderInitialized = false;
    private boolean decoderInitialized = false;
    private long encoderState;
    private long decoderState;

    private OpusCodec(OpusCodecOptions opusOptions) {
        this.opusOptions = opusOptions;
    }

    public int getFrameSize() {
        return this.opusOptions.getFrameSize();
    }

    public int getSampleRate() {
        return this.opusOptions.getSampleRate();
    }

    public int getChannels() {
        return this.opusOptions.getChannels();
    }

    public int getBitrate() {
        return this.opusOptions.getBitrate();
    }

    public int getMaxFrameSize() {
        return this.opusOptions.getMaxFrameSize();
    }

    public int getMaxPacketSize() {
        return this.opusOptions.getMaxPacketSize();
    }

    public static OpusCodec createDefault() {
        return OpusCodec.newBuilder().build();
    }

    public static OpusCodec createByOptions(OpusCodecOptions opusCodecOptions) {
        return new OpusCodec(opusCodecOptions);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public byte[] encodeFrame(byte[] bytes) {
        return this.encodeFrame(bytes, 0, bytes.length);
    }

    public byte[] encodeFrame(byte[] bytes, int offset, int length) {
        if (length != this.getChannels() * this.getFrameSize() * 2) {
            throw new IllegalArgumentException(String.format("data length must be == CHANNELS * FRAMESIZE * 2 (%d bytes) but is %d bytes", this.getChannels() * this.getFrameSize() * 2, bytes.length));
        }
        this.ensureEncoderExistence();
        return this.encodeFrame(this.encoderState, bytes, offset, length);
    }

    private native byte[] encodeFrame(long var1, byte[] var3, int var4, int var5);

    public byte[] decodeFrame(byte[] bytes) {
        this.ensureDecoderExistence();
        byte[] decoded = this.decodeFrame(this.decoderState, bytes);
        int expectedLength = this.getChannels() * this.getFrameSize() * 2;
        if (decoded.length != expectedLength) {
            byte[] padded = new byte[expectedLength];
            System.arraycopy(decoded, 0, padded, 0, Math.min(decoded.length, expectedLength));
            return padded;
        }
        return decoded;
    }

    private native byte[] decodeFrame(long var1, byte[] var3);

    private void ensureEncoderExistence() {
        if (this.encoderInitialized) {
            return;
        }
        this.encoderState = this.createEncoder(this.opusOptions);
        this.encoderInitialized = true;
    }

    private native long createEncoder(OpusCodecOptions var1);

    private void ensureDecoderExistence() {
        if (this.decoderInitialized) {
            return;
        }
        this.decoderState = this.createDecoder(this.opusOptions);
        this.decoderInitialized = true;
    }

    private native long createDecoder(OpusCodecOptions var1);

    public void destroy() {
        if (this.encoderInitialized) {
            this.destroyEncoder(this.encoderState);
        }
        if (this.decoderInitialized) {
            this.destroyDecoder(this.decoderState);
        }
        this.encoderInitialized = false;
        this.decoderInitialized = false;
    }

    private native void destroyEncoder(long var1);

    private native void destroyDecoder(long var1);

    private static String getNativeLibraryName(boolean allowArm) {
        String library;
        String arch;
        String bitnessArch = System.getProperty("os.arch").toLowerCase();
        String bitnessDataModel = System.getProperty("sun.arch.data.model", null);
        boolean is64bit = bitnessArch.contains("64") || bitnessDataModel != null && bitnessDataModel.contains("64");
        String string = arch = bitnessArch.startsWith("aarch") && allowArm ? "arm" : "";
        if (is64bit) {
            String library64 = OpusCodec.processLibraryName("opus-jni-native-" + arch + "64");
            if (OpusCodec.hasResource("/native-binaries/" + library64)) {
                return library64;
            }
        } else {
            String library32 = OpusCodec.processLibraryName("opus-jni-native-" + arch + "32");
            if (OpusCodec.hasResource("/native-binaries/" + library32)) {
                return library32;
            }
        }
        if (!OpusCodec.hasResource("/native-binaries/" + (library = OpusCodec.processLibraryName("opus-jni-native")))) {
            throw new NoSuchElementException("No binary for the current system found, even after trying bit neutral names");
        }
        return library;
    }

    private static String processLibraryName(String library) {
        String systemName = System.getProperty("os.name", "bare-metal?").toLowerCase();
        if (systemName.contains("nux") || systemName.contains("nix")) {
            return "lib" + library + ".so";
        }
        if (systemName.contains("mac")) {
            return "lib" + library + ".dylib";
        }
        if (systemName.contains("windows")) {
            return library + ".dll";
        }
        throw new NoSuchElementException("No native library for system " + systemName);
    }

    private static boolean hasResource(String resource) {
        return OpusCodec.class.getResource(resource) != null;
    }

    public static void loadNative(File directory) throws IOException {
        OpusCodec.loadNative(directory, true);
    }

    public static void loadNative(File directory, boolean allowArm) throws IOException {
        String nativeLibraryName = OpusCodec.getNativeLibraryName(allowArm);
        InputStream source = OpusCodec.class.getResourceAsStream("/native-binaries/" + nativeLibraryName);
        if (source == null) {
            throw new IOException("Could not find native library " + nativeLibraryName);
        }
        Path destination = directory.toPath().resolve(nativeLibraryName);
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AccessDeniedException accessDeniedException) {
            // empty catch block
        }
        System.load(new File(directory, nativeLibraryName).getAbsolutePath());
    }

    public static void setupWithTemporaryFolder() throws IOException {
        File temporaryDir = Files.createTempDirectory("opus-jni", new FileAttribute[0]).toFile();
        temporaryDir.deleteOnExit();
        try {
            OpusCodec.loadNative(temporaryDir);
        }
        catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            OpusCodec.loadNative(temporaryDir, false);
        }
    }

    public static class Builder {
        private int frameSize = 960;
        private int sampleRate = 48000;
        private int channels = 1;
        private int bitrate = 64000;
        private int maxFrameSize = 5760;
        private int maxPacketSize = 3828;

        private Builder() {
        }

        public int getFrameSize() {
            return this.frameSize;
        }

        public Builder withFrameSize(int frameSize) {
            this.frameSize = frameSize;
            return this;
        }

        public int getSampleRate() {
            return this.sampleRate;
        }

        public Builder withSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public int getChannels() {
            return this.channels;
        }

        public Builder withChannels(int channels) {
            this.channels = channels;
            return this;
        }

        public int getBitrate() {
            return this.bitrate;
        }

        public Builder withBitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        public int getMaxFrameSize() {
            return this.maxFrameSize;
        }

        public Builder withMaxFrameSize(int maxFrameSize) {
            this.maxFrameSize = maxFrameSize;
            return this;
        }

        public int getMaxPacketSize() {
            return this.maxPacketSize;
        }

        public Builder withMaxPacketSize(int maxPacketSize) {
            this.maxPacketSize = maxPacketSize;
            return this;
        }

        public OpusCodec build() {
            return new OpusCodec(OpusCodecOptions.of(this.frameSize, this.sampleRate, this.channels, this.bitrate, this.maxFrameSize, this.maxPacketSize));
        }
    }
}
