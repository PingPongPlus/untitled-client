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
import net.labymod.voice.protocol.packet.client.moderation.UnmutePlayerPacket;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class MutePlayerPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID uniqueId = null;
    private String reason = "Unknown";
    private long duration = 0L;
    private int relatedReportId = -1;
    @Deprecated
    private boolean isUnmute;

    public MutePlayerPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        MutePlayerPacket.writeUUID(this.uniqueId, buffer);
        MutePlayerPacket.writeString(this.reason, buffer);
        MutePlayerPacket.writeVarLong(this.duration, buffer);
        MutePlayerPacket.writeInt(this.relatedReportId, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.uniqueId = MutePlayerPacket.readUUID(buffer);
        if (protocolVersion >= 3) {
            this.reason = MutePlayerPacket.readString(buffer);
            this.duration = MutePlayerPacket.readVarLong(buffer);
            this.relatedReportId = MutePlayerPacket.readInt(buffer);
        } else {
            this.isUnmute = buffer.read() == 0;
            this.reason = MutePlayerPacket.readString(buffer);
            this.duration = (long)(MutePlayerPacket.readInt(buffer) * 60 * 60) * 1000L;
        }
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        if (this.isUnmute) {
            UnmutePlayerPacket packet = new UnmutePlayerPacket();
            packet.setUniqueId(this.uniqueId);
            handler.handleUnmutePlayer(packet);
        } else {
            handler.handleMutePlayer(this);
        }
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

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getRelatedReportId() {
        return this.relatedReportId;
    }

    public void setRelatedReportId(int relatedReportId) {
        this.relatedReportId = relatedReportId;
    }

    @Deprecated
    public boolean isUnmute() {
        return this.isUnmute;
    }

    @Deprecated
    public void setUnmute(boolean isUnmute) {
        this.isUnmute = isUnmute;
    }

    public String toString() {
        return "MutePlayerPacket(uniqueId=" + String.valueOf(this.getUniqueId()) + ", reason=" + this.getReason() + ", duration=" + this.getDuration() + ", relatedReportId=" + this.getRelatedReportId() + ", isUnmute=" + this.isUnmute() + ")";
    }
}
