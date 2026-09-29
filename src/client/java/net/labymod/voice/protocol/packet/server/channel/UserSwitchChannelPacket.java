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

public class UserSwitchChannelPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private UUID userId;
    private UUID previousChannelId;
    private UUID newChannelId;
    private UserProperties properties;

    public UserSwitchChannelPacket(UUID userId, UUID previousChannelId, UUID newChannelId, UserProperties properties) {
        this();
        this.userId = userId;
        this.previousChannelId = previousChannelId;
        this.newChannelId = newChannelId;
        this.properties = properties;
    }

    public UserSwitchChannelPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        UserSwitchChannelPacket.writeUUID(this.userId, buffer);
        UserSwitchChannelPacket.writeUUID(this.previousChannelId, buffer);
        UserSwitchChannelPacket.writeUUID(this.newChannelId, buffer);
        this.properties.writeToClient(buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.userId = UserSwitchChannelPacket.readUUID(buffer);
        this.previousChannelId = UserSwitchChannelPacket.readUUID(buffer);
        this.newChannelId = UserSwitchChannelPacket.readUUID(buffer);
        UserProperties properties = new UserProperties();
        properties.read(buffer);
        this.properties = properties;
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleUserSwitchChannel(this);
    }

    public UUID getUserId() {
        return this.userId;
    }

    public UUID getPreviousChannelId() {
        return this.previousChannelId;
    }

    public UUID getNewChannelId() {
        return this.newChannelId;
    }

    public UserProperties getProperties() {
        return this.properties;
    }
}
