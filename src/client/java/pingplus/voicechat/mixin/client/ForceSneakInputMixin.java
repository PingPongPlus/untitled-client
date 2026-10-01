package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.PlayerSettings;

/** Apply Sneak to gameplay input after vanilla reads keys, including in inventories. */
@Mixin(KeyboardInput.class)
public abstract class ForceSneakInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void voicechat$forceSneak(CallbackInfo ci) {
        if (!PlayerSettings.forceSneak) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !client.player.isAlive() || client.player.isSpectator()) return;
        var keys = keyPresses;
        if (!keys.shift()) keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
                keys.jump(), true, keys.sprint());
    }
}
