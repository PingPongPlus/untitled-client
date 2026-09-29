/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.udp.session;

public enum NetworkVersion {
    UNIDENTIFIED,
    V1,
    V2,
    V3;

    public static final NetworkVersion[] VALUES;

    public boolean isOrGreater(NetworkVersion version) {
        return this.ordinal() >= version.ordinal();
    }

    public boolean isOrOlder(NetworkVersion version) {
        return this.ordinal() <= version.ordinal();
    }

    static {
        VALUES = NetworkVersion.values();
    }
}
