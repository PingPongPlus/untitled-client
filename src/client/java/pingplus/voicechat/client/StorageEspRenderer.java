package pingplus.voicechat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;

/** Always-on-top gizmos keep storage boxes visible behind opaque terrain. */
public final class StorageEspRenderer implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues,
                           Frustum frustum, float partialTicks) {
        Minecraft client = Minecraft.getInstance();
        if (!PlayerSettings.storageEsp || client.player == null || client.level == null || client.gui.hud.isHidden()
                || client.gui.screen() != null) return;
        for (var target : StorageEspFeature.targets(client)) {
            var bounds = target.bounds();
            if (!StorageEspFeature.current(client, target) || !frustum.isVisible(bounds)) continue;
            int color = target.kind().color;
            var style = PlayerSettings.storageEspFill ? GizmoStyle.strokeAndFill(color, 2, 0x24000000 | (color & 0xFFFFFF))
                    : GizmoStyle.stroke(color, 2);
            Gizmos.cuboid(bounds, style).setAlwaysOnTop();
        }
    }
}
