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

public class SubscribeChannelsPacket
extends VoicePacket<ClientVoicePacketHandler> {
    public SubscribeChannelsPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleSubscribe(this);
    }
}
