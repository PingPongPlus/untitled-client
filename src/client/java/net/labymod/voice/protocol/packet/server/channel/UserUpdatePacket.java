/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.util.properties.UserProperties;

public class UserUpdatePacket
extends VoicePacket<ServerVoicePacketHandler> {
    private UUID userId;
    private UserProperties properties;

    public UserUpdatePacket(UUID userId, UserProperties properties) {
        this();
        this.userId = userId;
        this.properties = properties;
    }

    public UserUpdatePacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        UserUpdatePacket.writeUUID(this.userId, buffer);
        this.properties.writeToClient(buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.userId = UserUpdatePacket.readUUID(buffer);
        UserProperties properties = new UserProperties();
        properties.read(buffer);
        this.properties = properties;
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleUserUpdateProperties(this);
    }

    public UUID getUserId() {
        return this.userId;
    }

    public UserProperties getProperties() {
        return this.properties;
    }
}
