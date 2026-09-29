/*
 * Decompiled with CFR 0.152.
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

public class DeleteChannelPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID channelId;

    public DeleteChannelPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public DeleteChannelPacket(UUID channelId) {
        this();
        this.channelId = channelId;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        DeleteChannelPacket.writeUUID(this.channelId, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.channelId = DeleteChannelPacket.readUUID(buffer);
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleDeleteChannel(this);
    }

    public UUID getChannelId() {
        return this.channelId;
    }
}
