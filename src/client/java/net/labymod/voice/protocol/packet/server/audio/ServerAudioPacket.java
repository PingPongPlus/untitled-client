/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.type.TransportType;

public class ServerAudioPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private UUID uniqueId;
    private byte[] data;

    public ServerAudioPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED, TransportType.UDP);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        ServerAudioPacket.writeUUID(this.uniqueId, buffer);
        ServerAudioPacket.writeInt(this.data.length, buffer);
        buffer.write(this.data);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.uniqueId = ServerAudioPacket.readUUID(buffer);
        int length = ServerAudioPacket.readInt(buffer);
        if (length > 1024) {
            throw new IllegalStateException("Packet too large");
        }
        this.data = new byte[length];
        buffer.read(this.data);
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleServerAudio(this);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
