package pingplus.voicechat.client;

import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.level.GameType;
import pingplus.voicechat.client.gui.ClickGuiScreen;
import pingplus.voicechat.client.hud.ArraylistHud;
import pingplus.voicechat.client.hud.CoordinatesHud;
import pingplus.voicechat.client.hud.FpsHud;

/** Real input and server receipt with gameplay, inventory, chat, and control screens. */
final class ForceSneakGameTest {
    static void run(ClientGameTestContext context) {
        boolean original=PlayerSettings.forceSneak;
        try (var world=context.worldBuilder().create()) {
            context.waitFor(client -> client.player!=null && client.gui.overlay()==null);
            context.setScreen(() -> null);
            var id=context.computeOnClient(client -> client.player.getUUID());
            context.runOnClient(client -> {
                PlayerSettings.forceSneak=false;
                client.getWindow().setWindowed(1440,900);
                client.options.guiScale().set(2); client.resizeGui();
                client.options.keyShift.setDown(false);
                var server=client.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayer(id).setGameMode(GameType.SURVIVAL));
            });
            context.waitFor(client -> client.gameMode.getPlayerMode()==GameType.SURVIVAL && client.player.onGround());
            awaitSneak(context,id,false);

            controls(context); press(context,"Force Sneak");
            context.runOnClient(client -> check(PlayerSettings.forceSneak,"Force Sneak menu toggle did not enable"));
            awaitSneak(context,id,true);
            context.takeScreenshot("force-sneak-controls");
            context.setScreen(() -> null);
            awaitSneak(context,id,true);
            context.runOnClient(client -> check(client.player.isCrouching(),"Gameplay did not crouch"));

            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            awaitSneak(context,id,true); context.waitTicks(10);
            context.runOnClient(client -> {
                check(client.gui.screen() instanceof InventoryScreen,"Inventory unexpectedly closed");
                check(client.player.isCrouching(),"Inventory cleared the crouching pose");
                check(!client.options.keyShift.isDown(),"Force Sneak changed the real Sneak key state");
                var keys=client.player.input.keyPresses;
                check(!keys.forward() && !keys.backward() && !keys.left() && !keys.right() && !keys.jump(),
                        "Inventory acquired unrelated movement input");
            });
            context.takeScreenshot("force-sneak-inventory");
            context.setScreen(() -> new ChatScreen("",false));
            awaitSneak(context,id,true); context.waitTicks(5);
            context.runOnClient(client -> check(client.player.isCrouching(),"Chat cleared the crouching pose"));

            controls(context); press(context,"Force Sneak");
            awaitSneak(context,id,false);
            context.runOnClient(client -> check(!PlayerSettings.forceSneak && !client.player.isCrouching(),"Disable left forced sneak active"));
            context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
            awaitSneak(context,id,false);
            context.runOnClient(client -> check(!client.player.isCrouching(),"Disabled Force Sneak still crouched in inventory"));

            context.setScreen(() -> null);
            context.getInput().holdKey(options -> options.keyShift);
            awaitSneak(context,id,true);
            context.runOnClient(client -> PlayerSettings.forceSneak=true);
            context.waitTicks(3);
            context.runOnClient(client -> PlayerSettings.forceSneak=false);
            awaitSneak(context,id,true);
            context.getInput().releaseKey(options -> options.keyShift);
            awaitSneak(context,id,false);

            context.runOnClient(client -> {
                PlayerSettings.forceSneak=true;
                var server=client.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayer(id).setGameMode(GameType.SPECTATOR));
            });
            context.waitFor(client -> client.gameMode.getPlayerMode()==GameType.SPECTATOR);
            awaitSneak(context,id,false);
            context.runOnClient(client -> {
                var server=client.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayer(id).setGameMode(GameType.SURVIVAL));
            });
            context.waitFor(client -> client.gameMode.getPlayerMode()==GameType.SURVIVAL);
            awaitSneak(context,id,true);
            ClickGuiAllOffGameTest.run(context);
            System.out.println("PASS: Force Sneak client/server input, inventory/chat/control screens, release, held manual key, spectators and All off");
        } finally {
            context.getInput().releaseKey(options -> options.keyShift);
            context.runOnClient(client -> PlayerSettings.forceSneak=original);
            context.setScreen(TitleScreen::new);
        }
    }

    private static void awaitSneak(ClientGameTestContext context,UUID id,boolean expected) {
        context.waitFor(client -> {
            var player=client.getSingleplayerServer().getPlayerList().getPlayer(id);
            return client.player.input.keyPresses.shift()==expected && player!=null && player.getLastClientInput().shift()==expected;
        });
        context.waitTicks(3);
        context.runOnClient(client -> {
            check(client.player.input.keyPresses.shift()==expected,"Sneak input did not remain stable");
            check(client.getSingleplayerServer().getPlayerList().getPlayer(id).getLastClientInput().shift()==expected,
                    "Server received the wrong Sneak input");
        });
    }
    private static void controls(ClientGameTestContext context) {
        var fps=new FpsHud(); var coordinates=new CoordinatesHud();
        context.setScreen(() -> new ClickGuiScreen(fps,VoicechatClient.openGuiKey(),coordinates,new ArraylistHud(fps,coordinates)));
    }
    private static void press(ClientGameTestContext context,String label) {
        context.runOnClient(client -> {
            Screen screen=client.gui.screen();
            var button=screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(w->w.getMessage().getString().equals(label)).findFirst().orElseThrow();
            var event=new MouseButtonEvent((button.getX()+button.getWidth()/2.0)*ClickGuiScreen.UI_SCALE,
                    (button.getY()+button.getHeight()/2.0)*ClickGuiScreen.UI_SCALE,new MouseButtonInfo(0,0));
            check(screen.mouseClicked(event,false),"Force Sneak rejected scaled input"); screen.mouseReleased(event);
        });
    }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
