package pingplus.voicechat.client;

import pingplus.voicechat.client.hud.ArraylistHud;
import pingplus.voicechat.client.hud.CoordinatesHud;
import pingplus.voicechat.client.hud.FpsHud;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import pingplus.voicechat.client.gui.ClickGuiScreen;

/** Real crosshair picking and raw mouse callbacks, with local fixtures and saved gain checks. */
final class MiddleClickVoiceVolumeGameTest {
    static void run(ClientGameTestContext context) {
        var settings = VoicechatClient.voiceSettings();
        boolean original = settings.middleClickVolume;
        double output = settings.volume;
        UUID targetId = UUID.randomUUID(), otherId = UUID.randomUUID();
        var target = new AtomicReference<RemotePlayer>();
        CameraType camera = context.computeOnClient(client -> client.options.getCameraType());
        float yaw = context.computeOnClient(client -> client.player.getYRot());
        float pitch = context.computeOnClient(client -> client.player.getXRot());
        try {
            context.runOnClient(client -> {
                check(new VoiceSettings().middleClickVolume, "new settings must default to enabled");
                check(new Gson().fromJson("{\"enabled\":false}",VoiceSettings.class).middleClickVolume,
                        "existing configs without the new field must default to enabled");
                settings.middleClickVolume = true;
                settings.setVolume(targetId,.6);
                settings.setVolume(otherId,.35);
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.player.setYRot(0); client.player.setXRot(0);
                var remote = new RemotePlayer(client.level,new GameProfile(targetId,"VolumeTest"));
                remote.setId(1999995);
                remote.setPos(client.player.getX(),client.player.getY(),client.player.getZ()+2.5);
                remote.setNoGravity(true);
                client.level.addEntity(remote);
                target.set(remote);
            });
            context.setScreen(() -> null);
            awaitTarget(context,target.get());
            context.getInput().holdMouse(2);
            context.waitForScreen(PlayerVoiceVolumeScreen.class);
            context.runOnClient(client -> {
                check(!client.mouseHandler.isMiddlePressed(), "handled press must not reach vanilla pick-item input");
                check(settings.volume(targetId)==.6, "opening must not change volume");
                check(!client.gui.screen().isPauseScreen(), "quick volume window must not pause gameplay");
                check(slider(client.gui.screen()).getMessage().getString().equals("Volume: 60%"), "saved player level must populate the slider");
            });
            context.getInput().releaseMouse(2);
            context.waitTicks(3);
            context.takeScreenshot("middle-click-voice-volume");
            setSlider(context,1);
            context.runOnClient(client -> {
                check(settings.volume(targetId)==2 && VoiceSettings.load().volume(targetId)==2, "200% must apply immediately and persist");
                check(settings.volume(otherId)==.35 && settings.volume==output, "other players and master volume must stay unchanged");
            });
            press(context,"Mute");
            context.runOnClient(client -> {
                check(settings.volume(targetId)==0 && VoiceSettings.load().volume(targetId)==0, "mute must apply and save");
                check(slider(client.gui.screen()).getMessage().getString().equals("Volume: 0%"), "mute must update slider");
            });
            press(context,"Unmute");
            context.runOnClient(client -> check(settings.volume(targetId)==1, "unmute must restore normal volume"));
            setSlider(context,0);
            context.runOnClient(client -> check(button(client.gui.screen(),"Unmute")!=null, "zero slider level must update mute control"));
            press(context,"Reset");
            context.runOnClient(client -> check(settings.volume(targetId)==1, "reset must restore 100%"));
            setSlider(context,.375);
            context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(256,0,0)));
            context.runOnClient(client -> check(client.gui.screen()==null && VoiceSettings.load().volume(targetId)==.75, "Escape must keep the saved volume and return to gameplay"));
            awaitTarget(context,target.get());
            context.getInput().pressMouse(2);
            context.runOnClient(client -> check(slider(client.gui.screen()).getMessage().getString().equals("Volume: 75%"), "reopening must retain the same player's level"));
            press(context,"Done");

