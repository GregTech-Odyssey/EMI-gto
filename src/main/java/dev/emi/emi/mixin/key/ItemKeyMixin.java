package dev.emi.emi.mixin.key;

import com.gto.fastcollection.cache.WeakValueHashCache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

import dev.emi.emi.api.stack.EmiItemKey;
import dev.emi.emi.stack.EmiKeyHolder;

@Mixin(Item.class)
public class ItemKeyMixin implements EmiKeyHolder<EmiItemKey> {
    @Unique
    private volatile EmiItemKey emi$untaggedKey;
    @Unique
    private volatile WeakValueHashCache<CompoundTag, EmiItemKey> emi$taggedKeys;

    @Override
    public EmiItemKey emi$getUntaggedKey() {
        var key = emi$untaggedKey;
        return key != null ? key : emi$createUntaggedKey();
    }

    @Unique
    private synchronized EmiItemKey emi$createUntaggedKey() {
        var key = emi$untaggedKey;
        if (key == null) {
            emi$untaggedKey = key = new EmiItemKey((Item) (Object) this, null);
        }
        return key;
    }

    @Override
    public WeakValueHashCache<CompoundTag, EmiItemKey> emi$getTaggedKeys() {
        var cache = emi$taggedKeys;
        return cache != null ? cache : emi$createTaggedKeys();
    }

    @Unique
    private synchronized WeakValueHashCache<CompoundTag, EmiItemKey> emi$createTaggedKeys() {
        var cache = emi$taggedKeys;
        if (cache == null) {
            emi$taggedKeys = cache = new WeakValueHashCache<>(tag -> new EmiItemKey((Item) (Object) this, tag));
        }
        return cache;
    }
}
