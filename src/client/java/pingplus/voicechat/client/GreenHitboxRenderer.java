package pingplus.voicechat.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GreenHitboxRenderer extends EntityHitboxDebugRenderer {
    private static final GizmoStyle STYLE = GizmoStyle.strokeAndFill(
            0xFF00FF00, 1.0F, 0x4000FF00);

    public GreenHitboxRenderer() {
        super(Minecraft.getInstance());
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ,
                           DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!PlayerSettings.hitboxes || minecraft.level == null) {
            return;
        }

        for (Player player : minecraft.level.players()) {
            if (player == minecraft.getCameraEntity()
                    && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
                continue;
            }


            // Match the player's smooth rendered position between game ticks.
            float playerPartialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(
                    !minecraft.level.tickRateManager().isEntityFrozen(player));
            Vec3 offset = player.getPosition(playerPartialTicks).subtract(player.position());
            Gizmos.cuboid(player.getBoundingBox().move(offset), STYLE);
        }
    }
}
