/*
 * Decompiled with CFR 0.152.
 */
package net.labymod.voice.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import net.labymod.voice.protocol.VoicePacket;
import net.labymod.voice.protocol.packet.KeepAlivePacket;
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
import net.labymod.voice.protocol.packet.server.moderation.MuteInfoPacket;
import net.labymod.voice.protocol.packet.server.moderation.PlayerUpdateMetaPacket;
import net.labymod.voice.protocol.packet.server.moderation.PlayerUpdateReportsPacket;
import net.labymod.voice.protocol.packet.server.moderation.WarnPacket;
import net.labymod.voice.protocol.packet.server.world.PlayerAlivePacket;
import net.labymod.voice.protocol.packet.server.world.PlayerDeadPacket;

public class PacketRegistry {
    private static final Map<Byte, Callable<? extends VoicePacket>> REGISTERED_PACKETS = new HashMap<Byte, Callable<? extends VoicePacket>>();
    private static final Map<Class<? extends VoicePacket>, Byte> PACKET_IDS = new HashMap<Class<? extends VoicePacket>, Byte>();

    private static void register(int id, Callable<? extends VoicePacket> callable) {
        try {
            REGISTERED_PACKETS.put((byte)id, callable);
            PACKET_IDS.put(callable.call().getClass(), (byte)id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isValidPacket(byte id) {
        return REGISTERED_PACKETS.containsKey(id);
    }

    public static VoicePacket createPacket(byte id) {
        try {
            Callable<? extends VoicePacket> callable = REGISTERED_PACKETS.get(id);
            if (callable == null) {
                return null;
            }
            return callable.call();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T extends VoicePacket> byte getPacketId(T packet) {
        Byte packetId = PACKET_IDS.get(packet.getClass());
        if (packetId == null) {
            System.out.println("Invalid packet " + packet.getClass().getSimpleName());
            return -1;
        }
        return packetId;
    }

    static {
        PacketRegistry.register(0, HandshakePacket::new);
        PacketRegistry.register(1, HandshakeResponsePacket::new);
        PacketRegistry.register(2, InvalidKeyPacket::new);
        PacketRegistry.register(3, KeepAlivePacket::new);
        PacketRegistry.register(4, KickPacket::new);
        PacketRegistry.register(5, MuteInfoPacket::new);
        PacketRegistry.register(6, MutePlayerPacket::new);
        PacketRegistry.register(7, UpdateVisiblePlayersPacket::new);
        PacketRegistry.register(8, ClientAudioPacket::new);
        PacketRegistry.register(9, ServerAudioPacket::new);
        PacketRegistry.register(10, PlayerAlivePacket::new);
        PacketRegistry.register(11, PlayerDeadPacket::new);
        PacketRegistry.register(12, SwitchServerPacket::new);
        PacketRegistry.register(13, ReportPlayerPacket::new);
        PacketRegistry.register(14, PlayerUpdateReportsPacket::new);
        PacketRegistry.register(23, UnmutePlayerPacket::new);
        PacketRegistry.register(24, PlayerUpdateMetaPacket::new);
        PacketRegistry.register(25, RequestPlayerMetaPacket::new);
        PacketRegistry.register(26, WarnPlayerPacket::new);
        PacketRegistry.register(27, WarnPacket::new);
        PacketRegistry.register(28, UpdateNotePlayerPacket::new);
        PacketRegistry.register(29, CreateChannelPacket::new);
        PacketRegistry.register(30, UpdateChannelPacket::new);
        PacketRegistry.register(31, DeleteChannelPacket::new);
        PacketRegistry.register(32, MoveUserToChannelPacket::new);
        PacketRegistry.register(33, UpdatePropertiesPacket::new);
        PacketRegistry.register(34, ChannelShowPacket::new);
        PacketRegistry.register(35, ChannelUpdatePacket::new);
        PacketRegistry.register(36, ChannelHidePacket::new);
        PacketRegistry.register(37, UserShowPacket::new);
        PacketRegistry.register(38, UserUpdatePacket::new);
        PacketRegistry.register(39, UserSwitchChannelPacket::new);
        PacketRegistry.register(40, UserHidePacket::new);
        PacketRegistry.register(41, ChannelAlertPacket::new);
        PacketRegistry.register(42, DisconnectPacket::new);
        PacketRegistry.register(43, SubscribeChannelsPacket::new);
        PacketRegistry.register(44, RequestChannelResetPacket::new);
        PacketRegistry.register(45, ChannelResetPacket::new);
    }
}
