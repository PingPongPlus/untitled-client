package pingplus.voicechat.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import pingplus.voicechat.client.VoiceStatus;

public final class VoiceNametag {
    private static final FontDescription FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("voicechat", "voice_status"));

    private VoiceNametag() {}

    public static Component decorate(Component name, VoiceStatus status) {
        // A separate root prevents team colors/fonts/bold/obfuscation from leaking into the icon.
        return Component.empty().append(name).append(Component.literal(" "))
                .append(Component.literal(status.glyph).setStyle(Style.EMPTY.withFont(FONT)
                        .withColor(status.color).withBold(false).withItalic(false)
                        .withObfuscated(false).withUnderlined(false).withStrikethrough(false)));
    }
}
