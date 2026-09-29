/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.packet.client.world;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ClientVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class UpdateVisiblePlayersPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private Action action;
    private final List<UUID> uniqueIds = new ArrayList<UUID>();
    private int requestId;

    public UpdateVisiblePlayersPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        if (protocolVersion >= 6) {
            buffer.write(this.action.ordinal());
            UpdateVisiblePlayersPacket.writeShort((short)this.uniqueIds.size(), buffer);
            for (UUID player : this.uniqueIds) {
                UpdateVisiblePlayersPacket.writeUUID(player, buffer);
            }
        } else {
            buffer.write(this.requestId);
            if (this.uniqueIds.size() > 127) {
                buffer.write(0);
            } else {
                buffer.write(this.uniqueIds.size());
                for (UUID player : this.uniqueIds) {
                    UpdateVisiblePlayersPacket.writeUUID(player, buffer);
                }
            }
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        if (protocolVersion >= 6) {
            this.action = Action.values()[buffer.read()];
            int entriesSize = UpdateVisiblePlayersPacket.readShort(buffer);
            for (int i = 0; i < entriesSize; ++i) {
                this.uniqueIds.add(UpdateVisiblePlayersPacket.readUUID(buffer));
            }
        } else {
            this.requestId = buffer.read();
            int count = buffer.read();
            for (int i = 0; i < count; ++i) {
                this.uniqueIds.add(UpdateVisiblePlayersPacket.readUUID(buffer));
            }
        }
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleUpdateVisiblePlayers(this);
    }

    public Action getAction() {
        return this.action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public List<UUID> getUniqueIds() {
        return this.uniqueIds;
    }

    public int getRequestId() {
        return this.requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public static enum Action {
        ADD,
        REMOVE,
        UPDATE;

    }
}
