/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.udp.session;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import net.labymod.voice.protocol.udp.session.NetworkSession;

public interface FrameProcessor {
    public void onFrameReceived(NetworkSession var1, byte[] var2) throws Exception;

    default public int sendToSocket(NetworkSession session, DatagramSocket socket, byte[] data) throws Exception {
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, session.getAddress());
        socket.send(datagramPacket);
        return datagramPacket.getLength();
    }
}
