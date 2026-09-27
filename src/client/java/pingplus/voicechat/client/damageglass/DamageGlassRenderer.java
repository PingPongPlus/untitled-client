package pingplus.voicechat.client.damageglass;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import pingplus.voicechat.mixin.client.DamageGlassRenderTypeAccessor;
import pingplus.voicechat.mixin.client.DamageGlassSetupAccessor;
import pingplus.voicechat.mixin.client.DamageGlassTextureAccessor;

/** Render-thread scope is consumed at submission; all animation data is baked into vertices. */
public final class DamageGlassRenderer {
    public static boolean inWorld;
    public static int current;
    private static boolean requested;
    private static TextureTarget scene;
    private static final Map<RenderType, RenderType> TYPES = new IdentityHashMap<>();
    private static final RenderPipeline[] PIPELINES = new RenderPipeline[4];
    static {
        for (int i = 0; i < PIPELINES.length; i++) {
            PIPELINES[i] = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(id("pipeline/damage_glass_" + i))
                    .withVertexShader(id("core/damage_glass"))
                    .withFragmentShader(id("core/damage_glass"))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withCull((i & 1) != 0)
                    .withColorTargetState((i & 2) != 0 ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT)
                    .build());
        }
    }
    public static void initialize() {}
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("voicechat", path); }
    public static boolean isGlass(RenderPipeline pipeline) {
        for (var candidate : PIPELINES) if (candidate == pipeline) return true;
        return false;
    }
    public static RenderType material(RenderType original) {
        if (original.isOutline() || original.format() != DefaultVertexFormat.ENTITY) return original;
        // Glints and special beam/eye passes retain their native pipeline; only textured model surfaces change.
        var setup = (DamageGlassSetupAccessor) (Object) ((DamageGlassRenderTypeAccessor) original).voicechat$setup();
        var texture = (DamageGlassTextureAccessor) setup.voicechat$textures().get("Sampler0");
        String shader = original.pipeline().getFragmentShader().getPath();
        if (texture == null || !shader.equals("core/entity")) return original;
        requested = true;
        return TYPES.computeIfAbsent(original, type -> RenderType.create("voicechat_damage_glass",
                RenderSetup.builder(PIPELINES[(type.pipeline().isCull() ? 1 : 0) | (type.hasBlending() ? 2 : 0)])
                        .withTexture("Sampler0", texture.voicechat$location()).useLightmap()
                        .setLayeringTransform(setup.voicechat$layering()).createRenderSetup()));
    }
    public static void begin() {
        inWorld = true;
        requested = false;
        current = 0;
    }
    public static void end() {
        inWorld = false;
        current = 0;
        if (!requested) closeScene();
    }
    public static PreparedRenderType bindScene(PreparedRenderType prepared) {
        ensureScene();
        var textures = new ArrayList<>(prepared.textures());
        textures.add(new PreparedRenderType.Texture("Sampler1", scene.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)));
        return new PreparedRenderType(prepared.pipeline(), prepared.outputTarget(), prepared.dynamicTransforms(),
                prepared.scissorState(), textures);
    }
    private static void ensureScene() {
        var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (scene != null && scene.width == target.width && scene.height == target.height
                && scene.getColorTexture().getFormat() == target.getColorTexture().getFormat()) return;
        closeScene();
        scene = new TextureTarget("Damage glass scene", target.width, target.height, false, target.getColorTexture().getFormat());
    }
    /** Capture terrain after its pass closes, before entities draw. Never sample a live color attachment. */
    public static void capture() {
        if (!inWorld || !requested) return;
        ensureScene();
        var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(target.getColorTexture(), scene.getColorTexture(),
                0, 0, 0, 0, 0, target.width, target.height);
    }
    private static void closeScene() {
        if (scene != null) scene.destroyBuffers();
        scene = null;
    }
    public static void close() { closeScene(); TYPES.clear(); }
    private DamageGlassRenderer() {}
}
