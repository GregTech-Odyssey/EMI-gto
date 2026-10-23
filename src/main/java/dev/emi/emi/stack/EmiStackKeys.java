package dev.emi.emi.stack;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

final class EmiStackKeys implements StackKeys.Backend {

    @Override
    public Object ofItem(Item item, @Nullable CompoundTag tag) {
        return EmiItemKey.of(item, tag);
    }

    @Override
    public Object ofFluid(Fluid fluid, @Nullable CompoundTag tag) {
        return EmiFluidKey.of(fluid, tag);
    }

    @Override
    public Item getItem(Object key) {
        return ((EmiItemKey) key).getItem();
    }

    @Override
    public Fluid getFluid(Object key) {
        return ((EmiFluidKey) key).getFluid();
    }

    @Override
    public @Nullable CompoundTag getTag(Object key) {
        return ((EmiStackKey) key).getTag();
    }

    @Override
    public ResourceLocation getId(Object key) {
        return ((EmiStackKey) key).getId();
    }

    @Override
    public ItemStack getReadOnlyStack(Object key) {
        return ((EmiItemKey) key).getReadOnlyStack();
    }

    @Override
    public ItemStack toStack(Object key, int count) {
        return ((EmiItemKey) key).toStack(count);
    }
}
