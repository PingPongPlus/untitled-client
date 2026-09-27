package pingplus.voicechat.client;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pingplus.voicechat.client.damageglass.DamageGlassSettings;

/** Focused GPU regression run: JAVA_TOOL_OPTIONS=-Dvoicechat.damageGlassTest=true gradlew runClientGameTest. */
public final class DamageGlassRenderingTest implements FabricClientGameTest {
    private static LivingEntity mob, remote;
    private static boolean hold;
    private boolean hidden;
    public void runTest(ClientGameTestContext context) {
        boolean enabled = DamageGlassSettings.enabled(), players = DamageGlassSettings.players(), mobs = DamageGlassSettings.mobs();
        int reflectivity = DamageGlassSettings.reflectivity();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (hold && client.player != null) {
                for (var entity : new LivingEntity[]{mob, remote, client.player}) if (entity != null) {
                    entity.hurtDuration = 10;
                    entity.hurtTime = 7;
                }
            }
        });
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.gui.overlay() == null);
            context.runOnClient(client -> {
                hidden = client.gui.hud.isHidden();
                if (!hidden) client.gui.hud.toggle();
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.player.setYRot(0); client.player.setXRot(0);
                var pos = client.player.position();
                mob = EntityTypes.ZOMBIE.create(client.level, EntitySpawnReason.COMMAND);
                mob.setId(1999999); mob.setPos(pos.x - 1.3, pos.y, pos.z + 4);
                mob.setNoGravity(true); ((net.minecraft.world.entity.Mob) mob).setNoAi(true);
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                client.level.addEntity(mob);
                remote = new RemotePlayer(client.level, new GameProfile(UUID.randomUUID(), "GlassTest"));
                remote.setId(1999998); remote.setPos(pos.x + 1.3, pos.y, pos.z + 4);
                remote.setNoGravity(true);
                remote.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                client.level.addEntity(remote);
                hold = true;
            });
            context.setScreen(() -> null);
            for (int flags = 0; flags < 8; flags++) {
                final int f = flags;
                context.runOnClient(client -> {
                    set((f & 1) != 0, (f & 2) != 0, (f & 4) != 0);
                    var saved = new java.util.Properties();
                    try (var reader = java.nio.file.Files.newBufferedReader(net.fabricmc.loader.api.FabricLoader.getInstance()
                            .getConfigDir().resolve("voicechat-damage-glass.properties"))) { saved.load(reader); }
                    catch (java.io.IOException e) { throw new AssertionError("Cannot read damage glass settings", e); }
                    if (Boolean.parseBoolean(saved.getProperty("enabled")) != DamageGlassSettings.enabled()
                            || Boolean.parseBoolean(saved.getProperty("players")) != DamageGlassSettings.players()
                            || Boolean.parseBoolean(saved.getProperty("mobs")) != DamageGlassSettings.mobs())
                        throw new AssertionError("Damage glass settings not saved");
                });
                context.waitTicks(5);
                context.takeScreenshot("damage-glass-toggles-" + flags);
            }
            for (int percent : new int[]{0, 50, 100}) {
                context.runOnClient(client -> {
                    set(true, true, true);
                    DamageGlassSettings.setReflectivity(percent);
                    var saved = new java.util.Properties();
                    try (var reader = java.nio.file.Files.newBufferedReader(net.fabricmc.loader.api.FabricLoader.getInstance()
                            .getConfigDir().resolve("voicechat-damage-glass.properties"))) { saved.load(reader); }
                    catch (java.io.IOException e) { throw new AssertionError(e); }
                    if (Integer.parseInt(saved.getProperty("reflectivity")) != percent)
                        throw new AssertionError("Reflectivity was not saved");
                });
                context.waitTicks(5);
                context.takeScreenshot("damage-glass-reflectivity-" + percent);
            }
            context.runOnClient(client -> {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            });
            context.waitTicks(5);
            context.takeScreenshot("damage-glass-third-person");
            context.runOnClient(client -> { mob.setInvisible(true); remote.setInvisible(true); });
            context.waitTicks(5);
            context.takeScreenshot("damage-glass-invisible");
            context.runOnClient(client -> {
                mob.setInvisible(false); remote.setInvisible(false);
                hold = false;
                client.options.setCameraType(CameraType.FIRST_PERSON);
            });
            context.waitTicks(15);
            context.takeScreenshot("damage-glass-recovered");
            context.runOnClient(client -> hold = true);
            context.waitTicks(5);
            context.takeScreenshot("damage-glass-repeated-hit");
            context.runOnClient(client -> {
                var origin = client.player.blockPosition();
                for (int x = -3; x <= 3; x++) for (int y = 0; y <= 3; y++)
                    client.level.setBlock(origin.offset(x, y, 2), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            });
            context.waitTicks(15);
            context.takeScreenshot("damage-glass-wall-occlusion");
        } finally {
            hold = false; mob = remote = null;
            context.runOnClient(client -> {
                set(enabled, players, mobs); client.options.setCameraType(CameraType.FIRST_PERSON);
                DamageGlassSettings.setReflectivity(reflectivity);
                if (client.gui.hud.isHidden() != hidden) client.gui.hud.toggle();
            });
        }
    }
    private static void set(boolean enabled, boolean players, boolean mobs) {
        if (DamageGlassSettings.enabled() != enabled) DamageGlassSettings.toggleEnabled();
        if (DamageGlassSettings.players() != players) DamageGlassSettings.togglePlayers();
        if (DamageGlassSettings.mobs() != mobs) DamageGlassSettings.toggleMobs();
    }
}
