package dev.emi.emi.bom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.emi.emi.TestBootstrap;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiDurabilityTool;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiResolutionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.emi.stack.EmiItemKey;

class DurabilityTreeCostTest {
    @BeforeAll
    static void bootstrap() {
        TestBootstrap.init();
    }

    @Test
    void oneSufficientToolCovers48CraftsAndBatchAvailability() {
        var item = item();
        var input = tool(item, 2500, 4, 0);
        var stored = tool(item, 2500, 4, 160);
        var tree = root(input);
        try (var api = mockStatic(EmiApi.class)) {
            var inventory = new EmiPlayerInventory(List.of(stored));
            var cost = new TreeCost();
            cost.calculate(tree, 48);
            assertEquals(1, cost.costs.get(input).amount);
            cost.calculateProgress(tree, 48, inventory.copy());
            assertTrue(cost.costs.isEmpty());
            assertEquals(ProgressState.COMPLETED, tree.children.getFirst().progress);
            assertTrue(inventory.canCraftInputs(List.of(input), 48));
            assertTrue(inventory.canCraftInputs(List.of(input), 48), "reservation resets between checks");
            assertEquals(1, stored.getAmount());
            verify(((ItemEmiStack) stored).getReadOnlyStack(), never()).setDamageValue(anyInt());
        }
    }

    @Test
    void differentMaterialsCanSatisfyTheSameToolIngredient() {
        var first = tool(item(), 80, 4, 0);
        var second = tool(item(), 2500, 4, 0);
        var alternatives = new ListEmiIngredient(List.of(first, second), 1);
        var tree = root(alternatives);
        try (var api = mockStatic(EmiApi.class)) {
            var inventory = new EmiPlayerInventory(List.of(second.copy()));
            var cost = new TreeCost();
            cost.calculateProgress(tree, 48, inventory);
            assertTrue(cost.costs.isEmpty());
            assertTrue(inventory.canCraftInputs(List.of(alternatives), 48));
        }
    }

    @Test
    void craftingWireCutterTagCanMixDisposableAndDurableAlternatives() {
        var durable = tool(item(), 2500, 4, 0);
        var disposable = ItemEmiStack.ofKey(new EmiItemKey(Items.STICK, null), 1);
        var key = TagKey.create(Registries.ITEM, new ResourceLocation("gtceu", "tools/crafting_wire_cutters"));
        var wrapped = mock(EmiTagKey.class);
        when(wrapped.id()).thenReturn(key.location());
        when(wrapped.raw()).thenReturn(key);
        try (var keys = mockStatic(EmiTagKey.class); var api = mockStatic(EmiApi.class)) {
            keys.when(() -> EmiTagKey.of(key)).thenReturn(wrapped);
            for (var alternatives : List.of(List.of(disposable, durable), List.of(durable, disposable))) {
                var tag = new TagEmiIngredient(key, alternatives, 1);
                var tree = root(tag);
                var cost = new TreeCost();
                cost.calculate(tree, 54);
                assertEquals(1, cost.costs.get(tag).amount, "plan a reusable tool even when disposable is first");
                var inventory = new EmiPlayerInventory(List.of(durable.copy()));
                cost.calculateProgress(tree.copyForCalculation(), 54, inventory.copy());
                assertTrue(cost.costs.isEmpty(), "one durable wire cutter covers all 54 operations");
                assertTrue(inventory.canCraftInputs(List.of(tag), 54));

                inventory = new EmiPlayerInventory(List.of(disposable.copy().setAmount(54)));
                cost.calculateProgress(tree, 54, inventory.copy());
                assertTrue(cost.costs.isEmpty(), "54 disposable tools provide 54 operations");
                assertTrue(inventory.canCraftInputs(List.of(tag), 54));
                assertFalse(inventory.canCraftInputs(List.of(tag), 55));

                inventory = new EmiPlayerInventory(List.of(disposable.copy().setAmount(14),
                    tool((Item) durable.getKey(), 160, 4, 2340)));
                assertTrue(inventory.canCraftInputs(List.of(tag), 54));
                assertFalse(inventory.canCraftInputs(List.of(tag), 55));
            }
        }
    }

    @Test
    void anExplicitMaterialResolutionUsesThatToolsCapacityForMissingTools() {
        var first = tool(item(), 80, 4, 0);
        var second = tool(item(), 2500, 4, 0);
        var alternatives = new ListEmiIngredient(List.of(first, second), 1);
        var selected = second.copy().setRemainder(EmiStack.EMPTY);
        var tree = root(alternatives);
        var child = tree.children.getFirst();
        child.recipe = new EmiResolutionRecipe(alternatives, selected);
        child.children = List.of(new MaterialNode(selected));
        var cost = new TreeCost();
        cost.calculate(tree, 48);
        assertEquals(1, cost.costs.get(selected).amount);
        assertFalse(cost.costs.containsKey(first));
    }

