package pingplus.voicechat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import pingplus.voicechat.client.FastPlaceFeature;

@Mixin(BlockItem.class)
public abstract class FastPlaceMixin {
    @WrapMethod(method = "place")
    private InteractionResult voicechat$placeDelay(BlockPlaceContext context, Operation<InteractionResult> original) {
        // Only the client prediction of our own placement can change the input delay.
        Minecraft client = context.getLevel().isClientSide() ? Minecraft.getInstance() : null;
        // Capture before placing consumes the last block in a survival stack.
        boolean active = client != null && context.getPlayer() == client.player
                && FastPlaceFeature.active(client, context.getItemInHand());
        InteractionResult result = original.call(context);
        // This method succeeds only after placement; container interactions never reach it.
        if (active && result instanceof InteractionResult.Success) {
            var input = (MinecraftPlaceDelayAccessor)client;
            input.voicechat$setPlaceDelay(Math.min(input.voicechat$placeDelay(), FastPlaceFeature.delayTicks()));
        }
        return result;
    }
}
