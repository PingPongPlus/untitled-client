/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.server.world;

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

public class PlayerDeadPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private final List<UUID> uniqueIds = new ArrayList<UUID>();

    public PlayerDeadPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        if (this.uniqueIds.size() > 127) {
            buffer.write(0);
            return;
        }
        buffer.write(this.uniqueIds.size());
        for (UUID player : this.uniqueIds) {
            PlayerDeadPacket.writeUUID(player, buffer);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        int count = buffer.read();
        for (int i = 0; i < count; ++i) {
            this.uniqueIds.add(PlayerDeadPacket.readUUID(buffer));
        }
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handlePlayerDead(this);
    }

    public List<UUID> getUniqueIds() {
        return this.uniqueIds;
    }
}
