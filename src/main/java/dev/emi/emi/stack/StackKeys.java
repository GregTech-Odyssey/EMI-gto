package dev.emi.emi.stack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Interned item and fluid keys behind EMI stacks: Applied Energistics' own keys when it is loaded, so both mods share one
 * set of key objects, otherwise {@link EmiItemKey} and {@link EmiFluidKey}.
 */
@ApiStatus.Internal
public final class StackKeys {
    private static final Backend BACKEND = StackKeyMixinPlugin.useAeKeys() ? new AeStackKeys() : new EmiStackKeys();

    private StackKeys() {
    }

    public static Object ofItem(Item item, @Nullable CompoundTag tag) {
        return BACKEND.ofItem(item, tag);
    }

    public static Object ofFluid(Fluid fluid, @Nullable CompoundTag tag) {
        return BACKEND.ofFluid(fluid, tag);
    }

    public static Item getItem(Object key) {
        return BACKEND.getItem(key);
    }

    public static Fluid getFluid(Object key) {
        return BACKEND.getFluid(key);
    }

    public static @Nullable CompoundTag getTag(Object key) {
        return BACKEND.getTag(key);
    }

    public static ResourceLocation getId(Object key) {
        return BACKEND.getId(key);
    }

    public static ItemStack getReadOnlyStack(Object key) {
        return BACKEND.getReadOnlyStack(key);
    }

    public static ItemStack toStack(Object key, int count) {
        return BACKEND.toStack(key, count);
    }

    interface Backend {

        Object ofItem(Item item, @Nullable CompoundTag tag);

        Object ofFluid(Fluid fluid, @Nullable CompoundTag tag);

        Item getItem(Object key);

        Fluid getFluid(Object key);

        @Nullable
        CompoundTag getTag(Object key);

        ResourceLocation getId(Object key);

        ItemStack getReadOnlyStack(Object key);

        ItemStack toStack(Object key, int count);
    }
}
