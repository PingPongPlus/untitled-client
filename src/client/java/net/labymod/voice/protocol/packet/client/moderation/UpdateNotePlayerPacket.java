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

public class UpdateNotePlayerPacket
extends VoicePacket<ClientVoicePacketHandler> {
    private UUID uniqueId;
    private String note;
    private Action action;
    private int noteId;

    public UpdateNotePlayerPacket() {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
    }

    private UpdateNotePlayerPacket(UUID uniqueId, String note, int noteId, Action action) {
        super(EncryptType.SYM, ConnectionState.CONNECTED);
        this.uniqueId = uniqueId;
        this.note = note;
        this.noteId = noteId;
        this.action = action;
    }

    @Override
    public void write(ByteArrayOutputStream buffer, int protocolVersion) throws IOException {
        VoicePacket.writeVarInt(this.action.ordinal(), buffer);
        VoicePacket.writeUUID(this.uniqueId, buffer);
        if (this.action == Action.REMOVE || this.action == Action.UPDATE) {
            VoicePacket.writeVarInt(this.noteId, buffer);
        }
        if (this.action != Action.REMOVE) {
            VoicePacket.writeString(this.note, buffer);
        }
    }

    @Override
    public void read(ByteArrayInputStream buffer, int protocolVersion) throws IOException {
        this.action = Action.values()[VoicePacket.readVarInt(buffer)];
        this.uniqueId = VoicePacket.readUUID(buffer);
        if (this.action == Action.REMOVE || this.action == Action.UPDATE) {
            this.noteId = VoicePacket.readVarInt(buffer);
        }
        if (this.action != Action.REMOVE) {
            this.note = VoicePacket.readString(buffer);
        }
    }

    @Override
    public void handle(ClientVoicePacketHandler handler) {
        handler.handleUpdateNotePlayer(this);
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public int getNoteId() {
        return this.noteId;
    }

    public String getNote() {
        return this.note;
    }

    public Action getAction() {
        return this.action;
    }

    public static UpdateNotePlayerPacket add(UUID uniqueId, String note) {
        return new UpdateNotePlayerPacket(uniqueId, note, -1, Action.ADD);
    }

    public static UpdateNotePlayerPacket remove(UUID uniqueId, int noteId) {
        return new UpdateNotePlayerPacket(uniqueId, null, noteId, Action.REMOVE);
    }

    public static UpdateNotePlayerPacket update(UUID uniqueId, int noteId, String note) {
        return new UpdateNotePlayerPacket(uniqueId, note, noteId, Action.UPDATE);
    }

    public static enum Action {
        ADD,
        REMOVE,
        UPDATE;

    }
}