            var fps = new FpsHud(); var coordinates = new CoordinatesHud();
            context.setScreen(() -> new ClickGuiScreen(fps,VoicechatClient.openGuiKey(),coordinates,new ArraylistHud(fps,coordinates)));
            context.runOnClient(client -> click(client.gui.screen(),button(client.gui.screen(),"Middle-click volume"),ClickGuiScreen.UI_SCALE));
            context.runOnClient(client -> check(!settings.middleClickVolume && !VoiceSettings.load().middleClickVolume, "ClickGUI toggle must disable and persist"));
            context.setScreen(() -> null);
            awaitTarget(context,target.get());
            context.getInput().holdMouse(2);
            context.runOnClient(client -> check(client.gui.screen()==null && client.mouseHandler.isMiddlePressed(), "disabled setting must pass the middle press to vanilla"));
            context.getInput().releaseMouse(2);
            context.runOnClient(client -> VoicechatClient.toggleMiddleClickVolume());

            context.runOnClient(client -> client.player.setXRot(-90));
            context.waitFor(client -> client.hitResult != null && client.hitResult.getType()==HitResult.Type.MISS);
            context.getInput().pressMouse(2);
            context.runOnClient(client -> check(client.gui.screen()==null, "middle-clicking empty space must not open a window"));
            context.runOnClient(client -> client.player.setXRot(75));
            context.waitFor(client -> client.hitResult != null && client.hitResult.getType()==HitResult.Type.BLOCK);
            context.getInput().pressMouse(2);
            context.runOnClient(client -> check(client.gui.screen()==null, "middle-clicking a block must not open a window"));
            context.setScreen(() -> new PauseScreen(true));
            context.getInput().pressMouse(2);
            context.runOnClient(client -> check(client.gui.screen() instanceof PauseScreen, "middle clicks in menus must not replace the screen"));
            System.out.println("PASS: middle-click player targeting, default/legacy settings, live/saved player volume, mute/reset, reopen, disable/vanilla input, blocks/misses/menus");
        } finally {
            context.getInput().releaseMouse(2);
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                if (target.get()!=null) target.get().discard();
                settings.middleClickVolume=original;
                settings.playerVolumes.remove(targetId.toString());
                settings.playerVolumes.remove(otherId.toString());
                settings.save();
                client.options.setCameraType(camera);
                client.player.setYRot(yaw); client.player.setXRot(pitch);
            });
        }
    }
    private static void awaitTarget(ClientGameTestContext context, RemotePlayer target) {
        context.waitFor(client -> client.hitResult instanceof EntityHitResult hit && hit.getEntity()==target);
    }
    private static AbstractSliderButton slider(net.minecraft.client.gui.screens.Screen screen) {
        return screen.children().stream().filter(AbstractSliderButton.class::isInstance).map(AbstractSliderButton.class::cast).findFirst().orElseThrow();
    }
    private static Button button(net.minecraft.client.gui.screens.Screen screen, String label) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> b.getMessage().getString().equals(label)).findFirst().orElseThrow();
    }
    private static void press(ClientGameTestContext context,String label) {
        context.runOnClient(client -> click(client.gui.screen(),button(client.gui.screen(),label),1));
    }
    private static void click(net.minecraft.client.gui.screens.Screen screen,Button button,float scale) {
        var event = new MouseButtonEvent((button.getX()+button.getWidth()/2.0)*scale,(button.getY()+button.getHeight()/2.0)*scale,new MouseButtonInfo(0,0));
        check(screen.mouseClicked(event,false),"button must accept input: "+button.getMessage().getString());
        screen.mouseReleased(event);
    }
    private static void setSlider(ClientGameTestContext context,double fraction) {
        context.runOnClient(client -> {
            var screen=client.gui.screen(); var slider=slider(screen);
            var event=new MouseButtonEvent(slider.getX()+4+(slider.getWidth()-8)*fraction,slider.getY()+10,new MouseButtonInfo(0,0));
            check(screen.mouseClicked(event,false),"volume slider must accept input"); screen.mouseReleased(event);
        });
    }
    private static void check(boolean passed,String label) { if(!passed)throw new AssertionError(label); }
}
