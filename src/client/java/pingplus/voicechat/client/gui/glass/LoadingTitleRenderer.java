package pingplus.voicechat.client.gui.glass;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/** Feather only the artwork's blank outer margin using the built-in startup-safe pipeline. */
public final class LoadingTitleRenderer {
    private static final float[] GRID = {0, .02f, .04f, .06f, .08f, .92f, .94f, .96f, .98f, 1};

    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float opacity) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
        if (scissor != null) bounds = bounds.intersection(scissor);
        if (bounds != null) graphics.guiRenderState.addGuiElement(
                new TitleState(pose, x, y, width, height, opacity, scissor, bounds));
    }

    private record TitleState(Matrix3x2f pose, int x, int y, int width, int height, float opacity,
                              ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI_TEXTURED; }
        @Override public TextureSetup textureSetup() {
            var texture = Minecraft.getInstance().getTextureManager().getTexture(LoadingTitleTexture.ID);
            return TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
        }
        @Override public void buildVertices(VertexConsumer vertices) {
            for (int row = 0; row < GRID.length - 1; row++) {
                for (int col = 0; col < GRID.length - 1; col++) {
                    vertex(vertices, GRID[col], GRID[row]);
                    vertex(vertices, GRID[col], GRID[row + 1]);
                    vertex(vertices, GRID[col + 1], GRID[row + 1]);
                    vertex(vertices, GRID[col + 1], GRID[row]);
                }
            }
        }
        private void vertex(VertexConsumer vertices, float u, float v) {
            float alpha = opacity * feather(u) * feather(v);
            vertices.addVertexWith2DPose(pose, x + u * width, y + v * height)
                    .setUv(u, v).setColor(GlassStyle.alpha(0xFFFFFFFF, alpha));
        }
        private static float feather(float coordinate) {
            float t = Math.clamp(Math.min(coordinate, 1 - coordinate) / .08f, 0, 1);
            return t * t * (3 - 2 * t);
        }
    }
    private LoadingTitleRenderer() {}
}
