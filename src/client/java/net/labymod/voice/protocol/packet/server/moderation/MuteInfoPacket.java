/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.moderation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

@Deprecated
public class MuteInfoPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private String reason;
    private UUID uniqueId;
    private long timeEnd;
    private String mutedBy;

    private MuteInfoPacket(String reason, UUID uniqueId, long timeEnd) {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
        this.reason = reason;
        this.uniqueId = uniqueId;
        this.timeEnd = timeEnd;
        this.mutedBy = "";
    }

    private MuteInfoPacket(String reason, UUID uniqueId, long timeEnd, String mutedBy) {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
        this.reason = reason;
        this.uniqueId = uniqueId;
        this.timeEnd = timeEnd;
        this.mutedBy = mutedBy;
    }

    public MuteInfoPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        long secondsLeft = (this.timeEnd - System.currentTimeMillis()) / 1000L;
        MuteInfoPacket.writeUUID(this.uniqueId, buffer);
        buffer.write(this.timeEnd > System.currentTimeMillis() ? 1 : 0);
        MuteInfoPacket.writeVarLong(secondsLeft, buffer);
        MuteInfoPacket.writeString(this.reason, buffer);
        MuteInfoPacket.writeString(this.mutedBy, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
    }

    public static MuteInfoPacket unmute(UUID target) {
        return new MuteInfoPacket("", target, 0L);
    }

    public static MuteInfoPacket muteDetailed(UUID target, long timeEnd, String reason, String mutedBy) {
        return new MuteInfoPacket(reason, target, timeEnd, mutedBy);
    }

    public static MuteInfoPacket mute(UUID target, long timeEnd, String reason) {
        return new MuteInfoPacket(reason, target, timeEnd);
    }
}
