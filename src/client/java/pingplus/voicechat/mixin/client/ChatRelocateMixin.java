package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pingplus.voicechat.client.hud.ChatHud;

/**
 * Routes every vanilla chat draw into the movable Chat widget.
 * The gameplay render already runs inside the widget pose; the chat screen gets
 * the same transform plus the widget glass panel. Click targets are relocated
 * separately in ChatClickRelocateMixin.
 */
@Mixin(ChatComponent.class)
public abstract class ChatRelocateMixin {
    @Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I"))
    private int voicechat$widgetAnchor(GuiGraphicsExtractor g) {
        Integer logical = ChatHud.logicalAnchor();
        if (logical != null) return logical; // Gameplay render: the widget pose is already applied.
        if (!ChatHud.INSTANCE.isActive()) return g.guiHeight();
        var bounds = ChatHud.screenBounds();
        g.pose().translate((float) bounds.x(), (float) bounds.y());
        g.pose().scale((float) bounds.scale());
        ChatHud.INSTANCE.renderPanel(g);
        g.pose().translate(ChatHud.INSTANCE.textOffsetX(), 0);
        return ChatHud.INSTANCE.renderAnchor();
    }
}
