package dev.emi.emi.api.recipe;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.ItemStack;

/**
 * Durability information for a crafting ingredient. Implemented on every Item by EMI's mixin;
 * item subclasses can override these methods for custom durability rules.
 * All amounts describe a single item, independently of the stack count. Methods must not modify the stack.
 */
public interface EmiDurabilityTool {
    /**
     * @return Whether this ingredient uses durability when crafting.
     */
    @Contract(pure = true)
    boolean emi$isDurabilityTool(@NotNull ItemStack stack);

    /**
     * @return The nonnegative remaining durability of this ingredient.
     * Only queried when {@link #emi$isDurabilityTool(ItemStack)} returns true.
     */
    @Contract(pure = true)
    int emi$getRemainingDurability(@NotNull ItemStack stack);

    /**
     * @return The positive durability cost of one craft, in the same units as the remaining durability.
     */
    @Contract(pure = true)
    default int emi$getCraftingDurabilityCost(@NotNull ItemStack stack) {
        return 1;
    }
}
