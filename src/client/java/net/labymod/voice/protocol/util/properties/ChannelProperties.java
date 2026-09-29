/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package net.labymod.voice.protocol.util.properties;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.labymod.voice.protocol.util.properties.ChannelVisibility;
import net.labymod.voice.protocol.util.properties.IKey;
import net.labymod.voice.protocol.util.properties.Properties;
import org.jetbrains.annotations.NotNull;

public class ChannelProperties
extends Properties<ChannelProperties.Key> {
    public static final String PWD_HIDDEN = "hidden";

    public void setOwner(UUID owner) {
        this.setProperty(Key.OWNER, owner);
    }

    public UUID getOwner() {
        return (UUID)this.getProperty(Key.OWNER);
    }

    public void setPassword(String hash) {
        this.setProperty(Key.PASSWORD, hash);
    }

    public void setPasswordPlain(String plain) {
        this.setPassword(this.sha1(plain));
    }

    public boolean isPassword(String plain) {
        if (!this.hasPassword()) {
            return true;
        }
        return this.getProperty(Key.PASSWORD).equals(this.sha1(plain));
    }

    public boolean hasPassword() {
        return this.hasProperty(Key.PASSWORD) && !((String)this.getProperty(Key.PASSWORD)).isEmpty();
    }

    public void setCreationTime(long time) {
        this.setProperty(Key.CREATION_TIME, time);
    }

    public long getCreationTime() {
        return this.getProperty(Key.CREATION_TIME, 0L);
    }

    public void setServerAddress(String address) {
        this.setProperty(Key.SERVER_ADDRESS, address);
    }

    public String getServerAddress() {
        return (String)this.getProperty(Key.SERVER_ADDRESS);
    }

    public void setVisibility(@NotNull ChannelVisibility visibility) {
        this.setProperty(Key.VISIBILITY, (Object)visibility);
    }

    @NotNull
    public ChannelVisibility getVisibility() {
        return this.getProperty(Key.VISIBILITY, ChannelVisibility.SERVER);
    }

    public void setIndirectFriends(boolean indirectFriends) {
        this.setProperty(Key.INDIRECT_FRIENDS, indirectFriends);
    }

    public boolean isIndirectFriends() {
        return this.getProperty(Key.INDIRECT_FRIENDS, true);
    }

    public void setProximity(boolean proximity) {
        this.setProperty(Key.PROXIMITY, proximity);
    }

    public boolean isProximity() {
        return this.getProperty(Key.PROXIMITY, false);
    }

    public void setTalkPower(boolean talkPower) {
        this.setProperty(Key.TALK_POWER, talkPower);
    }

    public boolean hasTalkPower() {
        return this.getProperty(Key.TALK_POWER, true);
    }

    public boolean canTalk(UUID userId) {
        return this.hasTalkPower() || this.getOwner().equals(userId) || this.hasProperty(Key.TALKERS) && ((List)this.getProperty(Key.TALKERS)).contains(userId.toString());
    }

    public void addTalker(UUID userId) {
        if (this.canTalk(userId)) {
            return;
        }
        List talkers = this.getProperty(Key.TALKERS, new ArrayList());
        talkers.add(userId.toString());
        this.setProperty(Key.TALKERS, talkers);
    }

    public void removeTalker(UUID userId) {
        if (!this.canTalk(userId)) {
            return;
        }
        List talkers = this.getProperty(Key.TALKERS, new ArrayList());
        talkers.remove(userId.toString());
        this.setProperty(Key.TALKERS, talkers);
    }

    public boolean isBanned(UUID userId) {
        return this.hasProperty(Key.BANNED) && ((List)this.getProperty(Key.BANNED)).contains(userId.toString());
    }

    public void ban(UUID userId) {
        if (this.isBanned(userId)) {
            return;
        }
        List bannedUsers = this.getProperty(Key.BANNED, new ArrayList());
        bannedUsers.add(userId.toString());
        this.setProperty(Key.BANNED, bannedUsers);
    }

    public void unban(UUID userId) {
        if (!this.isBanned(userId)) {
            return;
        }
        List bannedUsers = this.getProperty(Key.BANNED, new ArrayList());
        bannedUsers.remove(userId.toString());
        this.setProperty(Key.BANNED, bannedUsers);
    }

    public void setName(String name) {
        this.setProperty(Key.NAME, name);
    }

    public String getName() {
        return (String)this.getProperty(Key.NAME);
    }

    private String sha1(String plain) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return plain;
        }
    }

    public ChannelProperties clone() {
        ChannelProperties clone = new ChannelProperties();
        clone.properties.putAll(this.properties);
        return clone;
    }

    @Override
    protected Key toEnum(String key) {
        return Key.fromId(key);
    }

    @Override
    protected Class<Key> getEnumClass() {
        return Key.class;
    }

    @Override
    protected Object toObject(Class<?> type, String value) {
        if (type == ChannelVisibility.class) {
            return ChannelVisibility.valueOf(value);
        }
        return super.toObject(type, value);
    }

    @Override
    protected void writeEntryToClient(OutputStream buffer, Key key, Object value) throws IOException {
        if (key == Key.PASSWORD && this.hasPassword()) {
            value = PWD_HIDDEN;
        }
        super.writeEntryToClient(buffer, key, value);
    }

    public static enum Key implements IKey
    {
        OWNER(UUID.class),
        PASSWORD(String.class),
        CREATION_TIME(Long.class),
        SERVER_ADDRESS(String.class),
        VISIBILITY(ChannelVisibility.class),
        INDIRECT_FRIENDS(Boolean.class),
        PROXIMITY(Boolean.class),
        TALK_POWER(Boolean.class),
        TALKERS(List.class),
        BANNED(List.class),
        NAME(String.class);

        public static final Key[] VALUES;
        private final String id = this.createId();
        private final Class<?> type;
        private final boolean exposed;

        private Key(Class<?> type) {
            this(type, true);
        }

        private Key(Class<?> type, boolean exposed) {
            this.type = type;
            this.exposed = exposed;
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public Class<?> getType() {
            return this.type;
        }

        @Override
        public boolean isExposed() {
            return this.exposed;
        }

        public static Key fromId(String id) {
            for (Key key : VALUES) {
                if (!key.id.equals(id)) continue;
                return key;
            }
            return null;
        }

        static {
            VALUES = Key.values();
        }
    }
}
