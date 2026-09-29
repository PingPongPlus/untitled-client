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
public class PlayerUpdateReportsPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private UUID uniqueId;
    private String reason;
    private int amount;

    public PlayerUpdateReportsPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public PlayerUpdateReportsPacket(UUID uniqueId, String reason, int amount) {
        this();
        this.uniqueId = uniqueId;
        this.reason = reason;
        this.amount = amount;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        PlayerUpdateReportsPacket.writeUUID(this.uniqueId, buffer);
        PlayerUpdateReportsPacket.writeString(this.reason, buffer);
        PlayerUpdateReportsPacket.writeInt(this.amount, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
    }
}
