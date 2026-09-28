package pingplus.voicechat.client.damageglass;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Death glass: a dead entity freezes into a 100% reflective statue, then bursts
 * into spinning glass shards that arc outward under gravity and dissolve.
 */
public final class DeathGlassMelt {
    /** Packed render-state flag marking the death path; HIDE replaces the model with the shard burst. */
    public static final int DEATH_FLAG = 1 << 31;
    public static final int HIDE_FLAG = 1 << 27;
    /** Shader preset id used for the mirror statue pass; 7 stays reserved for the ripple-only pass. */
    public static final int MELT_SHADER_ID = 6;
    /** Ticks the statue stands fully glass before the shards burst out. */
    public static final int STATUE_TICKS = 4;
    /** Total ticks from death until the burst fully dissolves. */
    private static final int LIFETIME = 30;
    private static final float GRAVITY = 0.05f;

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

    private static final Map<LivingEntity, Burst> BURSTS = new WeakHashMap<>();
    private static final Set<LivingEntity> HIDDEN = Collections.newSetFromMap(new WeakHashMap<>());
    private static final List<Burst> ACTIVE = new ArrayList<>();
    private static Object world;
    private static long ticks;

    /** One spinning glass sliver: a random tetrahedron on a ballistic arc. */
    private static final class Shard {
        final float[][] corners = new float[4][3];
        final float ax, ay, az, spin;
        float x, y, z, px, py, pz, vx, vy, vz, rot;

        Shard(Random random, float width, float height) {
            float size = 0.10f + random.nextFloat() * 0.22f;
            for (int i = 0; i < 4; i++) {
                unit(random, corners[i]);
                float scale = size * (0.6f + random.nextFloat() * 0.5f);
                corners[i][0] *= scale; corners[i][1] *= scale; corners[i][2] *= scale;
            }
            x = px = (random.nextFloat() - 0.5f) * width;
            z = pz = (random.nextFloat() - 0.5f) * width;
            y = py = random.nextFloat() * height;
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float speed = 0.14f + random.nextFloat() * 0.30f;
            vx = (float) Math.cos(angle) * speed * 0.8f;
            vz = (float) Math.sin(angle) * speed * 0.8f;
            vy = speed * (0.35f + random.nextFloat() * 0.9f);
            float tax = random.nextFloat() - 0.5f, tay = random.nextFloat() - 0.5f, taz = random.nextFloat() - 0.5f;
            float len = (float) Math.sqrt(tax * tax + tay * tay + taz * taz);
            if (len < 1e-3f) { tax = 1; tay = 0; taz = 0; } else { tax /= len; tay /= len; taz /= len; }
            ax = tax; ay = tay; az = taz;
            spin = (random.nextBoolean() ? 1 : -1) * (0.10f + random.nextFloat() * 0.30f);
            rot = random.nextFloat() * (float) (Math.PI * 2);
        }
    }

    private static final class Burst {
        final double x, z, groundY;
        final boolean player;
        final List<Shard> shards = new ArrayList<>();
        int age;

        Burst(LivingEntity living) {
            x = living.getX();
            z = living.getZ();
            groundY = living.getY();
            player = living instanceof Player;
            Random random = new Random(living.getId() * 31L
                    + (long) (living.getX() * 7919) + (long) (living.getZ() * 104729));
            int count = 30 + random.nextInt(8);
            for (int i = 0; i < count; i++) shards.add(new Shard(random, living.getBbWidth(), living.getBbHeight()));
        }

        void step() {
            age++;
            for (Shard shard : shards) {
                shard.px = shard.x; shard.py = shard.y; shard.pz = shard.z;
                shard.x += shard.vx; shard.y += shard.vy; shard.z += shard.vz;
                shard.vy -= GRAVITY;
                if (shard.y < 0) {
                    shard.y = 0;
                    shard.vy = -shard.vy * 0.30f;
                    shard.vx *= 0.65f; shard.vz *= 0.65f;
                }
                shard.rot += shard.spin;
            }
        }

        float alpha() {
            float p = Math.clamp(age / (float) LIFETIME, 0, 1);
            return 1 - smoothStep(p, 0.55f, 0.45f);
        }
    }

    public static void initialize() { ClientTickEvents.END_CLIENT_TICK.register(DeathGlassMelt::tick); }

