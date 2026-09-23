package pingplus.voicechat.client.gui.glass;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.IOException;

/** Loaded directly from the mod, so the first loading screen does not depend on pack reload. */
public final class LoadingTitleTexture extends ReloadableTexture {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("voicechat", "textures/gui/loading_title.png");

    public LoadingTitleTexture() { super(ID); }

    @Override public TextureContents loadContents(ResourceManager resources) throws IOException {
        try (var stream = LoadingTitleTexture.class.getResourceAsStream("/assets/voicechat/textures/gui/loading_title.png")) {
            if (stream == null) throw new IOException("Missing bundled loading title");
            return new TextureContents(NativeImage.read(stream),
                    new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0));
        }
    }
}
