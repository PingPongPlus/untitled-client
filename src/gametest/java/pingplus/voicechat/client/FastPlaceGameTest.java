package pingplus.voicechat.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import pingplus.voicechat.client.gui.*;
import pingplus.voicechat.mixin.test.MinecraftUseAccessor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the real vanilla use path and placement prediction in an isolated world. */
public final class FastPlaceGameTest {
    public static void run(ClientGameTestContext context) {
        boolean originalEnabled = PlayerSettings.fastPlace;
        int originalDelay = PlayerSettings.fastPlaceDelayTicks;
        var anchors = new AtomicReference<List<BlockPos>>();
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.gui.overlay() == null);
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                PlayerSettings.fastPlace = false; PlayerSettings.fastPlaceDelayTicks = 1;
                client.getWindow().setWindowed(1440, 900); client.options.guiScale().set(2);
                var origin = client.player.blockPosition();
                var positions = java.util.stream.IntStream.rangeClosed(-2, 2).mapToObj(z -> origin.offset(2, -1, z)).toList();
                anchors.set(positions);
                var server = client.getSingleplayerServer();
                server.execute(() -> {
                    for (var anchor : positions) {
                        server.overworld().setBlockAndUpdate(anchor, Blocks.STONE.defaultBlockState());
                        server.overworld().setBlockAndUpdate(anchor.above(), Blocks.AIR.defaultBlockState());
                    }
                    server.overworld().setBlockAndUpdate(positions.get(3).above(), Blocks.STONE.defaultBlockState());
                    server.overworld().setBlockAndUpdate(positions.get(4), Blocks.CRAFTING_TABLE.defaultBlockState());
                });
            });
            context.waitFor(client -> anchors.get().stream().limit(4).allMatch(pos -> client.level.getBlockState(pos).is(Blocks.STONE))
                    && client.level.getBlockState(anchors.get().get(4)).is(Blocks.CRAFTING_TABLE));
            equip(context, Items.STONE, null);
            context.runOnClient(client -> {
                use(client, anchors.get().get(0));
                check(client.level.getBlockState(anchors.get().get(0).above()).is(Blocks.STONE), "disabled feature keeps normal placement");
                check(delay(client) == 4, "disabled feature keeps vanilla four-tick delay");
                PlayerSettings.fastPlace = true;
                use(client, anchors.get().get(1));
                check(client.level.getBlockState(anchors.get().get(1).above()).is(Blocks.STONE), "enabled block placement succeeds");
                check(delay(client) == 1, "enabled feature sets one-tick delay");
                PlayerSettings.fastPlaceDelayTicks = 2;
                // A fresh, valid face on the block just placed supplies another placement.
                use(client, anchors.get().get(1).above());
                check(client.level.getBlockState(anchors.get().get(1).above(2)).is(Blocks.STONE), "repeat placement succeeds");
                check(delay(client) == 2, "configured delay is respected");
                PlayerSettings.fastPlaceDelayTicks = 1;
                use(client, anchors.get().get(3));
                check(delay(client) == 4, "failed placement retains vanilla delay");
            });
            equip(context, Items.DIAMOND_PICKAXE, Items.STONE);
            context.runOnClient(client -> {
                use(client, anchors.get().get(2));
                check(client.level.getBlockState(anchors.get().get(2).above()).is(Blocks.STONE), "offhand places behind a non-block main hand");
                check(delay(client) == 1, "actual offhand placement gets fast delay");
            });
            equip(context, Items.STICK, null);
            context.runOnClient(client -> {
                use(client, anchors.get().get(0));
                check(delay(client) == 4, "non-block items retain vanilla delay");
                PlayerSettings.fastPlaceDelayTicks = -99;
                check(FastPlaceFeature.delayTicks() == 1, "delay clamps to one tick");
                PlayerSettings.fastPlaceDelayTicks = 99;
                check(FastPlaceFeature.delayTicks() == 4, "delay clamps to vanilla maximum");
                PlayerSettings.fastPlaceDelayTicks = 1;
            });
            equip(context, Items.STONE, null);
            context.runOnClient(client -> {
                use(client, anchors.get().get(4));
                check(delay(client) == 4, "opening a crafting table with a block item retains vanilla delay");
            });
            context.waitForScreen(CraftingScreen.class);
            context.runOnClient(client -> client.gui.screen().onClose());
            var fps = new FpsHud(); var coords = new CoordinatesHud(); var arraylist = new ArraylistHud(fps, coords);
            context.setScreen(() -> new ClickGuiScreen(fps, VoicechatClient.openGuiKey(), coords, arraylist));
            context.waitTicks(5);
            click(context, "Fast Place");
            context.runOnClient(client -> check(!PlayerSettings.fastPlace, "Fast Place toggle disables"));
            click(context, "Fast Place"); click(context, "Fast Place options");
            context.runOnClient(client -> {
                check(PlayerSettings.fastPlace, "Fast Place toggle enables");
                check(client.gui.screen().children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.AbstractSliderButton)
                        .anyMatch(c -> ((net.minecraft.client.gui.components.AbstractSliderButton)c).getMessage().getString().startsWith("Place delay")), "placement slider expands");
                check(!FastPlaceFeature.active(client, client.player.getMainHandItem()), "menus suspend acceleration");
            });
            context.waitTicks(5); context.takeScreenshot("fast-place-settings");
        } finally {
            context.runOnClient(client -> { PlayerSettings.fastPlace = originalEnabled; PlayerSettings.fastPlaceDelayTicks = originalDelay; });
        }
    }
    private static void equip(ClientGameTestContext context, Item main, Item off) {
        context.runOnClient(client -> {
            var id = client.player.getUUID(); var server = client.getSingleplayerServer();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(id);
                player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(main, main == Items.STONE ? 64 : 1));
                player.setItemSlot(EquipmentSlot.OFFHAND, off == null ? ItemStack.EMPTY : new ItemStack(off, 64));
            });
        });
        context.waitFor(client -> client.player.getMainHandItem().is(main)
                && (off == null ? client.player.getOffhandItem().isEmpty() : client.player.getOffhandItem().is(off)));
    }
    private static void use(Minecraft client, BlockPos anchor) {
        client.hitResult = new BlockHitResult(Vec3.atCenterOf(anchor).add(0, .5, 0), Direction.UP, anchor, false);
        ((MinecraftUseAccessor)client).fastPlaceTest$use();
    }
    private static int delay(Minecraft client) { return ((MinecraftUseAccessor)client).fastPlaceTest$delay(); }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void click(ClientGameTestContext context, String label) {
        context.runOnClient(client -> {
            var screen = client.gui.screen();
            var button = screen.children().stream().filter(child -> child instanceof Button).map(child -> (Button)child)
                    .filter(child -> child.getMessage().getString().equals(label)).findFirst().orElseThrow();
            var down = new MouseButtonEvent((button.getX() + 5) * ClickGuiScreen.UI_SCALE,
                    (button.getY() + 5) * ClickGuiScreen.UI_SCALE, new MouseButtonInfo(0, 0));
            check(screen.mouseClicked(down, false), "clicked " + label); screen.mouseReleased(down);
        });
    }
}
