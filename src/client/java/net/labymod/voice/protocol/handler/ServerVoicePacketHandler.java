/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.handler;

import net.labymod.voice.protocol.handler.VoicePacketHandler;
import net.labymod.voice.protocol.packet.server.audio.ServerAudioPacket;
import net.labymod.voice.protocol.packet.server.auth.HandshakeResponsePacket;
import net.labymod.voice.protocol.packet.server.auth.InvalidKeyPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelAlertPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelHidePacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelResetPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelShowPacket;
import net.labymod.voice.protocol.packet.server.channel.ChannelUpdatePacket;
import net.labymod.voice.protocol.packet.server.channel.UserHidePacket;
import net.labymod.voice.protocol.packet.server.channel.UserShowPacket;
import net.labymod.voice.protocol.packet.server.channel.UserSwitchChannelPacket;
import net.labymod.voice.protocol.packet.server.channel.UserUpdatePacket;
import net.labymod.voice.protocol.packet.server.moderation.KickPacket;
import net.labymod.voice.protocol.packet.server.moderation.PlayerUpdateMetaPacket;
import net.labymod.voice.protocol.packet.server.moderation.WarnPacket;
import net.labymod.voice.protocol.packet.server.world.PlayerAlivePacket;
import net.labymod.voice.protocol.packet.server.world.PlayerDeadPacket;

public interface ServerVoicePacketHandler
extends VoicePacketHandler {
    public void handleHandshakeResponse(HandshakeResponsePacket var1);

    public void handleInvalidKey(InvalidKeyPacket var1);

    public void handleKick(KickPacket var1);

    public void handleWarn(WarnPacket var1);

    public void handlePlayerAlive(PlayerAlivePacket var1);

    public void handlePlayerMetaUpdate(PlayerUpdateMetaPacket var1);

    public void handlePlayerDead(PlayerDeadPacket var1);

    public void handleServerAudio(ServerAudioPacket var1);

    public void handleChannelShow(ChannelShowPacket var1);

    public void handleChannelUpdate(ChannelUpdatePacket var1);

    public void handleChannelHide(ChannelHidePacket var1);

    public void handleChannelReset(ChannelResetPacket var1);

    public void handleUserShow(UserShowPacket var1);

    public void handleUserSwitchChannel(UserSwitchChannelPacket var1);

    public void handleUserUpdateProperties(UserUpdatePacket var1);

    public void handleUserHide(UserHidePacket var1);

    public void handleChannelAlert(ChannelAlertPacket var1);
}
