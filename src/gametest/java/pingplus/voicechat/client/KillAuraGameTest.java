package pingplus.voicechat.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import java.util.concurrent.atomic.AtomicInteger;

/** Local-world combat regression; never connects to a multiplayer server. */
public final class KillAuraGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var id = new AtomicInteger();
        boolean enabled=PlayerSettings.killAura, players=PlayerSettings.killAuraPlayers, mobs=PlayerSettings.killAuraMobs;
        float range=PlayerSettings.killAuraRange, speed=PlayerSettings.killAuraTurnSpeed;
        try (var world=context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.gui.overlay() == null);
            context.setScreen(() -> null);
            context.waitTicks(20);
            context.runOnClient(client -> {
                PlayerSettings.killAura=false;
                PlayerSettings.killAuraPlayers=false;
                PlayerSettings.killAuraMobs=false;
                PlayerSettings.killAuraRange=3;
                PlayerSettings.killAuraTurnSpeed=180;
                client.player.setYRot(-90); client.player.setXRot(0);
                var position=client.player.position();
                var server=client.getSingleplayerServer();
                server.submit(() -> {
                    var mob=EntityTypes.COW.create(server.overworld(),EntitySpawnReason.COMMAND);
                    mob.setPos(position.x,position.y,position.z+2.1);
                    mob.setNoAi(true); mob.setNoGravity(true); mob.setPersistenceRequired();
                    server.overworld().addFreshEntity(mob);
                    id.set(mob.getId());
                });
            });
            context.waitFor(client -> client.level.getEntity(id.get()) instanceof LivingEntity);
            context.waitTicks(20);
            context.runOnClient(client -> {
                check(((LivingEntity)client.level.getEntity(id.get())).getHealth()==10, "disabled feature attacked");
                PlayerSettings.killAura=true;
            });
            context.waitTicks(20);
            context.runOnClient(client -> {
                check(((LivingEntity)client.level.getEntity(id.get())).getHealth()==10, "disabled mob filter attacked");
            });
            context.setScreen(() -> new Screen(Component.literal("Combat menu pause")) {
                @Override public boolean isPauseScreen() { return false; }
            });
            context.runOnClient(client -> PlayerSettings.killAuraMobs=true);
            context.waitTicks(20);
            context.runOnClient(client -> check(((LivingEntity)client.level.getEntity(id.get())).getHealth()==10,
                    "open menu did not suspend combat"));
            context.setScreen(() -> null);
            context.waitFor(client -> client.level.getEntity(id.get()) instanceof LivingEntity mob && mob.getHealth()<10);
            context.runOnClient(client -> {
                check(Math.abs(client.player.getYRot()+90)>10, "camera did not smoothly acquire the target");
                PlayerSettings.killAura=false;
                KillAuraFeature.clear();
            });
            context.takeScreenshot("killaura-local-combat");
        } finally {
            context.runOnClient(client -> {
                PlayerSettings.killAura=enabled; PlayerSettings.killAuraPlayers=players; PlayerSettings.killAuraMobs=mobs;
                PlayerSettings.killAuraRange=range; PlayerSettings.killAuraTurnSpeed=speed;
                KillAuraFeature.clear();
            });
            context.setScreen(TitleScreen::new);
        }
    }
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
