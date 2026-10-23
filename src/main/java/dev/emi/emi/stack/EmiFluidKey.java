package dev.emi.emi.stack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

@ApiStatus.Internal
public final class EmiFluidKey extends EmiStackKey {
    private final Fluid fluid;

    @ApiStatus.Internal
    public EmiFluidKey(Fluid fluid, @Nullable CompoundTag tag) {
        super(tag);
        this.fluid = fluid;
    }

    public static EmiFluidKey of(Fluid fluid) {
        return of(fluid, null);
    }

    @SuppressWarnings("unchecked")
    public static EmiFluidKey of(Fluid fluid, @Nullable CompoundTag tag) {
        return EmiKeyHolder.intern((EmiKeyHolder<EmiFluidKey>) fluid, tag);
    }

    public Fluid getFluid() {
        return fluid;
    }

    @Override
    public ResourceLocation getId() {
        return BuiltInRegistries.FLUID.getKey(fluid);
    }
}
