package pingplus.voicechat.mixin.client;

import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface DamageGlassSetupAccessor {
    @Accessor("textures") Map<String, ?> voicechat$textures();
    @Accessor("layeringTransform") LayeringTransform voicechat$layering();
}
