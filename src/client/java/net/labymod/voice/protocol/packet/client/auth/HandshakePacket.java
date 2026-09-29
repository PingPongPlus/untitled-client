/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client.auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.labymod.voice.protocol.ProtocolVersion;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.AuthenticationMethod;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class HandshakePacket
extends VoicePacket<ClientVoicePacketHandler> {
    public static final byte[] FIREWALL_IDENTIFIER = new byte[]{0, -128, 26, -79, -79, 31, -36, 78, -128};
    private int protocolVersion;
    private AuthenticationMethod method;
    private byte[] symKey = new byte[16];
    private UUID uniqueId;
    private String string;

    public HandshakePacket() {
        super(EncryptType.ASYM, ConnectionState.HANDSHAKE);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        HandshakePacket.writeInt(ProtocolVersion.VERSION, buffer);
        buffer.write(this.symKey);
        HandshakePacket.writeUUID(this.uniqueId, buffer);
        HandshakePacket.writeEnum(this.method, buffer);
        HandshakePacket.writeString(this.string, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.protocolVersion = HandshakePacket.readInt(buffer);
        buffer.read(this.symKey);
        this.uniqueId = HandshakePacket.readUUID(buffer);
        this.method = HandshakePacket.readEnum(buffer, AuthenticationMethod.values());
        this.string = HandshakePacket.readString(buffer);
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleHandshake(this);
    }

    @Override
    public byte[] getExposedIdentifier() {
        return FIREWALL_IDENTIFIER;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public AuthenticationMethod getMethod() {
        return this.method;
    }

    public void setMethod(AuthenticationMethod method) {
        this.method = method;
    }

    public byte[] getSymKey() {
        return this.symKey;
    }

    public void setSymKey(byte[] symKey) {
        this.symKey = symKey;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getString() {
        return this.string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public static boolean isFirewallIdentifier(byte[] frame, int offset) {
        if (frame.length < offset + FIREWALL_IDENTIFIER.length) {
            return false;
        }
        for (int i = 0; i < FIREWALL_IDENTIFIER.length; ++i) {
            if (frame[offset + i] == FIREWALL_IDENTIFIER[i]) continue;
            return false;
        }
        return true;
    }
}
