/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.util.properties.ChannelProperties;

public class CreateChannelPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private ChannelProperties properties;

    public CreateChannelPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public CreateChannelPacket(ChannelProperties properties) {
        this();
        this.properties = properties;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        this.properties.write(buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        ChannelProperties properties = new ChannelProperties();
        properties.read(buffer);
        this.properties = properties;
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleCreateChannel(this);
    }

    public ChannelProperties getProperties() {
        return this.properties;
    }
}
