/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.util.properties;

public interface IKey {
    default public String createId() {
        String name = this.name();
        StringBuilder builder = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            upper = false;
        }
        return builder.toString();
    }

    public String name();

    public String getId();

    public Class<?> getType();

    public boolean isExposed();
}
