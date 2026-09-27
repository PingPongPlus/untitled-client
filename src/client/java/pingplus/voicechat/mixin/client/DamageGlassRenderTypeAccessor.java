package pingplus.voicechat.mixin.client;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.class)
public interface DamageGlassRenderTypeAccessor {
    @Accessor("state") RenderSetup voicechat$setup();
}
