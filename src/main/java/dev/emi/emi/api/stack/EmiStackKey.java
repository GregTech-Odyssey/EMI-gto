package dev.emi.emi.api.stack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Interned identity of an item or fluid with its NBT: equal content always yields the same instance, so keys compare with
 * {@code ==}. An integration may cache one object of its own per key in the attachment slot.
 */
@ApiStatus.Experimental
public abstract sealed class EmiStackKey permits EmiItemKey, EmiFluidKey {
    private final @Nullable CompoundTag tag;
    private final int hash = System.identityHashCode(this);
    private volatile @Nullable Object attachment;

    EmiStackKey(@Nullable CompoundTag tag) {
        this.tag = tag;
    }

    public abstract Object getPrimary();

    public abstract ResourceLocation getId();

    public final @Nullable CompoundTag getTag() {
        return tag;
    }

    public final @Nullable CompoundTag copyTag() {
        return tag != null ? tag.copy() : null;
    }

    public final @Nullable Object getAttachment() {
        return attachment;
    }

    public final void setAttachment(@Nullable Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }
}
