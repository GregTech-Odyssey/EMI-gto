package dev.emi.emi.bom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.emi.emi.TestBootstrap;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.stack.EmiItemKey;
import net.minecraft.world.item.Items;

class TreeCostTest {
    private static EmiItemKey stone;
    private static EmiItemKey diamond;

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.init();
        stone = new EmiItemKey(Items.STONE, null);
        diamond = new EmiItemKey(Items.DIAMOND, null);
    }

    @Test
    void equalIngredientCopiesMergeCostsAndConsumeSharedInventoryRemainders() {
        var first = input(2, 1);
        var second = input(3, 1);
        var root = root(first, second);
        var cost = new TreeCost();

        cost.calculate(root, 1);
        assertEquals(1, cost.costs.size());
        assertEquals(5, cost.costs.get(ItemEmiStack.ofKey(stone, 1)).amount);

        var stored = ItemEmiStack.ofKey(stone, 4);
        try (var api = mockStatic(EmiApi.class)) {
            var inventory = new EmiPlayerInventory(List.of(stored));
            cost.calculateProgress(root, 1, inventory);
        }
        assertEquals(1, cost.costs.size());
        assertEquals(1, cost.costs.get(ItemEmiStack.ofKey(stone, 1)).amount);
        assertEquals(ProgressState.COMPLETED, first.progress);
        assertEquals(ProgressState.PARTIAL, second.progress);
        assertEquals(4, stored.getAmount());
    }

    @Test
    void mergedChanceCostsKeepWeightedChanceAndLargestMinimumBatch() {
        var cost = new TreeCost();
        cost.calculate(root(input(2, 0.5f), input(6, 0.25f)), 1);

        assertTrue(cost.costs.isEmpty());
        assertEquals(1, cost.chanceCosts.size());
        var merged = cost.chanceCosts.get(ItemEmiStack.ofKey(stone, 1));
        assertEquals(8, merged.amount);
        assertEquals(0.3125f, merged.chance);
        assertEquals(6, merged.minBatch);
        assertEquals(6, merged.getEffectiveAmount());
    }

    private static MaterialNode input(long amount, float chance) {
        var node = new MaterialNode(ItemEmiStack.ofKey(stone, 1));
        node.amount = amount;
        node.consumeChance = chance;
        return node;
    }

    private static MaterialNode root(MaterialNode... children) {
        EmiStack output = ItemEmiStack.ofKey(diamond, 1);
        var root = new MaterialNode(output);
        root.recipe = mock(EmiRecipe.class);
        when(root.recipe.getOutputs()).thenReturn(List.of(output));
        root.children = List.of(children);
        return root;
    }
}
