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
import net.labymod.voice.protocol.util.properties.ChannelProperties;

public class UpdateChannelPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID channelId;
    private ChannelProperties properties;

    public UpdateChannelPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public UpdateChannelPacket(UUID channelId, ChannelProperties properties) {
        this();
        this.channelId = channelId;
        this.properties = properties;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        UpdateChannelPacket.writeUUID(this.channelId, buffer);
        this.properties.write(buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.channelId = UpdateChannelPacket.readUUID(buffer);
        ChannelProperties properties = new ChannelProperties();
        properties.read(buffer);
        this.properties = properties;
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleUpdateChannel(this);
    }

    public UUID getChannelId() {
        return this.channelId;
    }

    public ChannelProperties getProperties() {
        return this.properties;
    }
}
