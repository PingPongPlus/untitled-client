/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  org.jetbrains.annotations.NotNull
 */
package net.labymod.voice.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.labymod.voice.protocol.handler.VoicePacketHandler;
import net.labymod.voice.protocol.type.ConnectionState;
import net.labymod.voice.protocol.type.EncryptType;
import net.labymod.voice.protocol.type.TransportType;
import org.jetbrains.annotations.NotNull;

public abstract class VoicePacket<T extends VoicePacketHandler> {
    private static final byte[] NO_EXPOSED_IDENTIFIER = new byte[0];
    private static final Gson GSON = new Gson();
    private final EncryptType encryptType;
    private final ConnectionState allowedState;
    private final TransportType transportType;

    public VoicePacket(EncryptType encryptType, ConnectionState allowedState, TransportType transportType) {
        this.encryptType = encryptType;
        this.allowedState = allowedState;
        this.transportType = transportType;
    }

    public VoicePacket(EncryptType encryptType, ConnectionState allowedState) {
        this(encryptType, allowedState, TransportType.TCP);
    }

    public abstract void write(ByteArrayOutputStream var1, int var2) throws IOException;

    public abstract void read(ByteArrayInputStream var1, int var2) throws IOException;

    public abstract void handle(T var1);

    public static JsonElement readJson(ByteArrayInputStream buffer) throws IOException {
        return (JsonElement)GSON.fromJson(VoicePacket.readString(buffer), JsonElement.class);
    }

    public static void writeJson(JsonElement json, ByteArrayOutputStream buffer) throws IOException {
        VoicePacket.writeString(json.toString(), buffer);
    }

    public static void writeBoolean(boolean value, OutputStream buf) throws IOException {
        buf.write(value ? 1 : 0);
    }

    public static boolean readBoolean(InputStream buf) throws IOException {
        return buf.read() == 1;
    }

    public static void writeShort(short value, OutputStream buffer) throws IOException {
        buffer.write(value >> 8);
        buffer.write(value);
    }

    public static short readShort(InputStream buffer) throws IOException {
        return (short)(buffer.read() << 8 | buffer.read() & 0xFF);
    }

    public static long readVarLong(InputStream buf) throws IOException {
        byte b0;
        long i = 0L;
        int j = 0;
        do {
            b0 = (byte)buf.read();
            i |= (long)(b0 & 0x7F) << j++ * 7;
            if (j <= 10) continue;
            throw new RuntimeException("VarLong too big");
        } while ((b0 & 0x80) == 128);
        return i;
    }

    public static void writeVarLong(long value, OutputStream buf) throws IOException {
        while ((value & 0xFFFFFFFFFFFFFF80L) != 0L) {
            buf.write((int)(value & 0x7FL) | 0x80);
            value >>>= 7;
        }
        buf.write((int)value);
    }

    public static void writeVarInt(int value, OutputStream buf) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            buf.write(value & 0x7F | 0x80);
            value >>>= 7;
        }
        buf.write(value);
    }

    public static int readVarInt(InputStream buf) throws IOException {
        byte b0;
        int i = 0;
        int j = 0;
        do {
            b0 = (byte)buf.read();
            i |= (b0 & 0x7F) << j++ * 7;
            if (j <= 5) continue;
            throw new IndexOutOfBoundsException("VarInt too big");
        } while ((b0 & 0x80) == 128);
        return i;
    }

    public static void writeUUID(UUID uuid, OutputStream buf) throws IOException {
        VoicePacket.writeLong(uuid == null ? 0L : uuid.getMostSignificantBits(), buf);
        VoicePacket.writeLong(uuid == null ? 0L : uuid.getLeastSignificantBits(), buf);
    }

    @NotNull
    public static UUID readUUID(InputStream buf) throws IOException {
        return new UUID(VoicePacket.readLong(buf), VoicePacket.readLong(buf));
    }

    public static void writeEnum(Enum<?> e, OutputStream buf) throws IOException {
        int ord = e != null ? e.ordinal() : -1;
        buf.write(ord);
    }

    public static <T> T readEnum(InputStream buf, T[] values) throws IOException {
        int ord = buf.read();
        if (ord == -1) {
            return null;
        }
        if (values.length <= ord) {
            throw new RuntimeException("No Enum found");
        }
        return values[ord];
    }

    public static void writeString(String s, OutputStream buf) throws IOException {
        VoicePacket.writeString(s, StandardCharsets.UTF_8, buf);
    }

    public static void writeString(String s, Charset charset, OutputStream buf) throws IOException {
        if (s == null) {
            s = "";
        }
        byte[] b = s.getBytes(charset);
        buf.write(VoicePacket.intToBytes(b.length));
        buf.write(b);
    }

    public static String readString(InputStream buf) throws IOException {
        return VoicePacket.readString(StandardCharsets.UTF_8, buf);
    }

    public static String readString(Charset charset, InputStream buf) throws IOException {
        byte[] lenData = new byte[4];
        buf.read(lenData);
        int len = VoicePacket.byteArrayToInt(lenData);
        if (len < 0) {
            throw new IOException("String length is negative: " + len);
        }
        if (len > 1000000) {
            throw new IOException("String too long: " + len + " bytes");
        }
        byte[] b = new byte[len];
        buf.read(b);
        String s = new String(b, charset);
        return s.equals("") ? null : s;
    }

    public static void writeInt(int i, OutputStream buf) throws IOException {
        buf.write(VoicePacket.intToBytes(i));
    }

    public static int readInt(InputStream buf) throws IOException {
        byte[] data = new byte[4];
        buf.read(data);
        return VoicePacket.byteArrayToInt(data);
    }

    public static void writeLong(long value, OutputStream buf) throws IOException {
        byte[] bytes = new byte[8];
        int length = bytes.length;
        for (int i = 0; i < length; ++i) {
            bytes[length - i - 1] = (byte)(value & 0xFFL);
            value >>= 8;
        }
        buf.write(bytes);
    }

    public static long readLong(InputStream buf) throws IOException {
        byte[] data = new byte[8];
        buf.read(data);
        long value = 0L;
        for (byte b : data) {
            value = (value << 8) + (long)(b & 0xFF);
        }
        return value;
    }

    public static byte[] intToBytes(int value) {
        byte[] bytes = new byte[4];
        int length = bytes.length;
        for (int i = 0; i < length; ++i) {
            bytes[length - i - 1] = (byte)(value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }

    public static int byteArrayToInt(byte[] data) {
        if (data == null || data.length != 4) {
            return 0;
        }
        return (0xFF & data[0]) << 24 | (0xFF & data[1]) << 16 | (0xFF & data[2]) << 8 | 0xFF & data[3];
    }

    public ConnectionState getAllowedState() {
        return this.allowedState;
    }

    public EncryptType getEncryptType() {
        return this.encryptType;
    }

    public byte[] getExposedIdentifier() {
        return NO_EXPOSED_IDENTIFIER;
    }

    public TransportType getTransportType() {
        return this.transportType;
    }
}
