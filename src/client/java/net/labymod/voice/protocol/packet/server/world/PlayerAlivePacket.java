/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package net.labymod.voice.protocol.packet.server.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class PlayerAlivePacket
extends VoicePacket<ServerVoicePacketHandler> {
    private final List<UUID> uniqueIds = new ArrayList<UUID>();
    private final Map<UUID, JsonObject> meta = new HashMap<UUID, JsonObject>();

    public PlayerAlivePacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public void add(UUID uniqueId, JsonObject meta) {
        this.uniqueIds.add(uniqueId);
        if (meta != null) {
            this.meta.put(uniqueId, meta);
        }
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        if (protocolVersion >= 6) {
            PlayerAlivePacket.writeShort((short)this.uniqueIds.size(), buffer);
        } else {
            buffer.write(this.uniqueIds.size());
        }
        for (UUID uUID : this.uniqueIds) {
            PlayerAlivePacket.writeUUID(uUID, buffer);
        }
        if (protocolVersion == 3) {
            buffer.write(this.uniqueIds.size());
            for (UUID uUID : this.uniqueIds) {
                JsonObject json = this.meta.get(uUID);
                VoicePacket.writeJson((JsonElement)(json == null ? new JsonObject() : json), buffer);
            }
        }
        if (protocolVersion >= 4) {
            if (protocolVersion >= 6) {
                PlayerAlivePacket.writeShort((short)this.meta.size(), buffer);
            } else {
                buffer.write(this.meta.size());
            }
            for (Map.Entry entry : this.meta.entrySet()) {
                PlayerAlivePacket.writeUUID((UUID)entry.getKey(), buffer);
                VoicePacket.writeJson((JsonElement)entry.getValue(), buffer);
            }
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        int i;
        int metaCount;
        int playerCount = protocolVersion >= 6 ? (int)PlayerAlivePacket.readShort(buffer) : (int)buffer.read();
        for (int i2 = 0; i2 < playerCount; ++i2) {
            this.uniqueIds.add(PlayerAlivePacket.readUUID(buffer));
        }
        if (protocolVersion == 3) {
            metaCount = buffer.read();
            for (i = 0; i < metaCount; ++i) {
                JsonElement json = VoicePacket.readJson(buffer);
                this.meta.put(this.uniqueIds.get(i), json.getAsJsonObject());
            }
        }
        if (protocolVersion >= 4) {
            metaCount = protocolVersion >= 6 ? (byte)PlayerAlivePacket.readShort(buffer) : (byte)buffer.read();
            for (i = 0; i < metaCount; ++i) {
                UUID uniqueId = PlayerAlivePacket.readUUID(buffer);
                JsonElement json = VoicePacket.readJson(buffer);
                this.meta.put(uniqueId, json.getAsJsonObject());
            }
        }
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handlePlayerAlive(this);
    }

    public Collection<UUID> getUniqueIds() {
        return this.uniqueIds;
    }

    public boolean hasUniqueIds() {
        return !this.uniqueIds.isEmpty();
    }

    public JsonObject getMeta(UUID uniqueId) {
        return this.meta.get(uniqueId);
    }
}
