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
    private void drawHoverBackground(
            GuiGraphicsExtractor graphics,
            CallbackInfo ci
    ) {
        AbstractButton button = (AbstractButton) (Object) this;

        if (button.active && button.isHoveredOrFocused()) {
            int opacity = Math.round(40 * button.getAlpha());
            int color = (opacity << 24) | 0xFFFFFF;

            graphics.fill(
                    button.getX(),
                    button.getY(),
                    button.getX() + button.getWidth(),
                    button.getY() + button.getHeight(),
                    color
            );
        }

        // Skip the original background, including when not hovered.
        ci.cancel();
    }
}