package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftPlaceDelayAccessor {
    @Accessor("rightClickDelay") int voicechat$placeDelay();
    @Accessor("rightClickDelay") void voicechat$setPlaceDelay(int ticks);
}
