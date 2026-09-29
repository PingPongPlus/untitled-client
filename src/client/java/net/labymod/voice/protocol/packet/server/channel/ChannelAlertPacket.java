/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class ChannelAlertPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private String messageId;
    private List<String> arguments;

    public ChannelAlertPacket(String messageId, List<String> arguments) {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
        this.messageId = messageId;
        this.arguments = arguments;
    }

    public ChannelAlertPacket(String messageId) {
        this(messageId, new ArrayList<String>());
    }

    public ChannelAlertPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        ChannelAlertPacket.writeString(this.messageId, buffer);
        ChannelAlertPacket.writeShort((short)this.arguments.size(), buffer);
        for (String argument : this.arguments) {
            ChannelAlertPacket.writeString(argument, buffer);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.messageId = ChannelAlertPacket.readString(buffer);
        int argumentsSize = ChannelAlertPacket.readShort(buffer);
        ArrayList<String> arguments = new ArrayList<String>(argumentsSize);
        for (int i = 0; i < argumentsSize; ++i) {
            arguments.add(ChannelAlertPacket.readString(buffer));
        }
        this.arguments = arguments;
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handleChannelAlert(this);
    }

    public String getMessageId() {
        return this.messageId;
    }

    public List<String> getArguments() {
        return this.arguments;
    }
}
