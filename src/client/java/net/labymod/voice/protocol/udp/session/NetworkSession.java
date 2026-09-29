/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.udp.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import net.labymod.voice.protocol.Encryption;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.udp.session.Frame;
import net.labymod.voice.protocol.udp.session.FrameProcessor;
import net.labymod.voice.protocol.udp.session.MalformedFrameException;
import net.labymod.voice.protocol.udp.session.NetworkVersion;

public class NetworkSession {
    public static final int MAX_SEGMENT_SIZE = 512;
    public static final int MAX_PACKET_SIZE = 1024;
    public static final int MAX_SEGMENTS = 1000;
    public static final int MAX_FRAME_SIZE = 512000;
    protected final InetSocketAddress address;
    private final FrameProcessor frameProcessor;
    private final Map<Short, Frame> framesIn = new ConcurrentHashMap<Short, Frame>();
    private final Map<Short, Frame> framesOut = new ConcurrentHashMap<Short, Frame>();
    private final Map<Short, Frame> bufferedFrames = new ConcurrentHashMap<Short, Frame>();
    private NetworkVersion networkVersion;
    private Encryption symmetricEncryption;
    private Encryption asymmetricEncryption;
    private short frameIdOut;
    private short frameIdIn;
    private long timeFirstBuffer;

    public NetworkSession(InetSocketAddress address, FrameProcessor frameProcessor) {
        this(address, frameProcessor, NetworkVersion.UNIDENTIFIED);
    }

