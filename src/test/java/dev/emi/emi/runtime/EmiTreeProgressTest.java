package dev.emi.emi.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Items;

import dev.emi.emi.TestBootstrap;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.bom.MaterialTree;
import dev.emi.emi.bom.ProgressState;
import dev.emi.emi.stack.EmiItemKey;

class EmiTreeProgressTest {
    @BeforeAll
    static void bootstrap() {
        TestBootstrap.init();
    }

    @AfterEach
    void resetMode() {
        BoM.craftingMode = false;
    }

    @Test
    void backgroundProgressOwnsNodesInventoryAndRecipeData() throws Exception {
        var input = stack(Items.COBBLESTONE, 2);
        var output = stack(Items.STONE, 1);
        var recipe = recipe(input, output);
        var tree = new MaterialTree(recipe);
        tree.batches = 3;
        var inventory = EmiPlayerInventory.empty();
        var available = stack(Items.COBBLESTONE, 4);
        inventory.inventory.put(available, available);
        var inventorySnapshot = inventory.copy();
        var snapshot = tree.copyForCalculation();

        // These getters may belong to plugins. They must be read before dispatch, on the client thread.
        when(recipe.getInputs()).thenThrow(new AssertionError("Live recipe inputs on worker"));
        when(recipe.getOutputs()).thenThrow(new AssertionError("Live recipe outputs on worker"));
        available.setAmount(100);
        tree.goal.children.getFirst().amount = 99;

        var result = CompletableFuture.supplyAsync(() -> EmiTreeProgress.calculate(snapshot, inventorySnapshot, true))
                .get(10, TimeUnit.SECONDS);

        assertEquals(6, result.total().costs.get(input).amount);
        assertEquals(2, result.calculated().cost.costs.get(input).amount);
        assertEquals(3, result.calculated().goal.neededBatches);
        assertEquals(0, tree.goal.neededBatches);
        assertEquals(4, inventorySnapshot.inventory.get(input).getAmount());
        assertEquals(100, available.getAmount());
        assertFalse(result.synthetic().complete());
        var crafting = result.synthetic().favorites().getFirst();
        assertEquals(1, crafting.state);
        assertEquals(3, crafting.batches);
    }

    @Test
    void completedGoalDoesNotConsumeMaterialsOrLeaveSyntheticRecipes() {
        var input = stack(Items.COBBLESTONE, 2);
        var output = stack(Items.STONE, 1);
        var tree = new MaterialTree(recipe(input, output));
        tree.batches = 3;
        var inventory = EmiPlayerInventory.empty();
        var available = stack(Items.STONE, 3);
        inventory.inventory.put(available, available);

        var result = EmiTreeProgress.calculate(tree.copyForCalculation(), inventory.copy(), true);

        assertTrue(result.synthetic().complete());
        assertTrue(result.synthetic().favorites().isEmpty());
        assertTrue(result.calculated().cost.costs.isEmpty());
        assertEquals(ProgressState.COMPLETED, result.calculated().goal.progress);
        assertEquals(ProgressState.UNSTARTED, tree.goal.progress);
        assertEquals(3, available.getAmount());
    }

    @Test
    void requestRejectsChangedInventoryTreeStructureBatchesScreenAndMode() {
        var input = stack(Items.COBBLESTONE, 2);
        var output = stack(Items.STONE, 1);
        var recipe = recipe(input, output);
        var tree = new MaterialTree(recipe);
        var inventory = EmiPlayerInventory.empty();
        var screen = new Object();
        BoM.craftingMode = true;
        var request = new EmiTreeProgress.Request(tree, tree.getRevision(), tree.batches, true, inventory, screen);

        assertTrue(request.matches(tree, inventory, screen));
        assertFalse(request.matches(tree, inventory.copy(), screen));
        assertFalse(request.matches(new MaterialTree(recipe), inventory, screen));
        assertFalse(request.matches(tree, inventory, new Object()));
        tree.batches = 2;
        assertFalse(request.matches(tree, inventory, screen));
        tree.batches = 1;
        BoM.craftingMode = false;
        assertFalse(request.matches(tree, inventory, screen));
        BoM.craftingMode = true;
        tree.recalculate();
        assertFalse(request.matches(tree, inventory, screen));
    }

    private static EmiRecipe recipe(EmiStack input, EmiStack output) {
        var recipe = mock(EmiRecipe.class);
        when(recipe.getInputs()).thenReturn(List.of(input));
        when(recipe.getOutputs()).thenReturn(List.of(output));
        return recipe;
    }

    private static EmiStack stack(net.minecraft.world.item.Item item, long amount) {
        return ItemEmiStack.ofKey(new EmiItemKey(item, null), amount);
    }
}
