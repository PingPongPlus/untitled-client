package pingplus.voicechat.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;

/** Opt-in GPU smoke test: ./gradlew runClientGameTest. Never included in the mod JAR. */
public final class GlassRenderingTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.setScreen(TitleScreen::new);
        context.waitTicks(30);
        context.takeScreenshot("glass-title");
        context.clickScreenButton("menu.options");
        context.waitForScreen(OptionsScreen.class);
        context.waitTicks(10);
        context.takeScreenshot("glass-options");
        context.setScreen(GlassTestScreen::new);
        context.waitTicks(10);
        context.takeScreenshot("glass-checkerboard");
        context.clickScreenButton("Glass test button");
        context.runOnClient(client -> {
            if (!((GlassTestScreen) client.gui.screen()).clicked) {
                throw new AssertionError("Glass button did not retain its click action");
            }
        });
        context.runOnClient(client -> client.options.guiScale().set(3));
        context.waitTicks(10);
        context.takeScreenshot("glass-scale-three");
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.waitTicks(10);
        context.takeScreenshot("glass-scale-two");
    }

    private static final class GlassTestScreen extends Screen {
        private boolean clicked;
        GlassTestScreen() { super(Component.literal("Glass rendering test")); }
        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.literal("Glass test button"), b -> clicked = true)
                    .bounds(width / 2 - 100, height / 2 - 40, 200, 20).build());
            Button disabled = addRenderableWidget(Button.builder(Component.literal("Disabled"), b -> {
                throw new AssertionError("Disabled glass button invoked");
            }).bounds(width / 2 - 100, height / 2 - 10, 200, 20).build());
            disabled.active = false;
            Button fading = addRenderableWidget(Button.builder(Component.literal("Half opacity"), b -> {})
                    .bounds(width / 2 - 100, height / 2 + 20, 200, 20).build());
            fading.setAlpha(0.5F);
        }
        @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            for (int y = 0; y < height; y += 8) {
                for (int x = 0; x < width; x += 8) {
                    graphics.fill(x, y, x + 8, y + 8,
                            ((x / 8 + y / 8) & 1) == 0 ? 0xFF24374F : 0xFF8FC2DB);
                }
            }
        }
    }
}
