/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class RequestChannelResetPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private boolean critical;
    private String reason;

    public RequestChannelResetPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public RequestChannelResetPacket(boolean critical, String reason) {
        this();
        this.critical = critical;
        this.reason = reason;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        RequestChannelResetPacket.writeString(this.reason, buffer);
        RequestChannelResetPacket.writeBoolean(this.critical, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.reason = RequestChannelResetPacket.readString(buffer);
        this.critical = buffer.available() > 0 ? RequestChannelResetPacket.readBoolean(buffer) : true;
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleRequestChannelReset(this);
    }

    public boolean isCritical() {
        return this.critical;
    }

    public String getReason() {
        return this.reason;
    }
}
