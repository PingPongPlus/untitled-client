/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.labymod.voice.protocol.packet.client.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import org.jetbrains.annotations.Nullable;

public class MoveUserToChannelPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID userId;
    private UUID channelId;
    private String password;

    public MoveUserToChannelPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public MoveUserToChannelPacket(UUID userId, UUID channelId) {
        this(userId, channelId, null);
    }

    public MoveUserToChannelPacket(UUID userId, UUID channelId, String password) {
        this();
        this.userId = userId;
        this.channelId = channelId;
        this.password = password;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        MoveUserToChannelPacket.writeUUID(this.userId, buffer);
        MoveUserToChannelPacket.writeUUID(this.channelId, buffer);
        MoveUserToChannelPacket.writeString(this.password, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.userId = MoveUserToChannelPacket.readUUID(buffer);
        this.channelId = MoveUserToChannelPacket.readUUID(buffer);
        this.password = MoveUserToChannelPacket.readString(buffer);
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleMovePlayerToChannel(this);
    }

    public UUID getUserId() {
        return this.userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getChannelId() {
        return this.channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    @Nullable
    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
