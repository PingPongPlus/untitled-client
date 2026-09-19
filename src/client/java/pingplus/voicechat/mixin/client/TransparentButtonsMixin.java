package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class TransparentButtonsMixin {
    @Inject(
            method = "extractDefaultSprite",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hideButtonBackground(
            GuiGraphicsExtractor graphics,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}