package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.PlayerSettings;
import pingplus.voicechat.client.ProjectilePreviewRenderer;

@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void voicechat$recordImpact(HitResult hit, CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        Minecraft client = Minecraft.getInstance();
        if (!PlayerSettings.projectilePreview || client.player == null || client.level == null
                || projectile.level() != client.level || projectile.getOwner() != client.player) return;
        if (hit.getType() != HitResult.Type.MISS)
            ProjectilePreviewRenderer.recordImpact(hit.getLocation());
    }
}
