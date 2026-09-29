/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 */
package net.labymod.voice.client.listener;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.UUID;
import net.labymod.voice.protocol.type.DisconnectType;
import net.labymod.voice.protocol.util.properties.ChannelProperties;
import net.labymod.voice.protocol.util.properties.UserProperties;

public interface VoiceClientListener {
    public void onDisconnected(DisconnectType var1, String var2);

    public void onAuthenticated(boolean var1);

    public void onUpdateMeta(UUID var1, JsonObject var2);

    public void onPlayerDiscovered(UUID var1, JsonObject var2);

    public void onPlayerDisappeared(UUID var1);

    public void onAudioReceived(UUID var1, byte[] var2);

    public void onWarn(String var1);

    public void onChannelShow(UUID var1, ChannelProperties var2);

    public void onChannelUpdate(UUID var1, ChannelProperties var2);

    public void onChannelHide(UUID var1);

    public void onChannelReset();

    public void onUserShow(UUID var1, UUID var2, UserProperties var3);

    public void onUserSwitchChannel(UUID var1, UUID var2, UUID var3, UserProperties var4);

    public void onChannelUserUpdateProperties(UUID var1, UserProperties var2);

    public void onUserHide(UUID var1, UUID var2);

    public void onChannelAlert(String var1, List<String> var2);
}
