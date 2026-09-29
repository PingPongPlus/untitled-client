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
import net.labymod.voice.protocol.util.properties.ChannelProperties;

public class ChannelShowPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private List<ChannelEntry> channels = new ArrayList<ChannelEntry>();

    public ChannelShowPacket(UUID channelId, ChannelProperties properties) {
        this();
        this.channels.add(new ChannelEntry(channelId, properties));
    }

    public ChannelShowPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public void add(UUID channelId, ChannelProperties properties) {
        this.channels.add(new ChannelEntry(channelId, properties));
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        ChannelShowPacket.writeShort((short)this.channels.size(), buffer);
        for (ChannelEntry entry : this.channels) {
            entry.write(buffer, protocolVersion);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        int entriesSize = ChannelShowPacket.readShort(buffer);
        this.channels = new ArrayList<ChannelEntry>(entriesSize);
        for (int i = 0; i < entriesSize; ++i) {
            ChannelEntry entry = new ChannelEntry();
            entry.read(buffer, protocolVersion);
            this.channels.add(entry);
        }
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleChannelShow(this);
    }

    public List<ChannelEntry> getChannels() {
        return this.channels;
    }

    public static class ChannelEntry {
        private UUID channelId;
        private ChannelProperties properties;

        public ChannelEntry(UUID channelId, ChannelProperties properties) {
            this.channelId = channelId;
            this.properties = properties;
        }

        public ChannelEntry() {
        }

        public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
            VoicePacket.writeUUID(this.channelId, buffer);
            this.properties.writeToClient(buffer);
        }

        public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
            this.channelId = VoicePacket.readUUID(buffer);
            ChannelProperties properties = new ChannelProperties();
            properties.read(buffer);
            this.properties = properties;
        }

        public UUID getChannelId() {
            return this.channelId;
        }

        public ChannelProperties getProperties() {
            return this.properties;
        }
    }
}
