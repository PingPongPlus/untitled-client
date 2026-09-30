package pingplus.voicechat.mixin.client;

import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pingplus.voicechat.client.XrayFeature;

/** Filters only render snapshots, including Fabric's neighbor-face and lighting queries. */
@Mixin(RenderSectionRegion.class)
public abstract class XrayRegionMixin implements XrayFeature.FilteredRegion {
    @Unique private XrayFeature.Filter voicechat$xray;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void voicechat$captureFilter(CallbackInfo ci) { voicechat$xray = XrayFeature.filter(); }

    @Override public XrayFeature.Filter voicechat$xrayFilter() { return voicechat$xray; }

    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void voicechat$renderState(BlockPos pos, CallbackInfoReturnable<BlockState> ci) {
        if (voicechat$xray.enabled() && !voicechat$xray.includes(ci.getReturnValue()))
            ci.setReturnValue(Blocks.AIR.defaultBlockState());
    }
}
