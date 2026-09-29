/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.type;

public enum EncryptType {
    ASYM,
    SYM,
    NONE;

    private static final EncryptType[] VALUES;

    public static EncryptType fromId(int id) {
        if (id < 0 || id >= VALUES.length) {
            throw new IllegalArgumentException("Invalid encrypt type id: " + id);
        }
        return VALUES[id];
    }

    public int getId() {
        return this.ordinal();
    }

    static {
        VALUES = EncryptType.values();
    }
}
