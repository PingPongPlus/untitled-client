/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.util.properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.util.properties.IKey;

public abstract class Properties<Key extends Enum<Key>> {
    protected final Map<Key, Object> properties = new EnumMap<Key, Object>(this.getEnumClass());

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setProperty(Key key, Object value) {
        if (value == null) {
            this.removeProperty(key);
        } else {
            Properties properties = this;
            synchronized (properties) {
                this.properties.put(key, value);
            }
        }
    }

    public <T> T getProperty(Key key) {
        return (T)this.properties.get(key);
    }

    public <T> T getProperty(Key key, T defaultValue) {
        return (T)this.properties.getOrDefault(key, defaultValue);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeProperty(Key key) {
        Properties properties = this;
        synchronized (properties) {
            this.properties.remove(key);
        }
    }

    public boolean hasProperty(Key key) {
        return this.properties.containsKey(key);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void write(OutputStream buffer) throws IOException {
        Properties properties = this;
        synchronized (properties) {
            VoicePacket.writeShort((short)this.properties.size(), buffer);
            for (Map.Entry<Key, Object> entry : this.properties.entrySet()) {
                VoicePacket.writeString(((IKey)((Object)((Enum)entry.getKey()))).getId(), buffer);
                VoicePacket.writeString(this.toString(entry.getValue()), buffer);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void writeToClient(OutputStream buffer) throws IOException {
        Properties properties = this;
        synchronized (properties) {
            Set<Map.Entry<Key, Object>> entries = this.properties.entrySet();
            int amount = 0;
            for (Map.Entry<Key, Object> entry : entries) {
                if (!this.shouldWriteEntryToClient(entry.getKey())) continue;
                ++amount;
            }
            VoicePacket.writeShort((short)amount, buffer);
            for (Map.Entry<Key, Object> entry : entries) {
                Key key = entry.getKey();
                if (!this.shouldWriteEntryToClient(key)) continue;
                this.writeEntryToClient(buffer, key, entry.getValue());
            }
        }
    }

    protected boolean shouldWriteEntryToClient(Key key) {
        return ((IKey)key).isExposed();
    }

    protected void writeEntryToClient(OutputStream buffer, Key key, Object value) throws IOException {
        VoicePacket.writeString(((IKey)key).getId(), buffer);
        VoicePacket.writeString(this.toString(value), buffer);
    }

    public void read(InputStream buffer) throws IOException {
        int propertiesSize = VoicePacket.readShort(buffer);
        this.properties.clear();
        for (int i = 0; i < propertiesSize; ++i) {
            String key = VoicePacket.readString(buffer);
            String value = VoicePacket.readString(buffer);
            Key keyType = this.toEnum(key);
            if (keyType == null) continue;
            this.properties.put(keyType, this.toObject(((IKey)keyType).getType(), value));
        }
    }

    public Iterable<? extends Map.Entry<Key, Object>> entrySet() {
        return this.properties.entrySet();
    }

    public String toString() {
        StringBuilder string = new StringBuilder();
        for (Map.Entry<Key, Object> entry : this.properties.entrySet()) {
            if (string.length() > 0) {
                string.append(", ");
            }
            string.append(entry.getKey()).append("=").append(this.toString(entry.getValue()));
        }
        return string.toString();
    }

    public String toString(Object obj) {
        if (obj instanceof List) {
            StringBuilder builder = new StringBuilder();
            for (Object value : (List)obj) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(this.toString(value));
            }
            String out = builder.toString();
            if (out.isEmpty()) {
                out = " ";
            }
            return out;
        }
        return obj.toString();
    }

    protected Object toObject(Class<?> type, String value) {
        if (type == String.class) {
            return value;
        }
        if (type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == Integer.class) {
            return Integer.parseInt(value);
        }
        if (type == Long.class) {
            return Long.parseLong(value);
        }
        if (type == Double.class) {
            return Double.parseDouble(value);
        }
        if (type == UUID.class) {
            return UUID.fromString(value);
        }
        if (type == List.class) {
            return new ArrayList(value == null ? Collections.emptyList() : Arrays.asList(value.split(",")));
        }
        return value;
    }

    protected abstract Key toEnum(String var1);

    protected abstract Class<Key> getEnumClass();
}
