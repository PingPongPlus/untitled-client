package pingplus.voicechat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Client-side estimate of the first impact of a held projectile. */
public final class ProjectilePreviewRenderer extends EntityHitboxDebugRenderer {
    private static final int PATH_COLOR = 0xB07FDBFF;
    private static final int BLOCK_COLOR = 0xE0FFCC70;
    private static final int ENTITY_COLOR = 0xE0FF606C;
    private static final int ACTUAL_COLOR = 0xE060FFAA;
    private static final int MAX_STEPS = 100;
    private static Vec3 lastImpact;
    private static long lastImpactAt;

    public static void recordImpact(Vec3 location) {
        lastImpact = location;
        lastImpactAt = System.currentTimeMillis();
    }

    public ProjectilePreviewRenderer() {
        super(Minecraft.getInstance());
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ,
                           DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (!PlayerSettings.projectilePreview || player == null || client.level == null
                || client.gui.screen() != null) return;

        if (lastImpact != null && System.currentTimeMillis() - lastImpactAt < 4000) {
            Gizmos.point(lastImpact, ACTUAL_COLOR, 10.0f);
            Gizmos.circle(lastImpact, 0.35f, net.minecraft.gizmos.GizmoStyle.stroke(ACTUAL_COLOR, 2));
        }

        ItemStack held = player.isUsingItem() ? player.getUseItem() : player.getMainHandItem();
        Item item = held.getItem();
        double speed;
        double gravity;
        double pitchOffset = 0;
        if (item instanceof BowItem) {
            if (!player.isUsingItem()) return;
            float power = BowItem.getPowerForTime(player.getTicksUsingItem());
            if (power < 0.1f) return;
            speed = power * 3.0;
            gravity = 0.05;
        } else if (item instanceof EnderpearlItem || item instanceof SnowballItem || item instanceof EggItem) {
            speed = 1.5;
            gravity = 0.03;
        } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            speed = 0.5;
            gravity = 0.05;
            pitchOffset = -20;
        } else {
            return;
        }

        double yaw = Math.toRadians(player.getYRot());
        double pitch = Math.toRadians(player.getXRot() + pitchOffset);
        Vec3 direction = new Vec3(-Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 motion = direction.scale(speed);
        Vec3 playerMotion = player.getDeltaMovement();
        motion = motion.add(playerMotion.x, player.onGround() ? 0 : playerMotion.y, playerMotion.z);
        Vec3 pos = player.getEyePosition(partialTicks).add(0, -0.1, 0);

        for (int step = 0; step < MAX_STEPS; step++) {
            Vec3 next = pos.add(motion);
            HitResult block = client.level.clip(new ClipContext(pos, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = block.getType() == HitResult.Type.MISS ? next : block.getLocation();
            int impactColor = BLOCK_COLOR;

            AABB search = new AABB(pos, end).inflate(0.5);
            for (Entity entity : client.level.getEntities(player, search,
                    other -> other.isPickable() && !other.isSpectator())) {
                Vec3 hit = entity.getBoundingBox().inflate(0.2).clip(pos, end).orElse(null);
                if (hit != null && hit.distanceToSqr(pos) < end.distanceToSqr(pos)) {
                    end = hit;
                    impactColor = ENTITY_COLOR;
                }
            }

            Gizmos.line(pos, end, PATH_COLOR, 1.5f);
            if (impactColor == ENTITY_COLOR || block.getType() != HitResult.Type.MISS) {
                Gizmos.point(end, impactColor, 8.0f);
                return;
            }
            pos = next;
            motion = motion.scale(0.99).add(0, -gravity, 0);
            if (pos.y < client.level.getMinY() - 4) return;
        }
    }
}
