package pingplus.voicechat.client;

import com.mojang.blaze3d.vertex.VertexSorting;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import pingplus.voicechat.client.gui.*;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicReference;

/** Real mining input, section meshes and menu controls in an isolated local world. */
public final class MiningFeaturesGameTest {
    public static void run(ClientGameTestContext context) {
        boolean tools = PlayerSettings.autoTools, restore = PlayerSettings.autoToolsRestore;
        boolean xray = XrayFeature.enabled(), bright = PlayerSettings.fullbright;
        var filters = new EnumMap<XrayFeature.Mineral, Boolean>(XrayFeature.Mineral.class);
        for (var mineral : XrayFeature.Mineral.values()) filters.put(mineral, XrayFeature.selected(mineral));
        var mining = new AtomicReference<BlockPos>();
        var section = new AtomicReference<SectionPos>();
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.gui.overlay() == null);
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                PlayerSettings.autoTools = false; PlayerSettings.autoToolsRestore = true;
                XrayFeature.setEnabled(false); PlayerSettings.fullbright = false;
                for (var mineral : XrayFeature.Mineral.values()) if (!XrayFeature.selected(mineral)) XrayFeature.toggle(mineral);
                client.getWindow().setWindowed(1440, 900); client.options.guiScale().set(2);
                mining.set(client.player.blockPosition().offset(2, 0, 0));
                section.set(SectionPos.of(client.player.blockPosition().offset(32, 16, 0)));
                var id = client.player.getUUID(); var server = client.getSingleplayerServer();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    var items = new net.minecraft.world.item.Item[]{Items.STICK, Items.WOODEN_PICKAXE, Items.DIAMOND_PICKAXE,
                            Items.GOLDEN_PICKAXE, Items.IRON_AXE, Items.DIAMOND_SHOVEL, Items.SHEARS, Items.IRON_PICKAXE};
                    for (int slot = 0; slot < 9; slot++) player.getInventory().setItem(slot,
                            slot < items.length ? new ItemStack(items[slot]) : ItemStack.EMPTY);
                    server.overworld().setBlockAndUpdate(mining.get(), Blocks.DIAMOND_ORE.defaultBlockState());
                    BlockPos origin = section.get().origin();
                    for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(15, 15, 15)))
                        server.overworld().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                    server.overworld().setBlockAndUpdate(origin.offset(5, 7, 8), Blocks.DIAMOND_ORE.defaultBlockState());
                    server.overworld().setBlockAndUpdate(origin.offset(8, 7, 8), Blocks.GOLD_ORE.defaultBlockState());
                    server.overworld().setBlockAndUpdate(origin.offset(11, 7, 8), Blocks.ANCIENT_DEBRIS.defaultBlockState());
                });
            });
            context.waitFor(client -> client.player.getInventory().getItem(2).is(Items.DIAMOND_PICKAXE)
                    && client.player.getInventory().getItem(6).is(Items.SHEARS)
                    && client.level.getBlockState(section.get().origin().offset(11, 7, 8)).is(Blocks.ANCIENT_DEBRIS)
                    && client.level.getBlockState(section.get().origin().offset(15, 15, 15)).is(Blocks.STONE));
            context.runOnClient(client -> {
                var inventory = client.player.getInventory(); inventory.setSelectedSlot(0);
                check(AutoToolsFeature.bestSlot(inventory, Blocks.STONE.defaultBlockState()) == 3, "fastest stone tool");
                check(AutoToolsFeature.bestSlot(inventory, Blocks.DIAMOND_ORE.defaultBlockState()) == 2, "correct drops beat faster wrong-tier gold");
                check(AutoToolsFeature.bestSlot(inventory, Blocks.OAK_LOG.defaultBlockState()) == 4, "axe for wood");
                check(AutoToolsFeature.bestSlot(inventory, Blocks.DIRT.defaultBlockState()) == 5, "shovel for dirt");
                check(AutoToolsFeature.bestSlot(inventory, Blocks.COBWEB.defaultBlockState()) == 6, "shears for cobweb");
                var enchanted = inventory.getItem(7).copy();
                enchanted.enchant(client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), 5);
                inventory.setItem(7, enchanted);
                check(AutoToolsFeature.bestSlot(inventory, Blocks.STONE.defaultBlockState()) == 7, "Efficiency can beat a faster unenchanted tool");
                inventory.setItem(7, new ItemStack(Items.IRON_PICKAXE));
                inventory.setItem(8, new ItemStack(Items.DIAMOND_PICKAXE)); inventory.setSelectedSlot(8);
                check(AutoToolsFeature.bestSlot(inventory, Blocks.DIAMOND_ORE.defaultBlockState()) == 8, "equal speeds keep the current slot");
                inventory.setItem(8, ItemStack.EMPTY); inventory.setSelectedSlot(0);
                client.gameMode.startDestroyBlock(mining.get(), Direction.UP);
                check(inventory.getSelectedSlot() == 0, "disabled Auto Tools leaves slot alone");
                client.gameMode.stopDestroyBlock(); PlayerSettings.autoTools = true;
                client.gameMode.startDestroyBlock(mining.get(), Direction.UP);
                check(inventory.getSelectedSlot() == 2, "vanilla mining selects pickaxe before action");
                client.gameMode.continueDestroyBlock(mining.get(), Direction.UP);
                check(inventory.getSelectedSlot() == 2, "continuing mining keeps the tool");
                client.gameMode.stopDestroyBlock();
                check(inventory.getSelectedSlot() == 0, "release restores original slot");
                PlayerSettings.autoToolsRestore = false;
                client.gameMode.startDestroyBlock(mining.get(), Direction.UP); client.gameMode.stopDestroyBlock();
                check(inventory.getSelectedSlot() == 2, "optional restore off keeps tool");
                PlayerSettings.autoToolsRestore = true; inventory.setSelectedSlot(0);
                client.gameMode.startDestroyBlock(mining.get(), Direction.UP); inventory.setSelectedSlot(4);
                client.gameMode.continueDestroyBlock(mining.get(), Direction.UP);
                check(inventory.getSelectedSlot() == 4, "manual hotbar selection wins during mining");
                client.gameMode.stopDestroyBlock();
                check(inventory.getSelectedSlot() == 4, "manual slot is not restored over");
                inventory.setSelectedSlot(2); client.gameMode.startDestroyBlock(mining.get(), Direction.UP);
                inventory.setSelectedSlot(0); client.gameMode.continueDestroyBlock(mining.get(), Direction.UP);
                check(inventory.getSelectedSlot() == 0, "manual selection wins when mining started with the best tool already held");
                client.gameMode.stopDestroyBlock();
                PlayerSettings.autoTools = false;
            });
            context.setScreen(() -> new Screen(Component.literal("Mining menu")) { @Override public boolean isPauseScreen() { return false; } });
            context.runOnClient(client -> {
                PlayerSettings.autoTools = true; client.player.getInventory().setSelectedSlot(0);
                check(!AutoToolsFeature.mine(client, mining.get()) && client.player.getInventory().getSelectedSlot() == 0, "menus suspend selection");
                PlayerSettings.autoTools = false;
            });
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                int normal = vertices(client, section.get(), false);
                check(normal > 72, "normal stone shell renders");
                XrayFeature.setEnabled(true);
                int ores = vertices(client, section.get(), true);
                check(ores == 72, "three buried cubes render all six faces; terrain is hidden (normal=" + normal + ", ores=" + ores + ")");
                XrayFeature.toggle(XrayFeature.Mineral.OTHER);
                check(vertices(client, section.get(), true) == 48, "other-ore filter hides gold");
                XrayFeature.toggle(XrayFeature.Mineral.DIAMOND);
                check(vertices(client, section.get(), true) == 24, "diamond filter leaves ancient debris");
                XrayFeature.toggle(XrayFeature.Mineral.DEBRIS);
                check(vertices(client, section.get(), true) == 0, "no matching blocks produces an empty section");
                XrayFeature.setEnabled(false);
                check(vertices(client, section.get(), false) == normal, "disabling Xray restores original geometry");
                check(client.level.getBlockState(section.get().origin()).is(Blocks.STONE), "Xray does not change the world");
                for (var mineral : XrayFeature.Mineral.values()) if (!XrayFeature.selected(mineral)) XrayFeature.toggle(mineral);
                BlockPos origin = section.get().origin();
                client.player.setNoGravity(true); client.player.setYRot(0); client.player.setXRot(0);
                var id = client.player.getUUID(); var server = client.getSingleplayerServer();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    player.setNoGravity(true); player.teleportTo(server.overworld(), origin.getX() + 8.5,
                            origin.getY() + 6, origin.getZ() - 10, java.util.Set.of(), 0, 0, false);
                });
            });
            context.waitTicks(30); context.takeScreenshot("mining-normal-terrain");
            context.runOnClient(client -> XrayFeature.setEnabled(true));
            context.waitFor(client -> client.levelRenderer.isSectionCompiledAndVisible(section.get().origin()));
            context.waitTicks(10);
            context.runOnClient(client -> {
                var renderSection = client.levelRenderer.viewArea().getRenderSectionAt(section.get().origin());
                check(renderSection.getSectionMesh().hasRenderableLayers(), "ore section uploads renderable geometry");
                check(client.levelRenderer.visibleSections().contains(renderSection), "ore section is visible in the actual renderer");
            });
            context.takeScreenshot("mining-xray-ores");
            var fps = new FpsHud(); var coords = new CoordinatesHud(); var list = new ArraylistHud(fps, coords);
            context.setScreen(() -> new ClickGuiScreen(fps, VoicechatClient.openGuiKey(), coords, list));
            click(context, "Auto Tools"); click(context, "Auto Tools options"); click(context, "Xray options");
            context.runOnClient(client -> check(PlayerSettings.autoTools && XrayFeature.enabled(), "menu toggles enable modules"));
            click(context, "Restore slot");
            context.runOnClient(client -> check(!PlayerSettings.autoToolsRestore, "restore option toggles"));
            click(context, "Restore slot");
            context.waitTicks(5); context.takeScreenshot("mining-feature-settings");
            click(context, "Xray");
            context.runOnClient(client -> check(!XrayFeature.enabled(), "menu can disable Xray"));
        } finally {
            context.runOnClient(client -> {
                AutoToolsFeature.finish(client); PlayerSettings.autoTools = tools; PlayerSettings.autoToolsRestore = restore;
                XrayFeature.setEnabled(false);
                for (var mineral : XrayFeature.Mineral.values()) if (XrayFeature.selected(mineral) != filters.get(mineral)) XrayFeature.toggle(mineral);
                XrayFeature.setEnabled(xray); PlayerSettings.fullbright = bright;
            });
        }
    }

    private static int vertices(Minecraft client, SectionPos section, boolean openVisibility) {
        var models = client.getModelManager();
        var compiler = new SectionCompiler(true, true, models.getBlockStateModelSet(), models.getFluidStateModelSet(), client.getBlockColors());
        var region = new RenderRegionCache().createRegion(client.level, section.asLong());
        try (var buffers = new SectionBufferBuilderPack()) {
            var results = compiler.compile(section, region, VertexSorting.DISTANCE_TO_ORIGIN, buffers);
            try {
                if (openVisibility) for (Direction from : Direction.values()) for (Direction to : Direction.values())
                    check(results.visibilitySet.visibilityBetween(from, to), "Xray opens section visibility");
                return results.renderedLayers.values().stream().mapToInt(mesh -> mesh.drawState().vertexCount()).sum();
            } finally { results.release(); }
        }
    }
    private static void click(ClientGameTestContext context, String label) {
        context.runOnClient(client -> {
            var screen = client.gui.screen();
            var button = screen.children().stream().filter(child -> child instanceof Button).map(child -> (Button)child)
                    .filter(child -> child.getMessage().getString().equals(label)).findFirst().orElseThrow();
            var event = new MouseButtonEvent((button.getX() + 5) * ClickGuiScreen.UI_SCALE,
                    (button.getY() + 5) * ClickGuiScreen.UI_SCALE, new MouseButtonInfo(0, 0));
            check(screen.mouseClicked(event, false), "clicked " + label); screen.mouseReleased(event);
        });
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private MiningFeaturesGameTest() { }
}
