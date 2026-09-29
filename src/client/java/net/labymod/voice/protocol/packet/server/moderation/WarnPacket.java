/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.moderation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class WarnPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private String reason = "";

    public WarnPacket(String reason) {
        this();
        this.reason = reason;
    }

    public WarnPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        if (protocolVersion >= 5) {
            WarnPacket.writeString(this.reason, buffer);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        if (protocolVersion >= 5) {
            this.reason = WarnPacket.readString(buffer);
        }
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleWarn(this);
    }

    public String getReason() {
        return this.reason;
    }
}
