/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.labymod.voice.protocol.udp.receiver;

import java.net.DatagramPacket;
import net.labymod.voice.protocol.udp.session.NetworkSession;
import org.jetbrains.annotations.Nullable;

public interface NetworkSessionProvider {
    @Nullable
    public NetworkSession get(DatagramPacket var1);
}
