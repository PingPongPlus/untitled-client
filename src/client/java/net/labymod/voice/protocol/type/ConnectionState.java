/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.type;

public enum ConnectionState {
    HANDSHAKE((byte)0),
    CONNECTED((byte)1),
    DISCONNECTED((byte)2);

    private final byte id;

    private ConnectionState(byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }
}
