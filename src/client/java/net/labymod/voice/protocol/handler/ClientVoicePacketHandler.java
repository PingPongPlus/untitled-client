/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol.handler;

import net.labymod.voice.protocol.handler.VoicePacketHandler;
import net.labymod.voice.protocol.packet.client.DisconnectPacket;
import net.labymod.voice.protocol.packet.client.audio.ClientAudioPacket;
import net.labymod.voice.protocol.packet.client.auth.HandshakePacket;
import net.labymod.voice.protocol.packet.client.channel.CreateChannelPacket;
import net.labymod.voice.protocol.packet.client.channel.DeleteChannelPacket;
import net.labymod.voice.protocol.packet.client.channel.MoveUserToChannelPacket;
import net.labymod.voice.protocol.packet.client.channel.RequestChannelResetPacket;
import net.labymod.voice.protocol.packet.client.channel.SubscribeChannelsPacket;
import net.labymod.voice.protocol.packet.client.channel.UpdateChannelPacket;
import net.labymod.voice.protocol.packet.client.channel.UpdatePropertiesPacket;
import net.labymod.voice.protocol.packet.client.moderation.MutePlayerPacket;
import net.labymod.voice.protocol.packet.client.moderation.ReportPlayerPacket;
import net.labymod.voice.protocol.packet.client.moderation.RequestPlayerMetaPacket;
import net.labymod.voice.protocol.packet.client.moderation.UnmutePlayerPacket;
import net.labymod.voice.protocol.packet.client.moderation.UpdateNotePlayerPacket;
import net.labymod.voice.protocol.packet.client.moderation.WarnPlayerPacket;
import net.labymod.voice.protocol.packet.client.world.SwitchServerPacket;
import net.labymod.voice.protocol.packet.client.world.UpdateVisiblePlayersPacket;

public interface ClientVoicePacketHandler
extends VoicePacketHandler {
    public void handleClientAudio(ClientAudioPacket var1);

    public void handleHandshake(HandshakePacket var1);

    public void handleMutePlayer(MutePlayerPacket var1);

    public void handleRequestPlayerMeta(RequestPlayerMetaPacket var1);

    public void handleUnmutePlayer(UnmutePlayerPacket var1);

    public void handleWarnPlayer(WarnPlayerPacket var1);

    public void handleReportPlayer(ReportPlayerPacket var1);

    public void handleUpdateNotePlayer(UpdateNotePlayerPacket var1);

    public void handleSwitchServer(SwitchServerPacket var1);

    public void handleUpdateVisiblePlayers(UpdateVisiblePlayersPacket var1);

    public void handleSubscribe(SubscribeChannelsPacket var1);

    public void handleRequestChannelReset(RequestChannelResetPacket var1);

    public void handleMovePlayerToChannel(MoveUserToChannelPacket var1);

    public void handleCreateChannel(CreateChannelPacket var1);

    public void handleUpdateChannel(UpdateChannelPacket var1);

    public void handleDeleteChannel(DeleteChannelPacket var1);

    public void handleUpdateProperties(UpdatePropertiesPacket var1);

    public void handleDisconnect(DisconnectPacket var1);
}
