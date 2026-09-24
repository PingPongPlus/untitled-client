package pingplus.voicechat.mixin.test;

import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.GlassGpuTiming;

/** Test-only asynchronous GPU timestamps; no glFinish or render-thread stalls. */
@Mixin(GuiRenderer.class)
public abstract class GlassGpuTimingMixin {
    @Unique private GpuQueryPool glassTest$query;
    @Unique private boolean glassTest$pending;
    @Unique private boolean glassTest$started;

    @Inject(method = "render", at = @At("HEAD"))
    private void beginTiming(CallbackInfo ci) {
        if (glassTest$pending) {
            var start = glassTest$query.getValue(0);
            var end = glassTest$query.getValue(1);
            if (start.isEmpty() || end.isEmpty()) return;
            GlassGpuTiming.record(end.getAsLong() - start.getAsLong());
            glassTest$pending = false;
        }
        if (!GlassGpuTiming.enabled) return;
        if (glassTest$query == null) glassTest$query = RenderSystem.getDevice().createTimestampQueryPool(2);
        RenderSystem.getDevice().createCommandEncoder().writeTimestamp(glassTest$query, 0);
        glassTest$started = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void endTiming(CallbackInfo ci) {
        if (!glassTest$started) return;
        RenderSystem.getDevice().createCommandEncoder().writeTimestamp(glassTest$query, 1);
        glassTest$pending = true;
        glassTest$started = false;
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeTiming(CallbackInfo ci) {
        if (glassTest$query != null) glassTest$query.close();
    }
}
