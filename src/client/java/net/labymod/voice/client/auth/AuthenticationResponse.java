/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.client.auth;

import java.util.UUID;

public class AuthenticationResponse {
    private String string;
    private UUID player;

    private AuthenticationResponse() {
    }

    public AuthenticationResponse(String string, UUID player) {
        this.string = string;
        this.player = player;
    }

    public static AuthenticationResponse createLabyConnect(String pin, UUID uuid) {
        return new AuthenticationResponse(pin, uuid);
    }

    public static AuthenticationResponse createMojang(String username) {
        return new AuthenticationResponse(username, null);
    }

    public String getString() {
        return this.string;
    }

    public UUID getPlayer() {
        return this.player;
    }

    public String toString() {
        return "AuthenticationResponse(string=" + this.getString() + ", player=" + String.valueOf(this.getPlayer()) + ")";
    }
}
