package pingplus.voicechat.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pingplus.voicechat.client.AutoToolsFeature;

@Mixin(MultiPlayerGameMode.class)
public abstract class AutoToolsMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow protected abstract void ensureHasSentCarriedItem();

    @Inject(method = {"startDestroyBlock", "continueDestroyBlock"}, at = @At("HEAD"))
    private void voicechat$selectTool(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> ci) {
        if (AutoToolsFeature.mine(minecraft, pos)) ensureHasSentCarriedItem();
    }

    @Inject(method = "stopDestroyBlock", at = @At("TAIL"))
    private void voicechat$restoreTool(CallbackInfo ci) {
        if (AutoToolsFeature.finish(minecraft)) ensureHasSentCarriedItem();
    }
}
