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

public class ReportPlayerPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID uniqueId = null;
    private String reason = "Unknown";

    public ReportPlayerPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        ReportPlayerPacket.writeUUID(this.uniqueId, buffer);
        ReportPlayerPacket.writeString(this.reason, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.uniqueId = ReportPlayerPacket.readUUID(buffer);
        this.reason = ReportPlayerPacket.readString(buffer);
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleReportPlayer(this);
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
