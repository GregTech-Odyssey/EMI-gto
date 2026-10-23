package dev.emi.emi.stack;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;

final class AeStackKeys implements StackKeys.Backend {

    @Override
    public Object ofItem(Item item, @Nullable CompoundTag tag) {
        return AEItemKey.of(item, tag);
    }

    @Override
    public Object ofFluid(Fluid fluid, @Nullable CompoundTag tag) {
        return AEFluidKey.of(fluid, tag);
    }

    @Override
    public Item getItem(Object key) {
        return ((AEItemKey) key).getItem();
    }

    @Override
    public Fluid getFluid(Object key) {
        return ((AEFluidKey) key).getFluid();
    }

    @Override
    public @Nullable CompoundTag getTag(Object key) {
        return key instanceof AEItemKey item ? item.getTag() : ((AEFluidKey) key).getTag();
    }

    @Override
    public ResourceLocation getId(Object key) {
        return ((AEKey) key).getId();
    }

    @Override
    public ItemStack getReadOnlyStack(Object key) {
        return ((AEItemKey) key).getReadOnlyStack();
    }

    @Override
    public ItemStack toStack(Object key, int count) {
        return ((AEItemKey) key).toStack(count);
    }
}
