package dev.emi.emi.stack;

import com.gto.fastcollection.cache.WeakValueHashCache;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;

/**
 * Per item or fluid storage of interned stack keys, added to {@code Item} and {@code Fluid} by mixin.
 */
@ApiStatus.Internal
public interface EmiKeyHolder<K extends EmiStackKey> {

    K emi$getUntaggedKey();

    WeakValueHashCache<CompoundTag, K> emi$getTaggedKeys();

    static <K extends EmiStackKey> K intern(EmiKeyHolder<K> holder, @Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return holder.emi$getUntaggedKey();
        }
        var cache = holder.emi$getTaggedKeys();
        return cache.getCache(tag, cache.createFunction(), CompoundTag::copy);
    }
}
