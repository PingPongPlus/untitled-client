/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.udp.receiver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import net.labymod.voice.protocol.Encryption;
import net.labymod.voice.protocol.udp.receiver.NetworkExceptionHandler;
import net.labymod.voice.protocol.udp.receiver.NetworkSessionProvider;
import net.labymod.voice.protocol.udp.session.NetworkSession;

public class PacketReceiver
extends Thread {
    private final DatagramSocket socket;
    private final NetworkSessionProvider provider;
    private final NetworkExceptionHandler exceptionHandler;
    private Encryption asymmetricEncryption;
    private Cipher cipher;
    private boolean running = true;

    public PacketReceiver(DatagramSocket socket, NetworkSessionProvider provider, NetworkExceptionHandler exceptionHandler) {
        this.socket = socket;
        this.provider = provider;
        this.exceptionHandler = exceptionHandler;
        this.asymmetricEncryption = null;
        this.cipher = null;
        this.start();
    }

    public PacketReceiver(DatagramSocket socket, NetworkSessionProvider provider, NetworkExceptionHandler exceptionHandler, Encryption asymmetricEncryption) throws Exception {
        this.socket = socket;
        this.provider = provider;
        this.exceptionHandler = exceptionHandler;
        this.asymmetricEncryption = asymmetricEncryption;
        this.cipher = asymmetricEncryption.createDecryptCipher();
        this.start();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        while (this.running) {
            this.read(buffer);
        }
    }

    private void read(byte[] buffer) {
        try {
            DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
            this.socket.receive(packet);
            NetworkSession session = this.provider.get(packet);
            if (session != null) {
                try {
                    session.receiveSegment(this.socket, packet.getData(), packet.getOffset(), packet.getLength(), this.cipher);
                }
                catch (Throwable t) {
                    this.exceptionHandler.handle(session, t);
                    if (this.cipher != null && t instanceof GeneralSecurityException) {
                        this.cipher = this.asymmetricEncryption.createDecryptCipher();
                        System.out.println("Recreated cipher for packet receiver");
                    }
                }
            }
        }
        catch (SocketException t) {
            this.running = false;
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void kill() {
        this.running = false;
        this.interrupt();
    }
}
