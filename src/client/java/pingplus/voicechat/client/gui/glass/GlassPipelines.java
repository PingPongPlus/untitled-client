package pingplus.voicechat.client.gui.glass;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class GlassPipelines {
    public static final RenderPipeline BUTTON = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(id("pipeline/glass_button"))
                    .withVertexShader(id("core/glass_button"))
                    .withFragmentShader(id("core/glass_button"))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withCull(false).build());
    public static final RenderPipeline BLUR_HORIZONTAL = blur("glass_blur_horizontal", "HORIZONTAL");
    public static final RenderPipeline BLUR_VERTICAL = blur("glass_blur_vertical", "VERTICAL");

    private static RenderPipeline blur(String name, String direction) {
        return RenderPipelines.register(RenderPipeline.builder()
                .withLocation(id("pipeline/" + name))
                .withVertexShader("core/screenquad")
                .withFragmentShader(id("core/glass_blur"))
                .withShaderDefine(direction)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false).build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("voicechat", path);
    }

    // Force registration before Minecraft loads and compiles resource-pack shaders.
    public static void initialize() {}
    private GlassPipelines() {}
}
