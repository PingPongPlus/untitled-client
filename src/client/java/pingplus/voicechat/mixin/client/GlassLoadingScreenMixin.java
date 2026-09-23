package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.glass.GlassTransition;

@Mixin({GenericMessageScreen.class, LevelLoadingScreen.class})
public abstract class GlassLoadingScreenMixin {
    @Inject(method="extractBackground",at=@At("HEAD"),cancellable=true)
    private void glassBackground(GuiGraphicsExtractor g,int mx,int my,float dt,CallbackInfo ci) {
        GlassTransition.draw(g); ci.cancel();
    }
}
