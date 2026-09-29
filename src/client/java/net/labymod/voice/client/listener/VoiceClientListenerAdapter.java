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
import net.labymod.voice.client.listener.VoiceClientListener;
import net.labymod.voice.protocol.type.DisconnectType;
import net.labymod.voice.protocol.util.properties.ChannelProperties;
import net.labymod.voice.protocol.util.properties.UserProperties;

public class VoiceClientListenerAdapter
implements VoiceClientListener {
    @Override
    public void onDisconnected(DisconnectType type, String name) {
    }

    @Override
    public void onAuthenticated(boolean isStaff) {
    }

    @Override
    public void onUpdateMeta(UUID uniqueId, JsonObject meta) {
    }

    @Override
    public void onPlayerDiscovered(UUID uniqueId, JsonObject meta) {
    }

    @Override
    public void onPlayerDisappeared(UUID uniqueId) {
    }

    @Override
    public void onAudioReceived(UUID player, byte[] data) {
    }

    @Override
    public void onWarn(String reason) {
    }

    @Override
    public void onChannelShow(UUID channelId, ChannelProperties properties) {
    }

    @Override
    public void onChannelUpdate(UUID channelId, ChannelProperties properties) {
    }

    @Override
    public void onChannelHide(UUID channelId) {
    }

    @Override
    public void onChannelReset() {
    }

    @Override
    public void onUserShow(UUID userId, UUID channelId, UserProperties properties) {
    }

    @Override
    public void onUserSwitchChannel(UUID userId, UUID previousChannelId, UUID newChannelId, UserProperties properties) {
    }

    @Override
    public void onChannelUserUpdateProperties(UUID userId, UserProperties properties) {
    }

    @Override
    public void onUserHide(UUID userId, UUID channelId) {
    }

    @Override
    public void onChannelAlert(String messageId, List<String> arguments) {
    }
}
