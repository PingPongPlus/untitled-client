/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package net.labymod.voice.protocol.packet.server.moderation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.handler.ServerVoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;

public class PlayerUpdateMetaPacket
extends VoicePacket<ServerVoicePacketHandler> {
    private UUID uniqueId;
    private JsonObject meta;

    public PlayerUpdateMetaPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    public PlayerUpdateMetaPacket(UUID uniqueId, JsonObject meta) {
        this();
        this.uniqueId = uniqueId;
        this.meta = meta == null ? new JsonObject() : meta;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        PlayerUpdateMetaPacket.writeUUID(this.uniqueId, buffer);
        VoicePacket.writeJson((JsonElement)this.meta, buffer);
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.uniqueId = PlayerUpdateMetaPacket.readUUID(buffer);
        this.meta = VoicePacket.readJson(buffer).getAsJsonObject();
    }

    @Override
    public void handle(ServerVoicePacketHandler handler) {
        handler.handlePlayerMetaUpdate(this);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public JsonObject getMeta() {
        return this.meta;
    }
}
