package pingplus.voicechat.mixin.test;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Test-only access to vanilla input state; never included in the client JAR. */
@Mixin(Minecraft.class)
public interface MinecraftUseAccessor {
    @Accessor("rightClickDelay") int fastPlaceTest$delay();
    @Invoker("startUseItem") void fastPlaceTest$use();
}
