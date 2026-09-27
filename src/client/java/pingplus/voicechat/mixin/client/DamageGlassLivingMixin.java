package pingplus.voicechat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.damageglass.*;

@Mixin(LivingEntityRenderer.class)
public abstract class DamageGlassLivingMixin {
    @Unique private static final java.util.Map<LivingEntity, DamageGlassTiming.Envelope> voicechat$envelopes = new java.util.WeakHashMap<>();
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void voicechat$hurtState(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        boolean eligible = DamageGlassTiming.eligible(DamageGlassSettings.enabled(), DamageGlassSettings.players(),
                DamageGlassSettings.mobs(), entity instanceof Player, entity instanceof Mob,
                entity.isInvisible(), entity == client.getCameraEntity() && client.options.getCameraType().isFirstPerson());
        int packed = 0;
        if (eligible && entity.hurtTime > 0) {
            packed = voicechat$envelopes.computeIfAbsent(entity, ignored -> new DamageGlassTiming.Envelope())
                    .update(entity.hurtTime, entity.hurtDuration, partial);
        } else voicechat$envelopes.remove(entity);
        ((DamageGlassState) state).voicechat$damageGlass(packed);
    }

    @WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    private void voicechat$glassScope(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                     CameraRenderState camera, Operation<Void> original) {
        int previous = DamageGlassRenderer.current;
        int packed = DamageGlassRenderer.inWorld ? ((DamageGlassState) state).voicechat$damageGlass() : 0;
        DamageGlassRenderer.current = packed;
        boolean red = state.hasRedOverlay;
        // The material carries its own blend. Do not mutate extracted state after submission.
        if (packed != 0) state.hasRedOverlay = false;
        try { original.call(state, pose, collector, camera); }
        finally { state.hasRedOverlay = red; DamageGlassRenderer.current = previous; }
    }
}
