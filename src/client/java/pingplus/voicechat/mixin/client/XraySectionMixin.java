package pingplus.voicechat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import pingplus.voicechat.client.XrayFeature;

@Mixin(SectionCompiler.class)
public abstract class XraySectionMixin {
    @Inject(method = "compile", at = @At("HEAD"))
    private void voicechat$filter(SectionPos section, RenderSectionRegion region, VertexSorting sorting,
                                 SectionBufferBuilderPack buffers, CallbackInfoReturnable<SectionCompiler.Results> ci,
                                 @Share("xray") LocalRef<XrayFeature.Filter> filter) {
        filter.set(((XrayFeature.FilteredRegion)region).voicechat$xrayFilter());
    }

    @ModifyArgs(method = "compile", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;<init>(ZZLnet/minecraft/client/color/block/BlockColors;)V"))
    private void voicechat$showBuriedFaces(Args args, @Share("xray") LocalRef<XrayFeature.Filter> filter) {
        if (filter.get().enabled()) { args.set(0, false); args.set(1, false); }
    }

    @WrapOperation(method = "compile", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/chunk/VisGraph;setOpaque(Lnet/minecraft/core/BlockPos;)V"))
    private void voicechat$openVisibility(VisGraph graph, BlockPos pos, Operation<Void> original,
                                          @Share("xray") LocalRef<XrayFeature.Filter> filter) {
        if (!filter.get().enabled()) original.call(graph, pos);
    }
}