    private static void tick(Minecraft client) {
        if (world != client.level) { BURSTS.clear(); HIDDEN.clear(); ACTIVE.clear(); ticks = 0; world = client.level; }
        if (client.level == null || client.isPaused()) return;
        ticks++;
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living.isAlive()) continue;
            if (BURSTS.containsKey(living) || HIDDEN.contains(living)) continue;
            if (!eligible(living)) continue;
            BURSTS.put(living, new Burst(living));
        }
        var it = BURSTS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Burst burst = entry.getValue();
            burst.age++;
            if (burst.age >= STATUE_TICKS) {
                ACTIVE.add(burst);
                HIDDEN.add(entry.getKey());
                it.remove();
            }
        }
        for (var burst : ACTIVE) burst.step();
        ACTIVE.removeIf(burst -> burst.age >= LIFETIME);
    }

    private static boolean eligible(LivingEntity living) {
        if (!DamageGlassSettings.deathWave() || living.isInvisible()) return false;
        return living instanceof Player ? DamageGlassSettings.players()
                : living instanceof Mob && DamageGlassSettings.mobs();
    }

    /** -1 = not tracked, 0 = mirror statue (render the model), 1 = shard burst (hide the model). */
    public static int phase(LivingEntity entity) {
        if (HIDDEN.contains(entity)) return 1;
        return BURSTS.containsKey(entity) ? 0 : -1;
    }

    public static void submit(CameraRenderState camera, SubmitNodeCollector collector) {
        if (!DamageGlassSettings.deathWave() || world != Minecraft.getInstance().level || ACTIVE.isEmpty()) return;
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (Burst burst : ACTIVE) {
            if (!(burst.player ? DamageGlassSettings.players() : DamageGlassSettings.mobs())) continue;
            float alpha = burst.alpha();
            if (alpha <= 0.01f) continue;
            if (camera.pos.distanceToSqr(burst.x, burst.groundY + 1, burst.z) > 128 * 128) continue;
            DamageGlassRenderer.requestScene();
            PoseStack pose = new PoseStack();
            pose.translate(burst.x - camera.pos.x, burst.groundY - camera.pos.y, burst.z - camera.pos.z);
            shardMesh(pose, collector, burst, partial, alpha);
        }
    }

    private static void shardMesh(PoseStack pose, SubmitNodeCollector collector, Burst burst, float partial, float alpha) {
        collector.submitCustomGeometry(pose, TYPE, (transform, vertices) -> {
            for (Shard shard : burst.shards) {
                float x = lerp(shard.px, shard.x, partial);
                float y = lerp(shard.py, shard.y, partial);
                float z = lerp(shard.pz, shard.z, partial);
                shardGeometry(vertices, transform, shard, x, y, z, alpha);
            }
        });
    }

    private static final int[][] FACES = {{0, 1, 2}, {0, 3, 1}, {0, 2, 3}, {1, 3, 2}};

    private static void shardGeometry(VertexConsumer out, PoseStack.Pose pose, Shard shard, float x, float y, float z, float alpha) {
        float[][] p = new float[4][3];
        float c = (float) Math.cos(shard.rot), s = (float) Math.sin(shard.rot), one = 1 - c;
        for (int i = 0; i < 4; i++) {
            float vx = shard.corners[i][0], vy = shard.corners[i][1], vz = shard.corners[i][2];
            float dot = shard.ax * vx + shard.ay * vy + shard.az * vz;
            float cx = shard.ay * vz - shard.az * vy;
            float cy = shard.az * vx - shard.ax * vz;
            float cz = shard.ax * vy - shard.ay * vx;
            p[i][0] = x + vx * c + cx * s + shard.ax * dot * one;
            p[i][1] = y + vy * c + cy * s + shard.ay * dot * one;
            p[i][2] = z + vz * c + cz * s + shard.az * dot * one;
        }
        for (int[] face : FACES) {
            float[] a = p[face[0]], b = p[face[1]], d = p[face[2]];
            float nx = (b[1] - a[1]) * (d[2] - a[2]) - (b[2] - a[2]) * (d[1] - a[1]);
            float ny = (b[2] - a[2]) * (d[0] - a[0]) - (b[0] - a[0]) * (d[2] - a[2]);
            float nz = (b[0] - a[0]) * (d[1] - a[1]) - (b[1] - a[1]) * (d[0] - a[0]);
            float fx = (a[0] + b[0] + d[0]) / 3 - x, fy = (a[1] + b[1] + d[1]) / 3 - y, fz = (a[2] + b[2] + d[2]) / 3 - z;
            if (nx * fx + ny * fy + nz * fz < 0) { nx = -nx; ny = -ny; nz = -nz; }
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-6f) continue;
            nx /= len; ny /= len; nz /= len;
            for (int v = 0; v < 3; v++) {
                float[] q = p[face[v]];
                out.addVertex(pose, q[0], q[1], q[2]).setColor(1f, 1f, 1f, alpha)
                        .setUv(v, v).setOverlay(255)
                        .setLight(240 | 240 << 16).setNormal(pose, nx, ny, nz);
            }
        }
    }

    private static void unit(Random random, float[] out) {
        double u = random.nextDouble() * 2 - 1;
        double phi = random.nextDouble() * Math.PI * 2;
        double s = Math.sqrt(1 - u * u);
        out[0] = (float) (s * Math.cos(phi));
        out[1] = (float) u;
        out[2] = (float) (s * Math.sin(phi));
    }

    private static float smoothStep(float x, float edge0, float edge1) {
        float t = Math.clamp((x - edge0) / edge1, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("voicechat", path); }

    private DeathGlassMelt() {}
}
