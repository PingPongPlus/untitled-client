package pingplus.voicechat.client;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.level.GameType;
import pingplus.voicechat.client.gui.ClickGuiScreen;
import pingplus.voicechat.client.hud.ArraylistHud;
import pingplus.voicechat.client.hud.CoordinatesHud;
import pingplus.voicechat.client.hud.CustomCrosshairHud;
import pingplus.voicechat.client.hud.CustomCrosshairHud.Shape;
import pingplus.voicechat.client.hud.FpsHud;

/** Scaled UI input, persisted preferences, and pixels from the real HUD/mixin. */
final class CustomCrosshairGameTest {
    private static final int COLOR = 0xFF00FF;

    static void run(ClientGameTestContext context) {
        var crosshair = CustomCrosshairHud.INSTANCE;
        var saved = context.computeOnClient(client -> Saved.of(crosshair));
        var debug = context.computeOnClient(client -> client.debugEntries.getStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR));
        var camera = context.computeOnClient(client -> client.options.getCameraType());
        var indicator = context.computeOnClient(client -> client.options.attackIndicator().get());
        boolean hidden = context.computeOnClient(client -> client.gui.hud.isHidden());
        var holdCooldown = new AtomicBoolean();
        context.runOnClient(client -> ClientTickEvents.END_CLIENT_TICK.register(ticking -> {
            if (holdCooldown.get() && ticking.player != null) ticking.player.resetAttackStrengthTicker();
        }));
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.gui.overlay() == null);
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                client.getWindow().setWindowed(1440,900);
                client.options.guiScale().set(2); client.resizeGui();
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.options.attackIndicator().set(AttackIndicatorStatus.CROSSHAIR);
                client.debugEntries.setStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR,DebugScreenEntryStatus.NEVER);
                if (client.gui.hud.isHidden()) client.gui.hud.toggle();
                client.player.setXRot(-80);
                crosshair.resetAppearance(); crosshair.setColor(COLOR);
                if (!crosshair.isEnabled()) crosshair.toggle();
                if (crosshair.isOutline()) crosshair.toggleOutline();
                var id = client.player.getUUID(); var server = client.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayer(id).setGameMode(GameType.SURVIVAL));
            });
            context.waitFor(client -> client.gameMode.getPlayerMode() == GameType.SURVIVAL);
            context.waitTicks(15);
            for (Shape shape : Shape.values()) {
                context.runOnClient(client -> crosshair.setShape(shape));
                context.waitTicks(3);
                var pixels = capture(context,"crosshair-"+shape.name().toLowerCase(java.util.Locale.ROOT));
                check(pixels.colorCount()>0,"Custom shape did not render: "+shape);
                switch (shape) {
                    case CROSS -> check(pixels.colored(6,0) && pixels.colored(0,-6) && !pixels.colored(0,0),"Cross arms/gap incorrect");
                    case DOT -> check(pixels.colored(0,0) && !pixels.colored(6,0),"Dot bounds incorrect");
                    case CIRCLE -> check(pixels.colored(9,0) && !pixels.colored(0,0),"Circle radius/hole incorrect");
                    case T -> check(pixels.colored(6,0) && pixels.colored(0,6) && !pixels.colored(0,-6),"T has an upper arm");
                }
                context.runOnClient(client -> { crosshair.setSize(1); crosshair.setGap(0); });
                context.waitTicks(3);
                var smallest = capture(context,"crosshair-"+shape.name().toLowerCase(java.util.Locale.ROOT)+"-smallest");
                if (shape == Shape.CIRCLE)
                    check(smallest.colored(1,0) && !smallest.colored(0,0),"Smallest circle did not render");
                else check(smallest.colored(0,0) && smallest.colorCount()==1,"Minimum crosshair is not a single pixel: "+shape);
                context.runOnClient(client -> { crosshair.setSize(6); crosshair.setGap(3); });
            }
            context.runOnClient(client -> { crosshair.setShape(Shape.CROSS); crosshair.toggleCenterDot(); crosshair.toggleOutline(); });
            context.waitTicks(3);
            var outlined = capture(context,"crosshair-outline-dot");
            check(outlined.colored(0,0) && outlined.rgb(6,1)==0,"Center dot or black outline missing");
            context.runOnClient(client -> { crosshair.toggleCenterDot(); crosshair.toggleCooldownRing(); });
            context.waitTicks(20);
            var ready = capture(context,"crosshair-cooldown-ready");
            check(ready.colored(13,0) && ready.colored(0,13),"Ready cooldown ring missing");
            holdCooldown.set(true);
            context.waitTicks(3);
            var charging = capture(context,"crosshair-cooldown-charging");
            check(charging.colorCount()<ready.colorCount(),"Attack cooldown did not change the rendered ring");
            holdCooldown.set(false);
            context.runOnClient(client -> check(client.options.attackIndicator().get()==AttackIndicatorStatus.CROSSHAIR,"Ring changed vanilla attack settings"));

            context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(3);
            check(capture(context,"crosshair-third-person").colorCount()==0,"Crosshair rendered in third person");
            context.runOnClient(client -> { client.options.setCameraType(CameraType.FIRST_PERSON); client.gui.hud.toggle(); });
            context.waitTicks(3);
            check(capture(context,"crosshair-hidden-hud").colorCount()==0,"Crosshair ignored F1");
            context.runOnClient(client -> {
                client.gui.hud.toggle();
                client.debugEntries.setStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR,DebugScreenEntryStatus.ALWAYS_ON);
            });
            context.waitTicks(3);
            check(capture(context,"crosshair-debug").colorCount()==0,"Custom crosshair covered the 3D debug crosshair");
            context.runOnClient(client -> {
                client.debugEntries.setStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR,DebugScreenEntryStatus.NEVER);
                var id=client.player.getUUID(); var server=client.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayer(id).setGameMode(GameType.SPECTATOR));
            });
            context.waitFor(client -> client.gameMode.getPlayerMode()==GameType.SPECTATOR);
            context.waitTicks(3);
            check(capture(context,"crosshair-spectator").colorCount()==0,"Spectator sky target displayed a crosshair");
            context.runOnClient(client -> {
                var id=client.player.getUUID(); var server=client.getSingleplayerServer();
                server.execute(() -> server.getPlayerList().getPlayer(id).setGameMode(GameType.SURVIVAL));
                crosshair.toggle();
            });
            context.waitFor(client -> client.gameMode.getPlayerMode()==GameType.SURVIVAL);
            context.waitTicks(3);
            var vanilla = capture(context,"crosshair-vanilla-restored");
            // Vanilla centers its odd-width sprite at (guiSize - 15) / 2.
            check(vanilla.colorCount()==0 && vanilla.rgb(-1,-1)!=vanilla.rgb(20,0),"Disabling did not restore the vanilla crosshair");

            testControls(context,crosshair);
            ClickGuiAllOffGameTest.run(context);
            System.out.println("PASS: crosshair shapes, gap, dot, outline, cooldown, camera/F1/debug/spectator guards, vanilla restoration, scaled UI, save/reload and All off");
        } finally {
            holdCooldown.set(false);
            context.runOnClient(client -> {
                saved.restore(crosshair);
                client.options.setCameraType(camera); client.options.attackIndicator().set(indicator);
                client.debugEntries.setStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR,debug);
                if (client.gui.hud.isHidden()!=hidden) client.gui.hud.toggle();
            });
            context.setScreen(TitleScreen::new);
        }
    }

    private static void testControls(ClientGameTestContext context,CustomCrosshairHud crosshair) {
        var fps=new FpsHud(); var coordinates=new CoordinatesHud();
        context.setScreen(() -> new ClickGuiScreen(fps,VoicechatClient.openGuiKey(),coordinates,new ArraylistHud(fps,coordinates)));
        press(context,"Crosshair");
        press(context,"Crosshair options");
        context.waitTicks(5); context.takeScreenshot("crosshair-options");
        press(context,"Shape: Cross");
        context.runOnClient(client -> check(crosshair.shape()==Shape.DOT && crosshair.isEnabled(),"Shape button changed feature enablement"));
        slider(context,"Size",false);
        context.runOnClient(client -> check(crosshair.size()==1 && reload().size()==1,"Minimum size was not applied or saved"));
        slider(context,"Size",true); slider(context,"Gap",false); slider(context,"Thickness",true);
        slider(context,"Red",true); slider(context,"Green",false); slider(context,"Blue",true); slider(context,"Opacity (%)",false);
        context.runOnClient(client -> {
            check(crosshair.size()==24 && crosshair.gap()==0 && crosshair.thickness()==6 && crosshair.opacity()==10
                    && crosshair.color()==COLOR,"Crosshair sliders did not apply their bounds");
            check(Saved.of(reload()).equals(Saved.of(crosshair)),"Saved preferences did not reload correctly");
        });
        press(context,"Reset crosshair");
        context.runOnClient(client -> check(crosshair.isEnabled() && crosshair.shape()==Shape.CROSS && crosshair.size()==6
                && crosshair.gap()==3 && crosshair.thickness()==1 && crosshair.color()==0xFFFFFF
                && crosshair.opacity()==100 && crosshair.isOutline() && !crosshair.isCenterDot() && !crosshair.isCooldownRing(),"Reset defaults or enablement incorrect"));
        context.runOnClient(client -> { client.getWindow().setWindowed(854,480); client.options.guiScale().set(3); client.resizeGui(); });
        context.waitTicks(5);
        slider(context,"Blue",false);
        press(context,"Attack cooldown ring");
        context.runOnClient(client -> check(crosshair.blue()==0 && crosshair.isCooldownRing(),"Scrolled controls at small resolution did not work"));
        context.takeScreenshot("crosshair-options-small");
        press(context,"Close Crosshair options");
        context.runOnClient(client -> { client.getWindow().setWindowed(1440,900); client.options.guiScale().set(2); client.resizeGui(); });
    }

    private static CustomCrosshairHud reload() {
        try {
            var constructor=CustomCrosshairHud.class.getDeclaredConstructor(); constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void press(ClientGameTestContext context,String label) {
        context.runOnClient(client -> {
            Screen screen=client.gui.screen();
            var button=screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(w->w.getMessage().getString().equals(label)).findFirst().orElseThrow();
            focusVisible(screen,button);
            click(screen,button,button.getX()+button.getWidth()/2.0);
        });
    }
    private static void slider(ClientGameTestContext context,String label,boolean maximum) {
        context.runOnClient(client -> {
            Screen screen=client.gui.screen();
            var slider=screen.children().stream().filter(AbstractSliderButton.class::isInstance).map(AbstractSliderButton.class::cast)
                    .filter(w->w.getMessage().getString().startsWith(label+"   ")).findFirst().orElseThrow();
            focusVisible(screen,slider);
            click(screen,slider,maximum?slider.getRight()-4:slider.getX()+4);
        });
    }
    private static void focusVisible(Screen screen,AbstractWidget widget) {
        // The real keyboard handler scrolls the focused control into its settings viewport.
        screen.setFocused(widget); screen.keyPressed(new KeyEvent(340,0,0));
    }
    private static void click(Screen screen,AbstractWidget widget,double x) {
        var event=new MouseButtonEvent(x*ClickGuiScreen.UI_SCALE,(widget.getY()+widget.getHeight()/2.0)*ClickGuiScreen.UI_SCALE,new MouseButtonInfo(0,0));
        check(screen.mouseClicked(event,false),"Pointer rejected "+widget.getMessage().getString()); screen.mouseReleased(event);
    }
    private static Pixels capture(ClientGameTestContext context,String name) {
        var result=context.computeOnClient(client -> {
            var future=new CompletableFuture<Pixels>();
            double scale=client.getWindow().getGuiScale();
            int cx=client.getWindow().getGuiScaledWidth()/2,cy=client.getWindow().getGuiScaledHeight()/2;
            Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image -> {
                try (image) {
                    var path=client.gameDirectory.toPath().resolve("screenshots/"+name+".png");
                    Files.createDirectories(path.getParent()); image.writeToFile(path);
                    future.complete(new Pixels(javax.imageio.ImageIO.read(path.toFile()),cx,cy,scale));
                } catch (Throwable failure) { future.completeExceptionally(failure); }
            });
            return future;
        });
        context.waitFor(client -> result.isDone()); return result.join();
    }
    private record Pixels(BufferedImage image,int centerX,int centerY,double scale) {
        int rgb(int x,int y) { return image.getRGB((int)((centerX+x+.5)*scale),(int)((centerY+y+.5)*scale))&0xFFFFFF; }
        boolean colored(int x,int y) { return rgb(x,y)==COLOR; }
        int colorCount() { int count=0; for (int y=-45;y<=45;y++) for (int x=-45;x<=45;x++) if (colored(x,y)) count++; return count; }
    }
    private record Saved(boolean enabled,Shape shape,int size,int gap,int thickness,int color,int opacity,boolean outline,boolean dot,boolean ring) {
        static Saved of(CustomCrosshairHud c) { return new Saved(c.isEnabled(),c.shape(),c.size(),c.gap(),c.thickness(),c.color(),c.opacity(),c.isOutline(),c.isCenterDot(),c.isCooldownRing()); }
        void restore(CustomCrosshairHud c) {
            c.setShape(shape); c.setSize(size); c.setGap(gap); c.setThickness(thickness); c.setColor(color); c.setOpacity(opacity);
            if(c.isEnabled()!=enabled)c.toggle(); if(c.isOutline()!=outline)c.toggleOutline();
            if(c.isCenterDot()!=dot)c.toggleCenterDot(); if(c.isCooldownRing()!=ring)c.toggleCooldownRing();
        }
    }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
