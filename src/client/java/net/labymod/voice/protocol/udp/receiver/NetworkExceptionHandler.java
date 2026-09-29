/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package net.labymod.voice.protocol.udp.receiver;

import net.labymod.voice.protocol.udp.session.NetworkSession;
import org.jetbrains.annotations.NotNull;

public interface NetworkExceptionHandler {
    public void handle(@NotNull NetworkSession var1, Throwable var2);
}
