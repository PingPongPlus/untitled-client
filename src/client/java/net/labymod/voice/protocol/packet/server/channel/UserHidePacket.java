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

public class UserHidePacket
extends VoicePacket<ServerVoicePacketHandler> {
    private List<UserEntry> users = new ArrayList<UserEntry>();

    public UserHidePacket(UUID userId, UUID channelId) {
        this();
        this.add(userId, channelId);
    }

    public UserHidePacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public void add(UUID userId, UUID channelId) {
        this.users.add(new UserEntry(userId, channelId));
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        UserHidePacket.writeShort((short)this.users.size(), buffer);
        for (UserEntry entry : this.users) {
            entry.write(buffer, protocolVersion);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        int entriesSize = UserHidePacket.readShort(buffer);
        this.users = new ArrayList<UserEntry>(entriesSize);
        for (int i = 0; i < entriesSize; ++i) {
            UserEntry entry = new UserEntry();
            entry.read(buffer, protocolVersion);
            this.users.add(entry);
        }
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleUserHide(this);
    }

    public List<UserEntry> getUsers() {
        return this.users;
    }

    public static class UserEntry {
        private UUID userId;
        private UUID channelId;

        public UserEntry(UUID userId, UUID channelId) {
            this.userId = userId;
            this.channelId = channelId;
        }

        public UserEntry() {
        }

        public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
            VoicePacket.writeUUID(this.userId, buffer);
            VoicePacket.writeUUID(this.channelId, buffer);
        }

        public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
            this.userId = VoicePacket.readUUID(buffer);
            this.channelId = VoicePacket.readUUID(buffer);
        }

        public UUID getUserId() {
            return this.userId;
        }

        public UUID getChannelId() {
            return this.channelId;
        }
    }
}
