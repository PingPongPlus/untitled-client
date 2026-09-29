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
import net.labymod.voice.protocol.util.properties.ChannelProperties;

public class ChannelUpdatePacket
extends VoicePacket<ServerVoicePacketHandler> {
    private UUID channelId;
    private ChannelProperties properties;

    public ChannelUpdatePacket(UUID channelId, ChannelProperties properties) {
        this();
        this.channelId = channelId;
        this.properties = properties;
    }

    public ChannelUpdatePacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        ChannelUpdatePacket.writeUUID(this.channelId, buffer);
        this.properties.writeToClient(buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.channelId = ChannelUpdatePacket.readUUID(buffer);
        ChannelProperties properties = new ChannelProperties();
        properties.read(buffer);
        this.properties = properties;
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleChannelUpdate(this);
    }

    public UUID getChannelId() {
        return this.channelId;
    }

    public ChannelProperties getProperties() {
        return this.properties;
    }
}
