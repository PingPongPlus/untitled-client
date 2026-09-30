package pingplus.voicechat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/** Immutable filters are shared safely with the chunk compilation workers. */
public final class XrayFeature {
    public enum Mineral {
        DIAMOND("Diamond", Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
        EMERALD("Emerald", Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE),
        DEBRIS("Ancient debris", Blocks.ANCIENT_DEBRIS),
        OTHER("Other ores", Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
                Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE);

        public final String label;
        private final Set<Block> blocks;
        Mineral(String label, Block... blocks) { this.label = label; this.blocks = Set.of(blocks); }
    }

    public record Filter(boolean enabled, Set<Block> blocks) {
        public boolean includes(BlockState state) { return blocks.contains(state.getBlock()); }
    }

    public interface FilteredRegion { Filter voicechat$xrayFilter(); }

    private static int selected = (1 << Mineral.values().length) - 1;
    private static volatile Filter filter = createFilter(false);

    public static Filter filter() { return filter; }
    public static boolean enabled() { return filter.enabled(); }
    public static boolean selected(Mineral mineral) { return (selected & (1 << mineral.ordinal())) != 0; }
    public static void toggle() { setEnabled(!enabled()); }
    public static void setEnabled(boolean enabled) {
        if (filter.enabled() == enabled) return;
        filter = createFilter(enabled);
        rebuild();
    }
    public static void toggle(Mineral mineral) {
        selected ^= 1 << mineral.ordinal();
        filter = createFilter(enabled());
        if (enabled()) rebuild();
    }

    private static Filter createFilter(boolean enabled) {
        var blocks = new HashSet<Block>();
        for (Mineral mineral : Mineral.values()) if (selected(mineral)) blocks.addAll(mineral.blocks);
        return new Filter(enabled, Set.copyOf(blocks));
    }
    private static void rebuild() {
        Minecraft client = Minecraft.getInstance();
        // The extractor also resets the dirty-section tracker before rebuilding geometry.
        if (client.level != null) client.levelExtractor.allChanged();
    }
    private XrayFeature() { }
}
