package dev.emi.emi.stack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Interned identity of an item or fluid with its NBT, used when Applied Energistics is absent: equal content always
 * yields the same instance, so keys compare with {@code ==}.
 */
@ApiStatus.Internal
public abstract sealed class EmiStackKey permits EmiItemKey, EmiFluidKey {
    private final @Nullable CompoundTag tag;

    EmiStackKey(@Nullable CompoundTag tag) {
        this.tag = tag;
    }

    public abstract ResourceLocation getId();

    public final @Nullable CompoundTag getTag() {
        return tag;
    }

    public final @Nullable CompoundTag copyTag() {
        return tag != null ? tag.copy() : null;
    }
}
