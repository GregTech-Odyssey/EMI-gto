package dev.emi.emi.runtime;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Client-thread render cache, cleared once per tick. Craft actions must read the handler directly. */
public final class EmiInventorySnapshotCache {
    private final Reference2ObjectOpenHashMap<EmiRecipeHandler<?>, EmiPlayerInventory> inventories = new Reference2ObjectOpenHashMap<>();
    private AbstractContainerScreen<?> screen;

    public <T extends AbstractContainerMenu> EmiPlayerInventory get(EmiRecipeHandler<T> handler,
            AbstractContainerScreen<T> screen) {
        if (this.screen != screen) {
            clear();
            this.screen = screen;
        }
        var inventory = inventories.get(handler);
        if (inventory == null) {
            inventory = handler.getInventory(screen);
            inventories.put(handler, inventory);
        }
        return inventory;
    }

    public void clear() {
        inventories.clear();
        screen = null;
    }
}
