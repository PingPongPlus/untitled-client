package pingplus.voicechat.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import pingplus.voicechat.client.gui.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** Storage behind a solid wall, using the actual loaded block entities and gizmo renderer. */
public final class StorageEspGameTest {
    public static void run(ClientGameTestContext context) {
        boolean enabled = PlayerSettings.storageEsp, chests = PlayerSettings.storageEspChests,
                barrels = PlayerSettings.storageEspBarrels, shulkers = PlayerSettings.storageEspShulkers,
                enders = PlayerSettings.storageEspEnderChests, fill = PlayerSettings.storageEspFill;
        int range = PlayerSettings.storageEspRange;
        boolean xray = XrayFeature.enabled();
        boolean hiddenHud = context.computeOnClient(client -> client.gui.hud.isHidden());
        var positions = new AtomicReference<List<BlockPos>>();
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.gui.overlay() == null);
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                PlayerSettings.storageEsp = false;
                PlayerSettings.storageEspChests = PlayerSettings.storageEspBarrels = PlayerSettings.storageEspShulkers = PlayerSettings.storageEspEnderChests = true;
                PlayerSettings.storageEspFill = false; PlayerSettings.storageEspRange = 64;
                XrayFeature.setEnabled(false); StorageEspFeature.clear();
                if (client.gui.hud.isHidden()) client.gui.hud.toggle();
                client.getWindow().setWindowed(1440, 900); client.options.guiScale().set(2);
                var origin = client.player.blockPosition(); var id = client.player.getUUID();
                positions.set(java.util.stream.IntStream.of(-6, -3, 0, 3, 6, 11, 12, 48)
                        .mapToObj(x -> origin.offset(x, 1, 8)).toList());
                var server = client.getSingleplayerServer();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    player.teleportTo(server.overworld(), origin.getX() + .5, origin.getY(), origin.getZ() + .5,
                            java.util.Set.of(), 0, 0, false);
                    var states = List.of(Blocks.CHEST.defaultBlockState(), Blocks.TRAPPED_CHEST.defaultBlockState(),
                            Blocks.BARREL.defaultBlockState(), Blocks.DYED_SHULKER_BOX.purple().defaultBlockState(), Blocks.ENDER_CHEST.defaultBlockState(),
                            Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT),
                            Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT), Blocks.BARREL.defaultBlockState());
                    for (int i = 0; i < states.size(); i++) server.overworld().setBlockAndUpdate(positions.get().get(i), states.get(i));
                    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-15, 0, 5), origin.offset(15, 4, 5)))
                        server.overworld().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                });
            });
            context.waitFor(client -> positions.get().stream().allMatch(pos -> client.level.getBlockEntity(pos) != null));
            context.waitTicks(10);
            context.runOnClient(client -> {
                StorageEspFeature.refresh(client);
                check(StorageEspFeature.targets(client).isEmpty() && capture(client).isEmpty(), "disabled feature draws nothing");
                PlayerSettings.storageEsp = true;
                int loaded = client.level.getChunkSource().getLoadedChunksCount(); StorageEspFeature.refresh(client);
                check(client.level.getChunkSource().getLoadedChunksCount() == loaded, "scan does not load chunks");
                check(StorageEspFeature.targets(client).size() == 8, "all types and both double-chest halves detected");
                check(StorageEspFeature.targets(client).stream().filter(target -> target.kind() == StorageEspFeature.Kind.CHEST).count() == 4,
                        "normal, trapped and double chests share the chest filter");
                StorageEspFeature.setRange(16);
                check(StorageEspFeature.targets(client).size() == 7, "range excludes distant barrel");
                StorageEspFeature.Kind.CHEST.toggle();
                check(StorageEspFeature.targets(client).size() == 3, "chest filter excludes every chest variant");
                StorageEspFeature.Kind.BARREL.toggle(); StorageEspFeature.Kind.SHULKER.toggle(); StorageEspFeature.Kind.ENDER_CHEST.toggle();
                check(StorageEspFeature.targets(client).isEmpty(), "all filters off clears targets");
                for (var kind : StorageEspFeature.Kind.values()) kind.toggle();
                StorageEspFeature.setRange(-20); check(StorageEspFeature.range() == 16, "minimum range clamp");
                StorageEspFeature.setRange(999); check(StorageEspFeature.range() == 128, "maximum range clamp");
                StorageEspFeature.setRange(64);
                var draws = capture(client);
                check(draws.size() >= 4 && draws.stream().allMatch(SimpleGizmoCollector.GizmoInstance::isAlwaysOnTop), "storage uses the through-wall pass");
                check(draws.stream().allMatch(draw -> draw.gizmo() instanceof CuboidGizmo box && !box.style().hasFill()), "outline style starts without fill");
                PlayerSettings.storageEspFill = true;
                check(capture(client).stream().allMatch(draw -> ((CuboidGizmo)draw.gizmo()).style().hasFill()), "optional fill reaches renderer");
                PlayerSettings.storageEspFill = false;
            });
            context.waitTicks(10);
            context.runOnClient(client -> check(hasRenderedBoxes(client), "boxes reach the actual world renderer"));
            liveScreenshot(context, "storage-esp-through-wall", true);
            context.runOnClient(client -> PlayerSettings.storageEspFill = true);
            context.waitTicks(5); liveScreenshot(context, "storage-esp-filled-wall", true);
            context.runOnClient(client -> PlayerSettings.storageEspFill = false);
            context.runOnClient(client -> client.gui.hud.toggle());
            context.waitTicks(5); liveScreenshot(context, "storage-esp-hidden-hud", false);
            context.runOnClient(client -> client.gui.hud.toggle());
            context.runOnClient(client -> StorageEspFeature.toggle());
            context.runOnClient(client -> check(StorageEspFeature.targets(client).isEmpty() && capture(client).isEmpty(), "toggle off clears outlines immediately"));
            context.waitTicks(5); liveScreenshot(context, "storage-esp-disabled-wall", false);
            context.runOnClient(client -> StorageEspFeature.toggle());
            var removed = new StorageEspFeature.Target(positions.get().get(3), StorageEspFeature.Kind.SHULKER);
            context.runOnClient(client -> client.getSingleplayerServer().execute(() ->
                    client.getSingleplayerServer().overworld().setBlockAndUpdate(removed.pos(), Blocks.AIR.defaultBlockState())));
            context.waitFor(client -> client.level.getBlockState(removed.pos()).isAir());
            context.runOnClient(client -> {
                check(!StorageEspFeature.current(client, removed), "removed storage cannot leave a stale outline");
                StorageEspFeature.refresh(client); check(StorageEspFeature.targets(client).size() == 7, "scan evicts removed storage");
            });
            var fps = new FpsHud(); var coords = new CoordinatesHud(); var list = new ArraylistHud(fps, coords);
            context.setScreen(() -> new ClickGuiScreen(fps, VoicechatClient.openGuiKey(), coords, list));
            click(context, "Storage ESP options"); click(context, "Chests");
            context.runOnClient(client -> check(!PlayerSettings.storageEspChests && capture(client).isEmpty(), "menu filter works and menus suppress drawing"));
            click(context, "Chests"); click(context, "Filled boxes");
            context.runOnClient(client -> check(PlayerSettings.storageEspFill, "fill control toggles"));
            context.waitTicks(5); context.takeScreenshot("storage-esp-settings");
            click(context, "Storage ESP");
            context.runOnClient(client -> check(!PlayerSettings.storageEsp, "menu disables feature"));
            System.out.println("PASS: Storage ESP loaded storage, filters, range, removal, menu controls, and visible outlines/fill behind stone");
        } finally {
            context.runOnClient(client -> {
                PlayerSettings.storageEsp = enabled; PlayerSettings.storageEspChests = chests; PlayerSettings.storageEspBarrels = barrels;
                PlayerSettings.storageEspShulkers = shulkers; PlayerSettings.storageEspEnderChests = enders;
                PlayerSettings.storageEspFill = fill; PlayerSettings.storageEspRange = range;
                StorageEspFeature.clear(); XrayFeature.setEnabled(xray);
                if (client.gui.hud.isHidden() != hiddenHud) client.gui.hud.toggle();
            });
        }
    }
    private static List<SimpleGizmoCollector.GizmoInstance> capture(Minecraft client) {
        var collector = new SimpleGizmoCollector();
        try (var ignored = Gizmos.withCollector(collector)) {
            var camera = client.gameRenderer.mainCamera();
            var eye = camera.position();
            client.levelExtractor.debugRenderer.emitGizmos(camera.getCullFrustum(), eye.x, eye.y, eye.z, 0);
        }
        return collector.getGizmos();
    }
    private static boolean hasRenderedBoxes(Minecraft client) {
        try {
            var field = client.levelRenderer.getClass().getDeclaredField("finalizedGizmos"); field.setAccessible(true);
            var finalized = field.get(client.levelRenderer);
            var topField = finalized.getClass().getDeclaredField("alwaysOnTopPrimitives"); topField.setAccessible(true);
            var primitives = topField.get(finalized);
            var empty = primitives.getClass().getDeclaredField("isEmpty"); empty.setAccessible(true);
            return !empty.getBoolean(primitives);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
    /** Fabric's screenshot helper redraws without the per-frame gizmo collector; capture a normal frame instead. */
    private static void liveScreenshot(ClientGameTestContext context, String name, boolean expectBoxes) {
        var finished = context.computeOnClient(client -> {
            var future = new CompletableFuture<Void>();
            Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
                try (image) {
                    var path = client.gameDirectory.toPath().resolve("screenshots").resolve(name + ".png");
                    java.nio.file.Files.createDirectories(path.getParent()); image.writeToFile(path);
                    int[] pixels = image.getPixelsABGR();
                    for (var kind : StorageEspFeature.Kind.values()) {
                        int coloredPixels = 0;
                        for (int y = image.getHeight() / 3; y < image.getHeight() * 2 / 3; y++)
                            for (int x = 0; x < image.getWidth(); x++) {
                                int pixel = pixels[y * image.getWidth() + x];
                                int r = pixel & 255, g = pixel >> 8 & 255, b = pixel >> 16 & 255;
                                int expectedR = kind.color >> 16 & 255, expectedG = kind.color >> 8 & 255, expectedB = kind.color & 255;
                                // Vanilla's vignette dims the boxes near the sides of the frame.
                                double brightness = Math.max(r, Math.max(g, b)) / (double)Math.max(expectedR, Math.max(expectedG, expectedB));
                                if (brightness > .4 && brightness < 1.05
                                        && Math.abs(r - expectedR * brightness) < 8
                                        && Math.abs(g - expectedG * brightness) < 8
                                        && Math.abs(b - expectedB * brightness) < 8) coloredPixels++;
                            }
                        check(expectBoxes ? coloredPixels >= 30 : coloredPixels == 0,
                                kind.label + " visible pixels behind solid stone: " + coloredPixels);
                    }
                    future.complete(null);
                } catch (Throwable exception) { future.completeExceptionally(exception); }
            });
            return future;
        });
        context.waitFor(client -> finished.isDone()); finished.join();
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
    private StorageEspGameTest() { }
}
