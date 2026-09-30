package pingplus.voicechat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/** Selects a hotbar tool before vanilla sends the mining action. */
public final class AutoToolsFeature {
    private static LocalPlayer owner;
    private static int originalSlot = -1;
    private static int toolSlot = -1;
    private static boolean manualOverride;

    public static boolean mine(Minecraft client, BlockPos pos) {
        if (!active(client)) return finish(client);
        if (owner != client.player) clear();
        owner = client.player;
        var inventory = owner.getInventory();
        if (toolSlot >= 0 && inventory.getSelectedSlot() != toolSlot) {
            // Scrolling or pressing a hotbar key takes control until mining stops.
            originalSlot = toolSlot = -1;
            manualOverride = true;
        }
        if (manualOverride) return false;
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(client.level, pos) < 0) return false;
        int best = bestSlot(inventory, state);
        if (best == inventory.getSelectedSlot()) {
            toolSlot = best;
            return false;
        }
        if (originalSlot < 0) originalSlot = inventory.getSelectedSlot();
        inventory.setSelectedSlot(best);
        toolSlot = best;
        return true;
    }

    public static int bestSlot(Inventory inventory, BlockState state) {
        int best = inventory.getSelectedSlot();
        ItemStack current = inventory.getItem(best);
        boolean correct = !state.requiresCorrectToolForDrops() || current.isCorrectToolForDrops(state);
        float speed = miningSpeed(current, state);
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty()) continue;
            boolean suitable = !state.requiresCorrectToolForDrops() || candidate.isCorrectToolForDrops(state);
            float candidateSpeed = miningSpeed(candidate, state);
            if ((suitable && !correct) || (suitable == correct && candidateSpeed > speed)) {
                best = slot;
                correct = suitable;
                speed = candidateSpeed;
            }
        }
        return best;
    }

    private static float miningSpeed(ItemStack stack, BlockState state) {
        float speed = stack.getDestroySpeed(state);
        if (speed <= 1) return speed;
        // Use the item's data-driven Efficiency modifiers without equipping each candidate.
        var efficiency = new AttributeInstance(Attributes.MINING_EFFICIENCY, unused -> { });
        efficiency.setBaseValue(0);
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.MINING_EFFICIENCY)) efficiency.addOrUpdateTransientModifier(modifier);
        });
        return speed + (float)efficiency.getValue();
    }

    public static void tick(Minecraft client) {
        if (!active(client) || !client.options.keyAttack.isDown()
                || client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) finish(client);
    }

    public static boolean finish(Minecraft client) {
        boolean restore = owner != null && owner == client.player && owner.isAlive() && !owner.isSpectator()
                && PlayerSettings.autoToolsRestore && originalSlot >= 0
                && owner.getInventory().getSelectedSlot() == toolSlot;
        if (restore) owner.getInventory().setSelectedSlot(originalSlot);
        clear();
        return restore;
    }

    private static boolean active(Minecraft client) {
        return PlayerSettings.autoTools && client.player != null && client.level != null && client.gameMode != null
                && client.gui.screen() == null && client.gui.overlay() == null && !client.isPaused()
                && client.isWindowActive() && client.player.isAlive() && !client.player.isSpectator()
                && !client.player.isUsingItem();
    }

    private static void clear() { owner = null; originalSlot = toolSlot = -1; manualOverride = false; }
    private AutoToolsFeature() { }
}