    public NetworkSession(InetSocketAddress address, FrameProcessor frameProcessor, NetworkVersion networkVersion) {
        this.address = address;
        this.frameProcessor = frameProcessor;
        this.networkVersion = networkVersion;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void receiveSegment(DatagramSocket socket, byte[] data, int offset, int length, Cipher cipher) throws Exception {
        short frameId;
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data, offset, length);
        if (this.networkVersion == NetworkVersion.V1 || this.networkVersion == NetworkVersion.UNIDENTIFIED && NetworkSession.isLegacyFrame(data, offset)) {
            this.processLegacySecureFrame(data, offset, length, cipher);
            return;
        }
        byte transmissionType = (byte)inputStream.read();
        if (transmissionType == 0) {
            int available = inputStream.available();
            if (available > 1024) {
                throw new MalformedFrameException("Invalid frame size");
            }
            byte[] frame = new byte[available];
            inputStream.read(frame);
            this.processSecureFrame(frame, cipher);
        }
        if (transmissionType == 1) {
            Frame frame;
            frameId = VoicePacket.readShort(inputStream);
            short segmentId = VoicePacket.readShort(inputStream);
            short totalSegments = VoicePacket.readShort(inputStream);
            short segmentLength = VoicePacket.readShort(inputStream);
            if (totalSegments <= 0 || totalSegments > 1000 || segmentLength < 0 || segmentLength > 512) {
                return;
            }
            byte[] segment = new byte[segmentLength];
            int actuallyRead = inputStream.read(segment);
            if (actuallyRead == -1) {
                actuallyRead = 0;
            }
            if (actuallyRead < segmentLength) {
                return;
            }
            Map<Short, Frame> map = this.framesIn;
            synchronized (map) {
                frame = this.framesIn.computeIfAbsent(frameId, id -> new Frame(id, totalSegments));
                frame.setSegment(segmentId, segment, true);
            }
            this.sendAcknowledge(socket, frameId, segmentId);
            if (frame.isComplete()) {
                Frame removed;
                Map<Short, Frame> map2 = this.framesIn;
                synchronized (map2) {
                    removed = this.framesIn.remove(frameId);
                }
                if (removed != null) {
                    if (frame.isResetBuffer()) {
                        this.frameIdIn = (short)(frameId + 1);
                        if (this.networkVersion.isOrGreater(NetworkVersion.V3)) {
                            this.frameIdOut = 0;
                            this.framesIn.clear();
                            this.framesOut.clear();
                            this.bufferedFrames.clear();
                        }
                    } else {
                        if (this.frameIdIn > 2 && frameId == 1) {
                            this.frameIdIn = frameId;
                        }
                        if (this.networkVersion.isOrGreater(NetworkVersion.V3) || (removed.headerByte() & 8) == 8) {
                            this.bufferTcpFrame(socket, removed, cipher);
                        } else {
                            this.processSecureFrame(removed.bytes(), cipher);
                        }
                    }
                }
            }
        }
        if (transmissionType == 2) {
            frameId = VoicePacket.readShort(inputStream);
            short segmentId = VoicePacket.readShort(inputStream);
            Map<Short, Frame> map = this.framesOut;
            synchronized (map) {
                Frame frame = this.framesOut.get(frameId);
                if (frame != null) {
                    frame.acknowledge(segmentId);
                    if (frame.isComplete()) {
                        this.framesOut.remove(frameId);
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void bufferTcpFrame(DatagramSocket socket, Frame frame, Cipher cipher) throws Exception {
        boolean isInSync;
        short frameId = frame.getFrameId();
        Map<Short, Frame> map = this.framesIn;
        synchronized (map) {
            isInSync = frameId == this.frameIdIn;
        }
        if (isInSync) {
            try {
                this.processTcpFrame(frame.bytes(), cipher);
            }
            catch (BadPaddingException e) {
                this.frameIdIn = (short)(this.frameIdIn + 1);
                System.out.println("[" + this.address.getHostString() + "] [DIRECT] " + e.getMessage() + " | Frame ID: " + frame.getFrameId() + " | Total Segments: " + frame.getTotalSegments() + " | Acknowledged Segments: " + frame.getAcknowledgeSegments() + " | Frame length: " + frame.bytes().length);
            }
            this.processBufferedFrames(cipher);
        } else {
            short expected;
            if (this.bufferedFrames.isEmpty()) {
                this.timeFirstBuffer = System.currentTimeMillis();
            }
            this.bufferedFrames.put(frameId, frame);
            long timePassedSinceFirstBuffer = System.currentTimeMillis() - this.timeFirstBuffer;
            if (timePassedSinceFirstBuffer <= 1000L) {
                return;
            }
            short chosen = expected = this.frameIdIn;
            int bestDistance = Integer.MAX_VALUE;
            for (Short idObj : this.bufferedFrames.keySet()) {
                int uFrom;
                short id = idObj;
                int uTo = id & 0xFFFF;
                int distance = uTo >= (uFrom = expected & 0xFFFF) ? uTo - uFrom : 65536 - uFrom + uTo;
                if (distance <= 0 || distance >= bestDistance) continue;
                bestDistance = distance;
                chosen = id;
            }
            if (this.bufferedFrames.isEmpty()) {
                return;
            }
            if (bestDistance == Integer.MAX_VALUE) {
                return;
            }
            if (bestDistance < 32768) {
                synchronized (this.framesIn) {
                    this.frameIdIn = chosen;
                }
                this.processBufferedFrames(cipher);
            } else {
                try {
                    this.sendResetFrame(socket);
                }
                catch (Exception ex) {
                    System.out.println("[" + this.address.getHostString() + "] [IN] Failed to send reset: " + ex.getMessage());
                }
                synchronized (this.framesIn) {
                    this.framesIn.clear();
                    this.bufferedFrames.clear();
                    this.frameIdIn = 0;
                }
            }
            this.timeFirstBuffer = System.currentTimeMillis();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void processBufferedFrames(Cipher cipher) throws Exception {
        while (true) {
            Frame bufferedFrame;
            Map<Short, Frame> map = this.framesIn;
            synchronized (map) {
                bufferedFrame = this.bufferedFrames.remove(this.frameIdIn);
                if (bufferedFrame == null) {
                    break;
                }
            }
            try {
                this.processTcpFrame(bufferedFrame.bytes(), cipher);
            }
            catch (BadPaddingException e) {
                this.frameIdIn = (short)(this.frameIdIn + 1);
                System.out.println("[" + this.address.getHostString() + "] [BUFFERED] " + e.getMessage() + " | Frame ID: " + bufferedFrame.getFrameId() + " | Total Segments: " + bufferedFrame.getTotalSegments() + " | Acknowledged Segments: " + bufferedFrame.getAcknowledgeSegments() + " | Frame length: " + bufferedFrame.bytes().length);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void processTcpFrame(byte[] secureFrame, Cipher cipher) throws Exception {
        this.processSecureFrame(secureFrame, cipher);
        Map<Short, Frame> map = this.framesIn;
        synchronized (map) {
            this.frameIdIn = (short)(this.frameIdIn + 1);
        }
    }

    public void resendUnacknowledgedFrames(DatagramSocket socket) throws IOException {
        try {
            Frame frame;
            short frameId;
            if (!this.framesOut.isEmpty()) {
                for (Map.Entry<Short, Frame> entry : this.framesOut.entrySet()) {
                    frameId = entry.getKey();
                    frame = entry.getValue();
                    if (frame.isComplete() || frame.isTimeout()) {
                        this.framesOut.remove(frameId);
                        continue;
                    }
                    short totalSegments = frame.getTotalSegments();
                    boolean hadToResend = false;
                    for (short segmentId = 0; segmentId < totalSegments; segmentId = (short)(segmentId + 1)) {
                        if (!frame.isSegmentLost(segmentId)) continue;
                        this.sendSegment(socket, frameId, segmentId, totalSegments, frame.getData(segmentId));
                        hadToResend = true;
                    }
                    if (!hadToResend) continue;
                    frame.markTransmit();
                }
            }
            if (!this.framesIn.isEmpty()) {
                for (Map.Entry<Short, Frame> entry : this.framesIn.entrySet()) {
                    frameId = entry.getKey();
                    frame = entry.getValue();
                    if (!frame.isTimeout()) continue;
                    this.framesIn.remove(frameId);
                }
            }
        }
        catch (Exception e) {
            System.out.println("[" + this.address.getHostString() + "] [IN] Failed to resend unacknowledged frames: " + e.getClass().getSimpleName());
        }
    }

    private void processSecureFrame(byte[] secureFrame, Cipher cipher) throws Exception {
        byte[] frame = this.decodeSecureFrame(secureFrame, cipher);
        if (frame == null) {
            return;
        }
        this.frameProcessor.onFrameReceived(this, frame);
    }

    private void processLegacySecureFrame(byte[] legacy, int start, int length, Cipher cipher) throws Exception {
        byte packedId;
        int offset = start;
        int end = start + length;
        if ((packedId = legacy[offset++]) == 0) {
            offset += 7;
        }
        if (packedId != 0 && !this.hasEncryption(EncryptType.SYM)) {
            System.out.println("[" + this.address.getHostString() + "] [IN] Tried to process packet 0x" + Long.toHexString(packedId) + " before handshake");
            return;
        }
        EncryptType encryptType = packedId == 0 ? EncryptType.ASYM : (packedId == 2 ? EncryptType.NONE : EncryptType.SYM);
        int toDecryptLength = end - offset;
        if (toDecryptLength > 1024) {
            throw new MalformedFrameException("Invalid frame size");
        }
        byte[] toDecrypt = new byte[toDecryptLength];
        System.arraycopy(legacy, offset, toDecrypt, 0, toDecryptLength);
        try {
            byte[] payload = this.decrypt(encryptType, toDecrypt, cipher);
            byte[] frame = new byte[1 + payload.length];
            frame[0] = packedId;
            System.arraycopy(payload, 0, frame, 1, payload.length);
            this.networkVersion = NetworkVersion.V1;
            this.frameProcessor.onFrameReceived(this, frame);
        }
        catch (IllegalBlockSizeException payload) {
        }
        catch (BadPaddingException e) {
            System.out.println("[" + this.address.getHostString() + "] [IN] Tried to process packet 0x" + Long.toHexString(packedId) + " with invalid encryption key");
        }
    }

    public int sendLegacySecureUdpFrame(DatagramSocket socket, byte[] frame, EncryptType encryptType, Cipher cipher) throws Exception {
        byte[] toEncrypt = new byte[frame.length - 1];
        System.arraycopy(frame, 1, toEncrypt, 0, frame.length - 1);
        byte[] encrypted = this.encrypt(encryptType, toEncrypt, cipher);
        byte[] legacy = new byte[1 + encrypted.length];
        legacy[0] = frame[0];
        System.arraycopy(encrypted, 0, legacy, 1, encrypted.length);
        return this.sendToSocket(socket, legacy);
    }

    public int sendSecureUdpFrame(DatagramSocket socket, byte[] identifier, byte[] frame, EncryptType encryptType, Cipher cipher) throws Exception {
        byte[] secureFrame = this.encodeSecureFrame(identifier, frame, encryptType, cipher);
        return this.sendUdpFrame(socket, secureFrame);
    }

    public int sendSecureTcpFrame(DatagramSocket socket, byte[] identifier, byte[] frame, EncryptType encryptType, Cipher cipher) throws Exception {
        byte[] secureFrame = this.encodeSecureFrame(identifier, frame, encryptType, cipher);
        return this.sendTcpFrame(socket, secureFrame);
    }

    public void sendResetFrame(DatagramSocket socket) throws Exception {
        this.frameIdOut = 0;
        this.sendTcpFrame(socket, new byte[0]);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int sendTcpFrame(DatagramSocket socket, byte[] frame) throws Exception {
        short frameId;
        int bytesSent = 0;
        Map<Short, Frame> map = this.framesOut;
        synchronized (map) {
            short s = this.frameIdOut;
            this.frameIdOut = (short)(s + 1);
            frameId = s;
        }
        int frameLength = frame.length;
        if (frameLength > 512000) {
            throw new MalformedFrameException("Invalid frame size: " + frameLength + " bytes");
        }
        short totalSegments = (short)Math.max(1, (frameLength + 512 - 1) / 512);
        Frame frameObj = new Frame(frameId, totalSegments);
        frameObj.markTransmit();
        for (short segmentId = 0; segmentId < totalSegments; segmentId = (short)(segmentId + 1)) {
            int offset = segmentId * 512;
            int segmentLength = frameLength == 0 ? 0 : Math.min(frameLength - offset, 512);
            byte[] segment = new byte[segmentLength];
            if (segmentLength > 0) {
                System.arraycopy(frame, offset, segment, 0, segmentLength);
            }
            bytesSent += this.sendSegment(socket, frameId, segmentId, totalSegments, segment);
            frameObj.setSegment(segmentId, segment, false);
        }
        Map<Short, Frame> map2 = this.framesOut;
        synchronized (map2) {
            this.framesOut.put(frameId, frameObj);
        }
        return bytesSent;
    }

    public int sendUdpFrame(DatagramSocket socket, byte[] frame) throws Exception {
        byte[] packedFrame = new byte[frame.length + 1];
        packedFrame[0] = 0;
        System.arraycopy(frame, 0, packedFrame, 1, frame.length);
        return this.sendToSocket(socket, packedFrame);
    }

    private int sendSegment(DatagramSocket socket, short frameId, short segmentId, short totalSegments, byte[] segment) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(1);
        VoicePacket.writeShort(frameId, outputStream);
        VoicePacket.writeShort(segmentId, outputStream);
        VoicePacket.writeShort(totalSegments, outputStream);
        VoicePacket.writeShort((short)segment.length, outputStream);
        outputStream.write(segment);
        return this.sendToSocket(socket, outputStream.toByteArray());
    }

    public void sendAcknowledge(DatagramSocket socket, short frameId, short segmentId) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(2);
        VoicePacket.writeShort(frameId, outputStream);
        VoicePacket.writeShort(segmentId, outputStream);
        this.sendToSocket(socket, outputStream.toByteArray());
    }

    public int sendToSocket(DatagramSocket socket, byte[] data) throws IOException {
        try {
            return this.frameProcessor.sendToSocket(this, socket, data);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException("Failed to send frame", e);
        }
    }

    public byte[] encodeSecureFrame(byte[] identifier, byte[] frame, EncryptType encryptType, Cipher cipher) throws Exception {
        ByteArrayOutputStream secureBuffer = new ByteArrayOutputStream();
        secureBuffer.write(encryptType.getId() | (identifier.length > 0 ? 4 : 0) | (this.networkVersion.isOrGreater(NetworkVersion.V3) ? 8 : 0));
        if (identifier.length > 0) {
            VoicePacket.writeVarInt(identifier.length, secureBuffer);
            secureBuffer.write(identifier);
        }
        if (this.networkVersion.isOrGreater(NetworkVersion.V3)) {
            secureBuffer.write(this.networkVersion.ordinal());
        }
        byte[] secureFrame = this.encrypt(encryptType, frame, cipher);
        VoicePacket.writeVarInt(secureFrame.length, secureBuffer);
        secureBuffer.write(secureFrame);
        return secureBuffer.toByteArray();
    }

    public byte[] decodeSecureFrame(byte[] frame, Cipher cipher) throws Exception {
        ByteArrayInputStream secureBuffer = new ByteArrayInputStream(frame);
        byte header = (byte)secureBuffer.read();
        EncryptType encryptType = EncryptType.fromId(header & 3);
        if ((header & 4) == 4) {
            secureBuffer.skip(VoicePacket.readVarInt(secureBuffer));
        }
        if ((header & 8) == 8) {
            byte version = (byte)secureBuffer.read();
            if (this.networkVersion == NetworkVersion.UNIDENTIFIED) {
                this.networkVersion = NetworkVersion.VALUES[version];
            }
        } else if (this.networkVersion == NetworkVersion.UNIDENTIFIED) {
            this.networkVersion = NetworkVersion.V2;
        }
        int payloadLength = VoicePacket.readVarInt(secureBuffer);
        if (payloadLength < 0 || payloadLength > 512000 || payloadLength > secureBuffer.available()) {
            throw new MalformedFrameException("Invalid payload size: " + payloadLength + " bytes");
        }
        byte[] payload = new byte[payloadLength];
        int read = secureBuffer.read(payload);
        if (read != payloadLength) {
            throw new MalformedFrameException("Truncated payload");
        }
        return this.decrypt(encryptType, payload, cipher);
    }

    private byte[] decrypt(EncryptType type, byte[] data, Cipher cipher) throws Exception {
        if (!this.hasEncryption(type)) {
            throw new IllegalStateException("Can't decrypt packet without " + type.name() + " encryption key");
        }
        switch (type) {
            case ASYM: {
                if (cipher == null) {
                    return this.asymmetricEncryption.decrypt(data);
                }
                return this.asymmetricEncryption.decrypt(data, cipher);
            }
            case SYM: {
                return this.symmetricEncryption.decrypt(data);
            }
        }
        return data;
    }

    private byte[] encrypt(EncryptType type, byte[] data, Cipher cipher) throws Exception {
        if (!this.hasEncryption(type)) {
            throw new IllegalStateException("Can't encrypt packet without " + type.name() + " encryption key");
        }
        switch (type) {
            case ASYM: {
                if (cipher == null) {
                    return this.asymmetricEncryption.encrypt(data);
                }
                return this.asymmetricEncryption.encrypt(data, cipher);
            }
            case SYM: {
                return this.symmetricEncryption.encrypt(data);
            }
        }
        return data;
    }

    public void setAsymmetricEncryption(Encryption encryption) {
        this.asymmetricEncryption = encryption;
    }

    public void setSymmetricEncryption(Encryption encryption) {
        this.symmetricEncryption = encryption;
    }

    public boolean hasEncryption(EncryptType encryptType) {
        switch (encryptType) {
            case ASYM: {
                return this.asymmetricEncryption != null;
            }
            case SYM: {
                return this.symmetricEncryption != null;
            }
            case NONE: {
                return true;
            }
        }
        return false;
    }

    public Encryption getSymmeticEncryption() {
        return this.symmetricEncryption;
    }

    public Encryption getAsymmetricEncryption() {
        return this.asymmetricEncryption;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public NetworkVersion getNetworkVersion() {
        return this.networkVersion;
    }

    public static boolean isFrameBehind(short id, short marker) {
        if (id > 0 && marker > 0) {
            return id < marker;
        }
        if (id > 0 && marker < 0) {
            int diff = marker + 65534 - id;
            return diff < Short.MAX_VALUE;
        }
        if (id < 0 && marker > 0) {
            int diff = id + 65534 - marker;
            return diff > Short.MAX_VALUE;
        }
        return id < marker;
    }

    public static boolean isLegacyFrame(byte[] frame, int offset) {
        return offset >= 0 && frame.length >= offset + 9 && frame[offset] == 0 && frame[offset + 1] == -128 && frame[offset + 2] == 26 && frame[offset + 3] == -79 && frame[offset + 4] == -79 && frame[offset + 5] == 31 && frame[offset + 6] == -36 && frame[offset + 7] == 78 && frame[offset + 8] == -128;
    }
}
