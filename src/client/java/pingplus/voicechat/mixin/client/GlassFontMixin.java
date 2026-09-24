package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/** Route default text and its measurements through the same font as the ClickGUI. */
@Mixin(FontManager.class)
public abstract class GlassFontMixin {
    @Unique private static final Identifier UI_FONT = Identifier.fromNamespaceAndPath("voicechat", "ui");
    @Shadow @Final private Map<Identifier, FontSet> fontSets;

    @ModifyVariable(method = "getFontSetRaw", at = @At("HEAD"), argsOnly = true)
    private Identifier globalUiFont(Identifier id) {
        // Keep explicit icon fonts and the accessibility Unicode setting intact.
        if (id.equals(Minecraft.DEFAULT_FONT) && fontSets.containsKey(UI_FONT)
                && !Minecraft.getInstance().options.forceUnicodeFont().get()) return UI_FONT;
        return id;
    }
}
