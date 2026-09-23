package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.glass.GlassTransition;

@Mixin(Screen.class)
public abstract class GlassTransitionScreenMixin {
    @Inject(method="extractBackground",at=@At("HEAD"),cancellable=true)
    private void glassBackground(GuiGraphicsExtractor g,int mx,int my,float dt,CallbackInfo ci) {
        Object self=this;
        if (self instanceof ProgressScreen || self instanceof ConnectScreen) { GlassTransition.draw(g); ci.cancel(); }
    }
}
