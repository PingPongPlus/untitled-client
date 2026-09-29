/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.ProtocolVersion;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.type.HandshakeResponse;

public class HandshakeResponsePacket
extends VoicePacket<ServerVoicePacketHandler> {
    private HandshakeResponse response;
    private boolean isStaff;
    private int protocolVersion = ProtocolVersion.VERSION;

    public HandshakeResponsePacket() {
        super(EncryptType.SYM, ConnectionState.HANDSHAKE);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        HandshakeResponsePacket.writeInt(this.protocolVersion, buffer);
        HandshakeResponsePacket.writeEnum(this.response, buffer);
        buffer.write(this.isStaff ? 1 : 0);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.protocolVersion = HandshakeResponsePacket.readInt(buffer);
        this.response = HandshakeResponsePacket.readEnum(buffer, HandshakeResponse.values());
        this.isStaff = buffer.read() >= 1;
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleHandshakeResponse(this);
    }

    public HandshakeResponse getResponse() {
        return this.response;
    }

    public void setResponse(HandshakeResponse response) {
        this.response = response;
    }

    public boolean isStaff() {
        return this.isStaff;
    }

    public void setStaff(boolean isStaff) {
        this.isStaff = isStaff;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String toString() {
        return "HandshakeResponsePacket(response=" + String.valueOf((Object)this.getResponse()) + ", isStaff=" + this.isStaff() + ", protocolVersion=" + this.getProtocolVersion() + ")";
    }
}
