/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.util.properties.UserProperties;

public class UserShowPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private List<UserEntry> users = new ArrayList<UserEntry>();

    public UserShowPacket(UUID userId, UUID channelId, UserProperties properties) {
        this();
        this.add(userId, channelId, properties);
    }

    public UserShowPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public void add(UUID userId, UUID channelId, UserProperties properties) {
        this.users.add(new UserEntry(userId, channelId, properties));
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        UserShowPacket.writeShort((short)this.users.size(), buffer);
        for (UserEntry entry : this.users) {
            entry.write(buffer, protocolVersion);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        int entriesSize = UserShowPacket.readShort(buffer);
        this.users = new ArrayList<UserEntry>(entriesSize);
        for (int i = 0; i < entriesSize; ++i) {
            UserEntry entry = new UserEntry();
            entry.read(buffer, protocolVersion);
            this.users.add(entry);
        }
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleUserShow(this);
    }

    public List<UserEntry> getUsers() {
        return this.users;
    }

    public static class UserEntry {
        private UUID userId;
        private UUID channelId;
        private UserProperties properties;

        public UserEntry(UUID userId, UUID channelId, UserProperties properties) {
            this.userId = userId;
            this.channelId = channelId;
            this.properties = properties;
        }

        public UserEntry() {
        }

        public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
            VoicePacket.writeUUID(this.userId, buffer);
            VoicePacket.writeUUID(this.channelId, buffer);
            this.properties.writeToClient(buffer);
        }

        public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
            this.userId = VoicePacket.readUUID(buffer);
            this.channelId = VoicePacket.readUUID(buffer);
            UserProperties properties = new UserProperties();
            properties.read(buffer);
            this.properties = properties;
        }

        public UUID getUserId() {
            return this.userId;
        }

        public UUID getChannelId() {
            return this.channelId;
        }

        public UserProperties getProperties() {
            return this.properties;
        }
    }
}