    @Test
    void wornToolsAreKeptSeparateAndSharedBranchesCannotSpendDurabilityTwice() {
        var item = item();
        var input = tool(item, 100, 4, 0);
        var first = tool(item, 20, 4, 80);
        var second = tool(item, 20, 4, 81);
        var tree = root();
        tree.children = List.of(root(input), root(input.copy()));
        try (var api = mockStatic(EmiApi.class)) {
            var inventory = new EmiPlayerInventory(List.of(first, second));
            var cost = new TreeCost();
            cost.calculateProgress(tree, 5, inventory.copy());
            assertTrue(cost.costs.isEmpty(), "two worn tools provide exactly ten uses");
            cost.calculateProgress(tree, 6, inventory.copy());
            assertEquals(1, cost.costs.get(input).amount, "twelve uses require one additional tool");
            assertFalse(inventory.canCraftInputs(List.of(input), 11));
            assertTrue(inventory.canCraftInputs(List.of(input), 10));
        }
    }

    @Test
    void toolTagCopiesKeepRemaindersAndCalculationSnapshotsKeepDurabilityMetadata() throws Exception {
        var first = tool(item(), 80, 4, 0);
        var second = tool(item(), 2500, 4, 0);
        var key = TagKey.create(Registries.ITEM, new ResourceLocation("gtceu", "tools/wire_cutters"));
        var wrapped = mock(EmiTagKey.class);
        when(wrapped.id()).thenReturn(key.location());
        when(wrapped.raw()).thenReturn(key);
        try (var keys = mockStatic(EmiTagKey.class); var api = mockStatic(EmiApi.class)) {
            keys.when(() -> EmiTagKey.of(key)).thenReturn(wrapped);
            var tag = new TagEmiIngredient(key, List.of(first, second), 1);
            assertSame(tag.getEmiStacks(), tag.copy().getEmiStacks());
            var tree = root(tag).copyForCalculation();
            assertTrue(tree.children.getFirst().durabilityTool);
            var inventory = new EmiPlayerInventory(List.of(second.copy())).copy();
            var provider = (EmiDurabilityTool) second.getKey();
            when(provider.emi$getRemainingDurability(any())).thenThrow(new AssertionError("live durability callback"));
            var result = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                var cost = new TreeCost();
                cost.calculateProgress(tree, 48, inventory);
                return cost;
            }).get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(result.costs.isEmpty());
            assertEquals(ProgressState.COMPLETED, tree.children.getFirst().progress);
        }
    }

    @Test
    void twoInputSlotsNeedTwoPhysicalToolsAndConsumedEquipmentStaysCounted() {
        var item = item();
        var input = tool(item, 2500, 4, 0);
        try (var api = mockStatic(EmiApi.class)) {
            var inventory = new EmiPlayerInventory(List.of(input.copy()));
            assertFalse(inventory.canCraftInputs(List.of(input, input.copy()), 1));
            var cost = new TreeCost();
            var doubled = input.copy().setAmount(2);
            cost.calculateProgress(root(doubled), 48, inventory);
            assertEquals(1, cost.costs.get(input).amount);

            var consumed = input.copy().setRemainder(EmiStack.EMPTY);
            assertFalse(inventory.canCraftInputs(List.of(input, consumed), 1));
            assertFalse(inventory.canCraftInputs(List.of(consumed, input), 1));
            cost.calculateProgress(root(consumed), 48, inventory);
            assertEquals(47, cost.costs.get(input).amount);
            assertFalse(inventory.canCraftInputs(List.of(consumed), 48));
        }
    }

    private static Item item() {
        return mock(Item.class, withSettings().extraInterfaces(EmiDurabilityTool.class));
    }

    private static EmiStack tool(Item item, int remaining, int cost, int damage) {
        var raw = mock(ItemStack.class);
        when(raw.getItem()).thenReturn(item);
        var provider = (EmiDurabilityTool) item;
        when(provider.emi$isDurabilityTool(raw)).thenReturn(true);
        when(provider.emi$getRemainingDurability(raw)).thenReturn(remaining);
        when(provider.emi$getCraftingDurabilityCost(raw)).thenReturn(cost);
        var key = mock(EmiItemKey.class);
        when(key.getItem()).thenReturn(item);
        when(key.getReadOnlyStack()).thenReturn(raw);
        var tag = new CompoundTag();
        tag.putInt("Damage", damage);
        when(key.getTag()).thenReturn(tag);
        var stack = ItemEmiStack.ofKey(key, 1);
        return stack.setRemainder(stack.copy());
    }

    private static MaterialNode root(EmiIngredient... inputs) {
        var output = ItemEmiStack.ofKey(new EmiItemKey(Items.STONE, null), 1);
        var recipe = mock(EmiRecipe.class);
        when(recipe.getInputs()).thenReturn(List.of(inputs));
        when(recipe.getOutputs()).thenReturn(List.of(output));
        var root = new MaterialNode(output);
        root.defineRecipe(recipe);
        return root;
    }
}
