/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.handler;

import net.labymod.voice.protocol.packet.KeepAlivePacket;

public interface VoicePacketHandler {
    public void handleKeepAlive(KeepAlivePacket var1);
}
