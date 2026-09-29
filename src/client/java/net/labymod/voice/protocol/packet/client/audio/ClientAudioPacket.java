/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.type.TransportType;

public class ClientAudioPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private byte[] data;

    public ClientAudioPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED, TransportType.UDP);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        buffer.write(ClientAudioPacket.intToBytes(this.data.length));
        buffer.write(this.data);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        byte[] lenData = new byte[4];
        int bytesRead = buffer.read(lenData);
        if (bytesRead != 4) {
            throw new IllegalStateException("Invalid audio packet: incomplete length field");
        }
        int len = ClientAudioPacket.byteArrayToInt(lenData);
        if (len < 0) {
            throw new IllegalStateException("Invalid audio packet: negative length " + len);
        }
        if (len > 1024) {
            throw new IllegalStateException("Packet too large: " + len + " bytes");
        }
        if (len < 5) {
            throw new IllegalStateException("Invalid audio packet: too small " + len + " bytes");
        }
        if (buffer.available() < len) {
            throw new IllegalStateException("Invalid audio packet: claimed " + len + " bytes but only " + buffer.available() + " available");
        }
        this.data = new byte[len];
        bytesRead = buffer.read(this.data);
        if (bytesRead != len) {
            throw new IllegalStateException("Invalid audio packet: expected " + len + " bytes but read " + bytesRead);
        }
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleClientAudio(this);
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
