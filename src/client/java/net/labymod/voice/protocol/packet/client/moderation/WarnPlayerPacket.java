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

public class WarnPlayerPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID uniqueId;
    private String reason;

    public WarnPlayerPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        WarnPlayerPacket.writeUUID(this.uniqueId, buffer);
        if (protocolVersion >= 5) {
            WarnPlayerPacket.writeString(this.reason, buffer);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.uniqueId = WarnPlayerPacket.readUUID(buffer);
        if (protocolVersion >= 5) {
            this.reason = WarnPlayerPacket.readString(buffer);
        }
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleWarnPlayer(this);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
