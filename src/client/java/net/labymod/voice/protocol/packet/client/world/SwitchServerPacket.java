/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client.world;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class SwitchServerPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private String server;
    private int port;

    public SwitchServerPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        SwitchServerPacket.writeString(this.server, buffer);
        SwitchServerPacket.writeInt(this.port, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.server = SwitchServerPacket.readString(buffer);
        this.port = SwitchServerPacket.readInt(buffer);
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleSwitchServer(this);
    }

    public String getServer() {
        return this.server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
