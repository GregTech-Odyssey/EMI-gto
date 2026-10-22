package dev.emi.emi.api;

import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import dev.emi.emi.api.stack.EmiStack;

/**
 * Process-wide hooks for mods that integrate deeper than the plugin registry: stack amount rendering, items that wrap
 * fluids, and favorite list changes. Register them once during client setup.
 */
@ApiStatus.Experimental
public final class EmiHooks {
    private static volatile @Nullable AmountRenderer amountRenderer;
    private static final CopyOnWriteArrayList<FluidResolver> FLUID_RESOLVERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Runnable> FAVORITE_LISTENERS = new CopyOnWriteArrayList<>();

    private EmiHooks() {
    }

    public static void setAmountRenderer(@Nullable AmountRenderer renderer) {
        amountRenderer = renderer;
    }

    public static void addFluidResolver(FluidResolver resolver) {
        FLUID_RESOLVERS.add(resolver);
    }

    public static void addFavoritesListener(Runnable listener) {
        FAVORITE_LISTENERS.add(listener);
    }

    @ApiStatus.Internal
    public static boolean renderAmount(GuiGraphics draw, int x, int y, EmiStack stack, long amount) {
        var renderer = amountRenderer;
        return renderer != null && renderer.render(draw, x, y, stack, amount);
    }

    @ApiStatus.Internal
    public static @Nullable EmiStack resolveFluid(ItemStack stack) {
        for (var resolver : FLUID_RESOLVERS) {
            var fluid = resolver.resolve(stack);
            if (fluid != null) {
                return fluid;
            }
        }
        return null;
    }

    @ApiStatus.Internal
    public static void onFavoritesChanged() {
        for (var listener : FAVORITE_LISTENERS) {
            listener.run();
        }
    }

    @FunctionalInterface
    public interface AmountRenderer {

        boolean render(GuiGraphics draw, int x, int y, EmiStack stack, long amount);
    }

    @FunctionalInterface
    public interface FluidResolver {

        @Nullable
        EmiStack resolve(ItemStack stack);
    }
}
