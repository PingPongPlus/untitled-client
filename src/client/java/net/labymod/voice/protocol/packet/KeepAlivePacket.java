/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.VoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.type.TransportType;

public class KeepAlivePacket
extends VoicePacket<VoicePacketHandler> {
    public KeepAlivePacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED, TransportType.UDP);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) {
    }

    @Override
    public void handle(VoicePacketHandler handler) {
        handler.handleKeepAlive(this);
    }
}
