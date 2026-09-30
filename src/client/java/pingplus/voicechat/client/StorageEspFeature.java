package pingplus.voicechat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Scans block entities in already loaded chunks, never the individual terrain blocks. */
public final class StorageEspFeature {
    public enum Kind {
        CHEST("Chests", 0xFFFFC65B), BARREL("Barrels", 0xFFFF9955),
        SHULKER("Shulker boxes", 0xFFC68CFF), ENDER_CHEST("Ender chests", 0xFF55DDE0);
        public final String label;
        public final int color;
        Kind(String label, int color) { this.label = label; this.color = color; }
        public boolean enabled() {
            return switch (this) {
                case CHEST -> PlayerSettings.storageEspChests;
                case BARREL -> PlayerSettings.storageEspBarrels;
                case SHULKER -> PlayerSettings.storageEspShulkers;
                case ENDER_CHEST -> PlayerSettings.storageEspEnderChests;
            };
        }
        public void toggle() {
            switch (this) {
                case CHEST -> PlayerSettings.storageEspChests = !PlayerSettings.storageEspChests;
                case BARREL -> PlayerSettings.storageEspBarrels = !PlayerSettings.storageEspBarrels;
                case SHULKER -> PlayerSettings.storageEspShulkers = !PlayerSettings.storageEspShulkers;
                case ENDER_CHEST -> PlayerSettings.storageEspEnderChests = !PlayerSettings.storageEspEnderChests;
            }
            refresh(Minecraft.getInstance());
        }
    }

    public record Target(BlockPos pos, Kind kind) {
        public AABB bounds() { return new AABB(pos).inflate(.002); }
    }
    private static ClientLevel owner;
    private static volatile List<Target> targets = List.of();
    private static int ticks;

    public static int range() { return Math.clamp(PlayerSettings.storageEspRange, 16, 128); }
    public static void setRange(int range) {
        PlayerSettings.storageEspRange = Math.clamp(range, 16, 128);
        refresh(Minecraft.getInstance());
    }
    public static void toggle() {
        PlayerSettings.storageEsp = !PlayerSettings.storageEsp;
        refresh(Minecraft.getInstance());
    }
    public static void tick(Minecraft client) {
        if (!PlayerSettings.storageEsp || client.level == null || client.player == null) { clear(); return; }
        if (owner != client.level || ticks++ % 5 == 0) refresh(client);
    }
    public static void refresh(Minecraft client) {
        if (!PlayerSettings.storageEsp || client.level == null || client.player == null) { clear(); return; }
        owner = client.level;
        var found = new ArrayList<Target>();
        var eye = client.player.getEyePosition();
        int range = range();
        int minX = SectionPos.blockToSectionCoord((int)Math.floor(eye.x - range));
        int maxX = SectionPos.blockToSectionCoord((int)Math.floor(eye.x + range));
        int minZ = SectionPos.blockToSectionCoord((int)Math.floor(eye.z - range));
        int maxZ = SectionPos.blockToSectionCoord((int)Math.floor(eye.z + range));
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            var chunk = client.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            for (var entity : chunk.getBlockEntities().values()) {
                if (entity.isRemoved()) continue;
                BlockPos pos = entity.getBlockPos();
                Kind kind = kind(entity.getBlockState());
                if (kind != null && kind.enabled() && Vec3.atCenterOf(pos).distanceToSqr(eye) <= range * range)
                    found.add(new Target(pos.immutable(), kind));
            }
        }
        targets = List.copyOf(found);
    }
    public static List<Target> targets(Minecraft client) {
        return PlayerSettings.storageEsp && owner == client.level ? targets : List.of();
    }
    public static boolean current(Minecraft client, Target target) {
        return client.level != null && client.player != null && target.kind.enabled()
                && kind(client.level.getBlockState(target.pos)) == target.kind
                && Vec3.atCenterOf(target.pos).distanceToSqr(client.player.getEyePosition()) <= range() * range();
    }
    public static Kind kind(BlockState state) {
        if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) return Kind.CHEST;
        if (state.is(Blocks.BARREL)) return Kind.BARREL;
        if (state.getBlock() instanceof ShulkerBoxBlock) return Kind.SHULKER;
        if (state.is(Blocks.ENDER_CHEST)) return Kind.ENDER_CHEST;
        return null;
    }
    public static void clear() { targets = List.of(); owner = null; ticks = 0; }
    private StorageEspFeature() { }
}
