package dev.emi.emi.api.stack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import dev.emi.emi.stack.EmiKeyHolder;

@ApiStatus.Experimental
public final class EmiItemKey extends EmiStackKey {
    private final Item item;
    private @Nullable ItemStack readOnlyStack;

    @ApiStatus.Internal
    public EmiItemKey(Item item, @Nullable CompoundTag tag) {
        super(tag);
        this.item = item;
    }

    public static EmiItemKey of(ItemLike item) {
        return of(item.asItem(), null);
    }

    @SuppressWarnings("unchecked")
    public static EmiItemKey of(Item item, @Nullable CompoundTag tag) {
        return EmiKeyHolder.intern((EmiKeyHolder<EmiItemKey>) item, tag);
    }

    public static EmiItemKey of(ItemStack stack) {
        return of(stack.getItem(), stack.getTag());
    }

    public Item getItem() {
        return item;
    }

    @Override
    public Object getPrimary() {
        return item;
    }

    @Override
    public ResourceLocation getId() {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public ItemStack getReadOnlyStack() {
        var stack = readOnlyStack;
        if (stack == null || stack.isEmpty()) {
            stack = new ItemStack(item, 1);
            stack.setTag(getTag());
            readOnlyStack = stack;
        }
        return stack;
    }

    public ItemStack toStack(int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        var result = new ItemStack(item, count);
        result.setTag(copyTag());
        return result;
    }
}
