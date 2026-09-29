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

public class ChannelHidePacket
extends VoicePacket<ServerVoicePacketHandler> {
    private List<UUID> channels = new ArrayList<UUID>();

    public ChannelHidePacket(UUID channelId) {
        this();
        this.add(channelId);
    }

    public ChannelHidePacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public void add(UUID channelId) {
        this.channels.add(channelId);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        ChannelHidePacket.writeShort((short)this.channels.size(), buffer);
        for (UUID entry : this.channels) {
            ChannelHidePacket.writeUUID(entry, buffer);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        int entriesSize = ChannelHidePacket.readShort(buffer);
        this.channels = new ArrayList<UUID>(entriesSize);
        for (int i = 0; i < entriesSize; ++i) {
            this.channels.add(ChannelHidePacket.readUUID(buffer));
        }
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleChannelHide(this);
    }

    public List<UUID> getChannels() {
        return this.channels;
    }
}
