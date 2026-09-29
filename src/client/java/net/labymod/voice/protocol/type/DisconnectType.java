/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.type;

import net.labymod.voice.protocol.type.HandshakeResponse;

public enum DisconnectType {
    TIMEOUT,
    KICK,
    DISCONNECT,
    AUTHENTICATION_FAILED,
    ALREADY_CONNECTED,
    ERROR;


    public static DisconnectType of(HandshakeResponse response) {
        switch (response) {
            case AUTH_FAIL: {
                return AUTHENTICATION_FAILED;
            }
            case ALREADY_CONNECTED: {
                return ALREADY_CONNECTED;
            }
        }
        return DISCONNECT;
    }
}
