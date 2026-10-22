package dev.emi.emi.mixin.key;

import com.gto.fastcollection.cache.WeakValueHashCache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

import dev.emi.emi.api.stack.EmiFluidKey;
import dev.emi.emi.stack.EmiKeyHolder;

@Mixin(Fluid.class)
public class FluidKeyMixin implements EmiKeyHolder<EmiFluidKey> {
    @Unique
    private volatile EmiFluidKey emi$untaggedKey;
    @Unique
    private volatile WeakValueHashCache<CompoundTag, EmiFluidKey> emi$taggedKeys;

    @Override
    public EmiFluidKey emi$getUntaggedKey() {
        var key = emi$untaggedKey;
        return key != null ? key : emi$createUntaggedKey();
    }

    @Unique
    private synchronized EmiFluidKey emi$createUntaggedKey() {
        var key = emi$untaggedKey;
        if (key == null) {
            emi$untaggedKey = key = new EmiFluidKey((Fluid) (Object) this, null);
        }
        return key;
    }

    @Override
    public WeakValueHashCache<CompoundTag, EmiFluidKey> emi$getTaggedKeys() {
        var cache = emi$taggedKeys;
        return cache != null ? cache : emi$createTaggedKeys();
    }

    @Unique
    private synchronized WeakValueHashCache<CompoundTag, EmiFluidKey> emi$createTaggedKeys() {
        var cache = emi$taggedKeys;
        if (cache == null) {
            emi$taggedKeys = cache = new WeakValueHashCache<>(tag -> new EmiFluidKey((Fluid) (Object) this, tag));
        }
        return cache;
    }
}
