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
import net.labymod.voice.protocol.util.properties.UserProperties;

public class UpdatePropertiesPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UserProperties properties;

    public UpdatePropertiesPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public UpdatePropertiesPacket(UserProperties properties) {
        this();
        this.properties = properties;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        this.properties.write(buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        UserProperties properties = new UserProperties();
        properties.read(buffer);
        this.properties = properties;
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleUpdateProperties(this);
    }

    public UserProperties getProperties() {
        return this.properties;
    }
}
