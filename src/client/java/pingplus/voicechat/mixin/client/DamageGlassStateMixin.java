package pingplus.voicechat.mixin.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pingplus.voicechat.client.damageglass.DamageGlassState;

@Mixin(LivingEntityRenderState.class)
public abstract class DamageGlassStateMixin implements DamageGlassState {
    @Unique private int voicechat$glass;
    @Unique private int voicechat$center;
    public int voicechat$impactCenter() { return voicechat$center; }
    public void voicechat$impactCenter(int packed) { voicechat$center = packed; }
    public int voicechat$damageGlass() { return voicechat$glass; }
    public void voicechat$damageGlass(int packed) { voicechat$glass = packed; }
}
