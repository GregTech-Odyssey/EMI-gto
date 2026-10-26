package dev.emi.emi.mixin;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.emi.emi.api.recipe.EmiDurabilityTool;

@Mixin(Item.class)
public abstract class ItemMixin implements EmiDurabilityTool {
    @Override
    public boolean emi$isDurabilityTool(@NotNull ItemStack stack) {
        return stack.isDamageableItem();
    }

    @Override
    public int emi$getRemainingDurability(@NotNull ItemStack stack) {
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }
}
