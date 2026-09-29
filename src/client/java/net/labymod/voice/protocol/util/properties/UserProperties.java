/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.util.properties;

import net.labymod.voice.protocol.util.properties.IKey;
import net.labymod.voice.protocol.util.properties.Properties;

public class UserProperties
extends Properties<UserProperties.Key> {
    public void setName(String name) {
        this.setProperty(Key.NAME, name);
    }

    public String getName() {
        return (String)this.getProperty(Key.NAME);
    }

    public void setCountryCode(String countryCode) {
        this.setProperty(Key.COUNTRY_CODE, countryCode);
    }

    public String getCountryCode() {
        return (String)this.getProperty(Key.COUNTRY_CODE);
    }

    public void setInputMuted(boolean muted) {
        this.setProperty(Key.INPUT_MUTED, muted);
    }

    public boolean isInputMuted() {
        return this.getProperty(Key.INPUT_MUTED, false);
    }

    public void setOutputMuted(boolean muted) {
        this.setProperty(Key.OUTPUT_MUTED, muted);
    }

    public boolean isOutputMuted() {
        return this.getProperty(Key.OUTPUT_MUTED, false);
    }

    public void setInputDisabled(boolean disabled) {
        this.setProperty(Key.INPUT_DISABLED, disabled);
    }

    public boolean isInputDisabled() {
        return this.getProperty(Key.INPUT_DISABLED, false);
    }

    public void setOutputDisabled(boolean disabled) {
        this.setProperty(Key.OUTPUT_DISABLED, disabled);
    }

    public boolean isOutputDisabled() {
        return this.getProperty(Key.OUTPUT_DISABLED, false);
    }

    public UserProperties clone() {
        UserProperties clone = new UserProperties();
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

    public static enum Key implements IKey
    {
        NAME(String.class),
        COUNTRY_CODE(String.class),
        INPUT_MUTED(Boolean.class),
        OUTPUT_MUTED(Boolean.class),
        INPUT_DISABLED(Boolean.class),
        OUTPUT_DISABLED(Boolean.class);

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
