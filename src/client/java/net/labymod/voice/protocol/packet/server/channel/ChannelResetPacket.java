/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class ChannelResetPacket
extends VoicePacket<ServerVoicePacketHandler> {
    public ChannelResetPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleChannelReset(this);
    }
}
