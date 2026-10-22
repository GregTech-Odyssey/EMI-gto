package dev.emi.emi.handler;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class CookingRecipeHandler<T extends AbstractFurnaceMenu> implements StandardRecipeHandler<T> {
    private final EmiRecipeCategory category;

    public CookingRecipeHandler(EmiRecipeCategory category) {
        this.category = category;
    }

    @Override
    public List<Slot> getInputSources(T handler) {
        List<Slot> list = new ObjectArrayList<>();
        list.add(handler.getSlot(0));
        int invStart = 3;
        for (int i = invStart; i < invStart + 36; i++) { 
            list.add(handler.getSlot(i));
        }
        return list;
    }
    
    @Override
    public List<Slot> getCraftingSlots(T handler) {
        return List.of(handler.slots.get(0));
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == category && recipe.supportsRecipeTree();
    }
}
