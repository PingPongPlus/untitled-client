/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.client.auth;

import net.labymod.voice.client.auth.AuthenticationResponse;

public interface Authenticator {
    public AuthenticationResponse request(String var1);
}
