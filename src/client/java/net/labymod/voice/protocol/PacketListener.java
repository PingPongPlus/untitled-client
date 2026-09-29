/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol;

import java.net.DatagramPacket;
import net.labymod.voice.protocol.VoicePacket;

public interface PacketListener<T extends VoicePacket> {
    public void handle(T var1, DatagramPacket var2);
}
