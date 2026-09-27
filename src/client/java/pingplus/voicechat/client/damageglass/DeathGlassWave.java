package pingplus.voicechat.client.damageglass;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Bounded, world-local death pulses, independent of the entity's remaining render lifetime. */
public final class DeathGlassWave {
    public static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(id("pipeline/death_glass"))
            .withVertexShader(id("core/damage_glass"))
            .withFragmentShader(id("core/death_glass"))
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build());
    private static final RenderType TYPE = RenderType.create("voicechat_death_glass",
            RenderSetup.builder(PIPELINE).useLightmap().createRenderSetup());
    private static final Set<LivingEntity> DEAD = Collections.newSetFromMap(new WeakHashMap<>());
    private static final List<Wave> WAVES = new ArrayList<>();
    private static Object world;
    private static long ticks;
    private record Wave(Vec3 center, long start, float size, boolean player) {}
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("voicechat", path); }

    public static void initialize() { ClientTickEvents.END_CLIENT_TICK.register(DeathGlassWave::tick); }
    private static void tick(Minecraft client) {
        if (world != client.level) { DEAD.clear(); WAVES.clear(); ticks = 0; world = client.level; }
        if (client.level == null || client.isPaused()) return;
        ticks++;
        WAVES.removeIf(w -> ticks - w.start >= 32 || !DamageGlassSettings.deathWave()
                || !(w.player ? DamageGlassSettings.players() : DamageGlassSettings.mobs()));
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.isAlive()) { DEAD.remove(living); continue; }
            if (!DEAD.add(living)) continue;
            boolean player = living instanceof Player;
            if (!DamageGlassSettings.deathWave() || living.isInvisible()
                    || !(player ? DamageGlassSettings.players() : living instanceof Mob && DamageGlassSettings.mobs())) continue;
            if (WAVES.size() >= 32) WAVES.removeFirst();
            WAVES.add(new Wave(living.position().add(0, .35, 0), ticks,
                    Math.clamp(living.getBbHeight() * 0.9f, 1.0f, 1.7f), player));
        }
    }

    public static void submit(CameraRenderState camera, SubmitNodeCollector collector) {
        if (!DamageGlassSettings.deathWave() || world != Minecraft.getInstance().level || WAVES.isEmpty()) return;
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (Wave wave : WAVES) {
            if (!(wave.player ? DamageGlassSettings.players() : DamageGlassSettings.mobs())) continue;
            float phase = Math.clamp((ticks - wave.start + partial) / 32f, 0, 1);
            // Outward-only splash: erupts, rises and flares, then dissolves; the crest never folds back in.
            float expand = 1 - (float)Math.pow(1 - phase, 3);
            float alpha = smooth(Math.min(phase / .12f, 1)) * (1 - smooth(Math.max(0, (phase - .80f) / .20f)));
            if (expand < .01f || alpha < .001f || wave.center.distanceToSqr(camera.pos) > 128 * 128) continue;
            DamageGlassRenderer.requestScene();
            PoseStack pose = new PoseStack();
            pose.translate(wave.center.x - camera.pos.x, wave.center.y - camera.pos.y, wave.center.z - camera.pos.z);
            float size = wave.size;
            collector.submitCustomGeometry(pose, TYPE, (transform, vertices) -> {
                for (int lat = 0; lat < 20; lat++) for (int lon = 0; lon < 64; lon++) {
                    vertex(vertices, transform, lat, lon, expand, phase, alpha, size);
                    vertex(vertices, transform, lat + 1, lon, expand, phase, alpha, size);
                    vertex(vertices, transform, lat + 1, lon + 1, expand, phase, alpha, size);
                    vertex(vertices, transform, lat, lon + 1, expand, phase, alpha, size);
                }
            });
        }
    }
    private static float smooth(float x) { return x * x * (3 - 2 * x); }
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, int lat, int lon, float expand, float phase, float alpha, float size) {
        // A glass splash sheet: it erupts from the death spot, rises mostly along Y,
        // flares slightly outward and its rim undulates like a water crown; the crest
        // always leans away from the center and the wave never folds back inward.
        double b = Math.PI * 2 * lon / 64, s = lat / 20.0;
        double sb = Math.sin(b), cb = Math.cos(b);
        float H = size * 1.6f * expand;
        double h = H * (0.78 + 0.22 * Math.sin(5 * b - 9 * phase));
        double R0 = size * (0.28 + 0.42 * expand), R1 = R0 * 1.25, dR = R1 - R0;
        double w = 1 + 0.05 * Math.sin(6 * b - 8 * phase + 2.5 * s);
        double R = R0 + dR * s;
        double r = R * w;
        double x = r * cb, z = r * sb, y = h * s;
        // Analytic derivatives so reflections roll over the actual wave surface.
        double drds = dR * w + R * 0.125 * Math.cos(6 * b - 8 * phase + 2.5 * s);
        double drdb = R * 0.30 * Math.cos(6 * b - 8 * phase + 2.5 * s);
        double dhdb = H * 1.10 * Math.cos(5 * b - 9 * phase);
        double ax = drds * cb, ay = h, az = drds * sb;
        double bx = drdb * cb - r * sb, by = dhdb * s, bz = drdb * sb + r * cb;
        double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) { nx = 0; ny = 1; nz = 0; } else { nx /= len; ny /= len; nz /= len; }
        out.addVertex(pose, (float)x, (float)y, (float)z).setColor(1f, 1f, 1f, alpha)
                .setUv(lon / 64f, lat / 20f).setOverlay(255 | Math.round(phase * 255) << 16)
                .setLight(240 | 240 << 16).setNormal(pose, (float)nx, (float)ny, (float)nz);
    }
    private DeathGlassWave() {}
}
