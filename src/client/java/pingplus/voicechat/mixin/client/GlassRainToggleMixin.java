package pingplus.voicechat.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.gui.glass.GlassRainSettings;

@Mixin(TitleScreen.class)
public abstract class GlassRainToggleMixin extends Screen {
    protected GlassRainToggleMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void addGlassToggle(CallbackInfo ci) {
        addRenderableWidget(Button.builder(voicechat$glassLabel(), button -> {
            boolean saved = GlassRainSettings.setEnabled(!GlassRainSettings.isEnabled());
            button.setMessage(voicechat$glassLabel());
            button.setTooltip(Tooltip.create(Component.literal(saved
                    ? "Toggle the animated glass raindrops."
                    : "Could not save preference. Check the client log.")));
        }).bounds(width - 112, 6, 106, 20)
                .tooltip(Tooltip.create(Component.literal("Toggle the animated glass raindrops.")))
                .build());
    }

    @Unique private static Component voicechat$glassLabel() {
        return Component.literal("Glass drops: " + (GlassRainSettings.isEnabled() ? "ON" : "OFF"));
    }
}
