/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client.moderation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class RequestPlayerMetaPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID uniqueId;

    public RequestPlayerMetaPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public RequestPlayerMetaPacket(UUID uniqueId) {
        this();
        this.uniqueId = uniqueId;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        RequestPlayerMetaPacket.writeUUID(this.uniqueId, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.uniqueId = RequestPlayerMetaPacket.readUUID(buffer);
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleRequestPlayerMeta(this);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }
}
