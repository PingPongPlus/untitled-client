/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class DisconnectPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private String reason;

    public DisconnectPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public DisconnectPacket(String reason) {
        this();
        this.reason = reason;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        DisconnectPacket.writeString(this.reason, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.reason = DisconnectPacket.readString(buffer);
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleDisconnect(this);
    }

    public String getReason() {
        return this.reason;
    }
}
