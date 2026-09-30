package pingplus.voicechat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Shortens vanilla's repeat delay after a successful block-item placement. */
public final class FastPlaceFeature {
    public static boolean active(Minecraft client, ItemStack stack) {
        return PlayerSettings.fastPlace && !stack.isEmpty() && stack.getItem() instanceof BlockItem
                && client.player != null && client.level != null && client.gameMode != null
                && client.gui.screen() == null && client.gui.overlay() == null && !client.isPaused()
                && client.isWindowActive() && client.player.isAlive() && !client.player.isSpectator()
                && !client.player.isUsingItem();
    }

    // One tick permits a held-button attempt every tick without a second attempt in the same tick.
    public static int delayTicks() { return Math.clamp(PlayerSettings.fastPlaceDelayTicks, 1, 4); }
    private FastPlaceFeature() { }
}
