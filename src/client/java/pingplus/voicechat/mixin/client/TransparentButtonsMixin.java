package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.glass.GlassButtonRenderer;

@Mixin(AbstractButton.class)
public abstract class TransparentButtonsMixin {
    @Inject(
            method = "extractDefaultSprite",
            at = @At("HEAD"),
            cancellable = true
    )
    private void drawHoverBackground(
            GuiGraphicsExtractor graphics,
            CallbackInfo ci
    ) {
        AbstractButton button = (AbstractButton) (Object) this;

        GlassButtonRenderer.draw(graphics, button);

        // Replace the sprite only; Minecraft still renders labels and handles input.
        ci.cancel();
    }
}
