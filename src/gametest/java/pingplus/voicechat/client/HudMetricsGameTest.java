package pingplus.voicechat.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import pingplus.voicechat.client.gui.MetricsHud;
import pingplus.voicechat.client.gui.hud.HudEditor;
import pingplus.voicechat.client.gui.hud.HudEditorScreen;
import pingplus.voicechat.client.hud.HudMetrics;

/** Runs inside the ClickGUI test's world, through Minecraft's actual input callbacks. */
final class HudMetricsGameTest {
    static void run(ClientGameTestContext context) {
        boolean cps = MetricsHud.CPS.isEnabled(), speed = MetricsHud.SPEED.isEnabled();
        try {
            context.runOnClient(client -> {
                if (!cps) MetricsHud.CPS.toggle();
                if (!speed) MetricsHud.SPEED.toggle();
                HudMetrics.reset();
                client.player.setXRot(-60); // clicks face the sky rather than breaking the ground
            });
            context.setScreen(() -> null);
            context.waitTicks(2);
            for (int i = 0; i < 3; i++) { context.getInput().holdMouse(0); context.getInput().releaseMouse(0); }
            for (int i = 0; i < 2; i++) { context.getInput().holdMouse(1); context.getInput().releaseMouse(1); }
            context.runOnClient(client -> check(HudMetrics.leftCps() == 3 && HudMetrics.rightCps() == 2, "physical presses must count independently"));
            context.waitTicks(25);
            context.runOnClient(client -> check(HudMetrics.leftCps() == 0 && HudMetrics.rightCps() == 0, "CPS must expire without more input"));
            context.getInput().holdMouse(1);
            context.waitTicks(5);
            context.runOnClient(client -> check(HudMetrics.rightCps() == 1, "holding use must not inflate CPS"));
            context.getInput().releaseMouse(1);

            context.getInput().holdKey(clientOptions -> clientOptions.keyUp);
            context.waitTicks(20);
            context.runOnClient(client -> check(HudMetrics.speed() > .1 && Double.isFinite(HudMetrics.speed()), "walking must update speed"));
            context.getInput().holdMouse(0);
            context.getInput().releaseMouse(0);
            context.takeScreenshot("hud-cps-speed");
            context.getInput().releaseKey(clientOptions -> clientOptions.keyUp);
            context.setScreen(() -> new net.minecraft.client.gui.screens.PauseScreen(true));
            context.waitFor(client -> client.isPaused());
            context.runOnClient(client -> check(HudMetrics.speed() == 0, "pause must clear a moving readout"));
            context.setScreen(() -> null);
            context.waitTicks(25);
            context.runOnClient(client -> check(HudMetrics.speed() < .01, "stopping must return speed to zero"));

            context.setScreen(HudEditorScreen::new);
            context.waitTicks(3);
            context.getInput().pressMouse(0);
            context.runOnClient(client -> {
                check(HudMetrics.leftCps() == 0 && HudMetrics.rightCps() == 0, "editor clicks must not count");
                check(HudMetrics.speed() == 0, "stationary editor must show zero speed");
                var screen = client.gui.screen();
                for (String id : java.util.List.of("cps", "speed")) {
                    var before = HudEditor.bounds(id, screen.width, screen.height);
                    var down = new MouseButtonEvent(before.x()+5, before.y()+5, new MouseButtonInfo(0,0));
                    check(screen.mouseClicked(down,false), id + " must be draggable");
                    var moved = new MouseButtonEvent(down.x()+45, down.y()+10, down.buttonInfo());
                    check(screen.mouseDragged(moved,45,10), id + " drag must be handled");
                    screen.mouseReleased(moved);
                    var after = HudEditor.bounds(id, screen.width, screen.height);
                    check(Math.abs(after.x()-before.x()-45)<1 && Math.abs(after.y()-before.y()-10)<1, id + " drag must move only the widget");
                    check(screen.mouseScrolled(after.x()+5,after.y()+5,0,1), id + " must be resizable");
                    check(HudEditor.bounds(id,screen.width,screen.height).scale()>after.scale(), id + " size must increase");
                    screen.mouseClicked(new MouseButtonEvent(after.x()+5,after.y()+5,new MouseButtonInfo(1,0)),false);
                }
            });
            context.takeScreenshot("hud-metrics-editor");
            System.out.println("PASS: actual mouse CPS/hold/expiry, walking/stopping, pause, HUD drag and resize");
        } finally {
            context.getInput().releaseMouse(0);
            context.getInput().releaseMouse(1);
            context.getInput().releaseKey(clientOptions -> clientOptions.keyUp);
            context.setScreen(() -> null);
            context.runOnClient(client -> {
                if (MetricsHud.CPS.isEnabled()!=cps) MetricsHud.CPS.toggle();
                if (MetricsHud.SPEED.isEnabled()!=speed) MetricsHud.SPEED.toggle();
                HudMetrics.reset();
                client.player.setXRot(7);
            });
        }
    }
    private static void check(boolean passed, String label) { if (!passed) throw new AssertionError(label); }
}
