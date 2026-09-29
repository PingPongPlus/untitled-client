/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.util.properties;

public enum ChannelVisibility {
    SERVER,
    FRIENDS,
    PUBLIC;

    public static final ChannelVisibility[] VALUES;

    public static ChannelVisibility getByName(String name) {
        for (ChannelVisibility filter : VALUES) {
            if (!filter.name().equalsIgnoreCase(name)) continue;
            return filter;
        }
        return SERVER;
    }

    static {
        VALUES = ChannelVisibility.values();
    }
}
