package pingplus.voicechat.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import pingplus.voicechat.client.gui.glass.*;
import java.nio.file.Files;

/** Isolated visual/persistence verification; no production settings or multiplayer touched. */
final class WeatherGlassRenderingTest {
    static void run(ClientGameTestContext context) {
        var original = new java.util.concurrent.atomic.AtomicReference<WeatherGlassSettings.Values>();
        var corners = new java.util.concurrent.atomic.AtomicReference<Float>();
        context.runOnClient(client -> {
            original.set(WeatherGlassSettings.values()); corners.set(GlassCornerSettings.getScale());
            client.getWindow().setWindowed(1440, 900); client.options.guiScale().set(2); client.resizeGui();
            WeatherGlassSettings.set(new WeatherGlassSettings.Values(false, WeatherGlassSettings.Mode.AUTOMATIC, false, 65));
        });
        try {
            context.setScreen(Showroom::new); context.waitTicks(15); context.takeScreenshot("weather-glass-off");
            context.runOnClient(client -> WeatherGlassSettings.set(new WeatherGlassSettings.Values(true, WeatherGlassSettings.Mode.RAIN, true, 100)));
            context.waitTicks(110); context.takeScreenshot("weather-glass-rain");
            context.runOnClient(client -> GlassGpuTiming.begin()); context.waitTicks(40);
            context.runOnClient(client -> GlassGpuTiming.finish("Weather Glass Rain 1440x900"));
            context.waitTicks(25); context.takeScreenshot("weather-glass-rain-motion");
            context.runOnClient(client -> {
                var path = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("voicechat-weather-glass.properties");
                check(WeatherGlassSettings.read(path).equals(WeatherGlassSettings.values()), "Saved weather preferences differ");
                try {
                    var invalid = Files.createTempFile(path.getParent(), "weather-invalid-", ".properties");
                    try {
                        Files.writeString(invalid, "mode=unknown\nintensity=oops\n");
                        var value = WeatherGlassSettings.read(invalid);
                        check(!value.enabled() && value.mode() == WeatherGlassSettings.Mode.AUTOMATIC && value.intensity() == 65, "Invalid settings defaults");
                    } finally { Files.deleteIfExists(invalid); }
                } catch (java.io.IOException e) { throw new AssertionError(e); }
                WeatherGlassSettings.set(new WeatherGlassSettings.Values(true, WeatherGlassSettings.Mode.FROST, true, 100));
            });
            context.waitTicks(250); context.takeScreenshot("weather-glass-frost");
            context.runOnClient(client -> GlassGpuTiming.begin()); context.waitTicks(40);
            context.runOnClient(client -> GlassGpuTiming.finish("Weather Glass Frost 1440x900"));
            context.runOnClient(client -> { client.options.guiScale().set(3); client.resizeGui(); GlassCornerSettings.setScale(0); });
            context.waitTicks(10); context.takeScreenshot("weather-glass-square-scale3");
            context.runOnClient(client -> { GlassCornerSettings.setScale(1.5f); client.getWindow().setWindowed(854, 480); });
            context.waitTicks(10); context.takeScreenshot("weather-glass-small-scale3");
            context.runOnClient(client -> { client.getWindow().setWindowed(1440, 900); client.options.guiScale().set(2); client.resizeGui(); GlassCornerSettings.setScale(1); });
            var fps = new pingplus.voicechat.client.gui.FpsHud();
            var coordinates = new pingplus.voicechat.client.gui.CoordinatesHud();
            var arraylist = new pingplus.voicechat.client.gui.ArraylistHud(fps, coordinates);
            var category = net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("voicechat", "weather_test"));
            var key = new net.minecraft.client.KeyMapping("weather.test", 344, category);
            context.setScreen(() -> new pingplus.voicechat.client.gui.ClickGuiScreen(fps, key, coordinates, arraylist));
            context.waitTicks(10);
            press(context, "Weather Glass options");
            press(context, "Weather mode: Frost");
            context.runOnClient(client -> check(WeatherGlassSettings.mode() == WeatherGlassSettings.Mode.AUTOMATIC, "Mode control failed"));
            press(context, "Always active");
            context.runOnClient(client -> check(!WeatherGlassSettings.alwaysActive(), "Always active control failed"));
            context.runOnClient(client -> {
                var slider = client.gui.screen().children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.AbstractSliderButton)
                    .map(c -> (net.minecraft.client.gui.components.AbstractSliderButton)c)
                    .filter(c -> c.getMessage().getString().startsWith("Weather intensity")).findFirst().orElseThrow();
                var click = new net.minecraft.client.input.MouseButtonEvent(slider.getX() + slider.getWidth() / 2.0,
                    slider.getY() + 19, new net.minecraft.client.input.MouseButtonInfo(0, 0));
                check(slider.mouseClicked(click, false), "Intensity slider rejected pointer input"); slider.mouseReleased(click);
                check(WeatherGlassSettings.intensity() > 40 && WeatherGlassSettings.intensity() < 60, "Intensity slider did not update");
            });
            press(context, "Weather Glass");
            context.runOnClient(client -> check(!WeatherGlassSettings.enabled(), "Weather toggle failed to disable"));
            press(context, "Weather Glass");
            context.runOnClient(client -> check(WeatherGlassSettings.enabled(), "Weather toggle failed to enable"));
            context.waitTicks(5); context.takeScreenshot("weather-glass-settings");
            context.setScreen(Showroom::new); context.waitTicks(220); context.takeScreenshot("weather-glass-thawed");
            context.runOnClient(client -> check(WeatherGlass.amounts().frost() < .15, "Automatic mode must thaw without a world"));
            context.runOnClient(client -> client.reloadResourcePacks());
            context.waitFor(client -> client.gui.overlay() == null);
            context.waitTicks(5); context.takeScreenshot("weather-glass-reloaded");
            try (var world = context.worldBuilder().create()) {
                context.waitFor(client -> client.player != null && client.gui.overlay() == null);
                context.setScreen(() -> null);
                context.runOnClient(client -> {
                    WeatherGlassSettings.set(new WeatherGlassSettings.Values(true, WeatherGlassSettings.Mode.RAIN, false, 100));
                    client.getSingleplayerServer().submit(() -> client.getSingleplayerServer().setWeatherParameters(0, 6000, true, false));
                });
                context.waitFor(client -> client.level.getRainLevel(1) > .9f);
                context.waitTicks(100); context.takeScreenshot("weather-glass-world-rain");
                var outdoor = new java.util.concurrent.atomic.AtomicReference<Float>();
                context.runOnClient(client -> {
                    check(client.level.canSeeSky(client.player.blockPosition().above()), "Test player must be outside");
                    outdoor.set(WeatherGlass.amounts().rain());
                    check(outdoor.get() > .4, "Rain must wet HUD glass outdoors");
                    var roof = client.player.blockPosition().above(3);
                    var server = client.getSingleplayerServer();
                    server.submit(() -> server.overworld().setBlockAndUpdate(roof, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
                });
                context.waitFor(client -> !client.level.canSeeSky(client.player.blockPosition().above()));
                context.waitTicks(120);
                context.runOnClient(client -> {
                    check(WeatherGlass.amounts().rain() < outdoor.get() * .5, "Sheltered HUD glass must dry");
                    WeatherGlassSettings.set(new WeatherGlassSettings.Values(true, WeatherGlassSettings.Mode.FROST, true, 100));
                });
                context.waitTicks(160); context.takeScreenshot("weather-glass-world-forced-frost");
                context.runOnClient(client -> check(WeatherGlass.amounts().frost() > .65, "Always active must freeze indoors"));
            }
        } finally {
            context.runOnClient(client -> { WeatherGlassSettings.set(original.get()); GlassCornerSettings.setScale(corners.get()); });
            context.setScreen(TitleScreen::new);
        }
    }
    private static void press(ClientGameTestContext context, String label) {
        context.runOnClient(client -> {
            var button = client.gui.screen().children().stream().filter(c -> c instanceof Button).map(c -> (Button)c)
                .filter(b -> b.getMessage().getString().equals(label)).findFirst().orElseThrow();
            button.onPress(new net.minecraft.client.input.MouseButtonInfo(0, 0));
        });
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static final class Showroom extends Screen {
        Showroom() { super(Component.literal("Weather Glass material review")); }
        @Override public boolean isPauseScreen() { return false; }
        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.literal("Glass button — readable labels"), b -> {}).bounds(width / 2 - 100, height / 2 + 83, 200, 20).build());
        }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
            for (int row = 0; row < height; row++) {
                float t = row / (float)height;
                int r = (int)(32 + t * 34), green = (int)(61 + t * 45), b = (int)(82 + t * 42);
                g.fill(0, row, width, row + 1, 0xFF000000 | r << 16 | green << 8 | b);
            }
            for (int x = 0; x < width; x += 32) g.fill(x, height / 2, x + 14, height, 0x50364B39);
            g.nextStratum();
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
            int x = width / 2 - 150, y = height / 2 - 100;
            GlassStyle.surface(g, x, y, 300, 170, 1, .08f);
            g.nextStratum();
            g.text(font, GlassStyle.label("WEATHER GLASS"), x + 20, y + 22, 0xFFFFFFFF, false);
            g.text(font, "Mode: " + WeatherGlassSettings.mode().label, x + 20, y + 42, 0xFFE4F0F7, false);
            g.text(font, "Fine rain lenses / ice crystal growth", x + 20, y + 64, 0xFFD0E2EE, false);
            g.text(font, "Clear centre • frost gathers at the edges", x + 20, y + 83, 0xFFD0E2EE, false);
            GlassButtonRenderer.drawHudRect(g, x + 20, y + 112, 92, 34, 0xF000FF00, true, false);
            g.nextStratum(); g.text(font, "HUD  120 FPS", x + 28, y + 124, 0xFFFFFFFF, false);
            GlassButtonRenderer.control(g, x + 150, y + 118, 110, 23, 0xFF424242);
            g.nextStratum(); g.text(font, "Clipped control", x + 161, y + 125, 0xFFFFFFFF, false);
            super.extractRenderState(g, mx, my, dt);
        }
    }
}
