/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client.moderation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class UnmutePlayerPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID uniqueId;

    public UnmutePlayerPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        UnmutePlayerPacket.writeUUID(this.uniqueId, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.uniqueId = UnmutePlayerPacket.readUUID(buffer);
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleUnmutePlayer(this);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String toString() {
        return "UnmutePlayerPacket(uniqueId=" + String.valueOf(this.getUniqueId()) + ")";
    }
}
