package pingplus.voicechat.client.gui.glass;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/** One screen-sized draw behind widgets; no particles, CPU simulation, or per-drop allocations. */
public final class GlassRain {
    private static final long START = System.nanoTime();

    public static void draw(GuiGraphicsExtractor graphics) {
        if (Minecraft.getInstance().gui.overlay() instanceof LoadingOverlay) return;
        GlassBackdrop.request();
        graphics.nextStratum();
        // Wall time keeps rain moving while the integrated server is paused.
        float seconds = (System.nanoTime() - START) / 1_000_000_000f;
        var pose = new Matrix3x2f(graphics.pose());
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        graphics.guiRenderState.addGuiElement(new RainState(pose, width, height, seconds,
                new ScreenRectangle(0, 0, width, height).transformMaxBounds(pose)));
        graphics.nextStratum();
    }

    private record RainState(Matrix3x2f pose, int width, int height, float seconds,
                             ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return GlassPipelines.RAIN; }
        @Override public TextureSetup textureSetup() { return GlassBackdrop.rainTexture(); }
        @Override public ScreenRectangle scissorArea() { return null; }
        @Override public void buildVertices(VertexConsumer vertices) {
            vertex(vertices, 0, 0, 0);
            vertex(vertices, 0, height, 1);
            vertex(vertices, width, height, 1);
            vertex(vertices, width, 0, 0);
        }
        private void vertex(VertexConsumer vertices, float x, float y, float v) {
            // UV.x is constant wall time; UV.y retains top-to-bottom orientation on every backend.
            vertices.addVertexWith2DPose(pose, x, y).setUv(seconds, v).setColor(0xFFFFFFFF);
        }
    }

    private GlassRain() {}
}
