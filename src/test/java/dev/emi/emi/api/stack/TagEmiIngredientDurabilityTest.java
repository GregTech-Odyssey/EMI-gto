package dev.emi.emi.api.stack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.emi.emi.TestBootstrap;
import dev.emi.emi.api.recipe.EmiDurabilityTool;
import dev.emi.emi.runtime.EmiTagKey;

class TagEmiIngredientDurabilityTest {
    @BeforeAll
    static void bootstrap() {
        TestBootstrap.init();
    }

    @Test
    void toolTagUsesTheSelectedToolsDurabilityAndCostInsteadOfTheFirstAlternative() {
        var key = TagKey.create(Registries.ITEM, new ResourceLocation("gtceu", "tools/crafting_hammers"));
        var alternatives = List.of(mock(EmiStack.class), mock(EmiStack.class));
        var tag = ingredient(key, alternatives);
        var first = tool(key, 120, 2);
        var second = tool(key, 35, 4);

        assertTrue(tag.emi$isDurabilityTool(first));
        assertEquals(120, tag.emi$getRemainingDurability(first));
        assertEquals(2, tag.emi$getCraftingDurabilityCost(first));
        assertTrue(tag.emi$isDurabilityTool(second));
        assertEquals(35, tag.emi$getRemainingDurability(second));
        assertEquals(4, tag.emi$getCraftingDurabilityCost(second));
        assertSame(alternatives, tag.getEmiStacks());

        var provider = (EmiDurabilityTool) second.getItem();
        when(provider.emi$getRemainingDurability(second)).thenReturn(1);
        assertEquals(1, tag.emi$getRemainingDurability(second));
        assertEquals(120, tag.emi$getRemainingDurability(first));
    }

    @Test
    void nonmembersAndNonDurabilityItemsAreNotToolIngredients() {
        var key = TagKey.create(Registries.ITEM, new ResourceLocation("gtceu", "tools/screwdrivers"));
        var tag = ingredient(key, List.of(mock(EmiStack.class)));
        var stack = tool(key, 80, 4);
        var provider = (EmiDurabilityTool) stack.getItem();
        when(stack.is(key)).thenReturn(false);
        assertFalse(tag.emi$isDurabilityTool(stack));
        verify(provider, never()).emi$isDurabilityTool(stack);

        when(stack.is(key)).thenReturn(true);
        when(provider.emi$isDurabilityTool(stack)).thenReturn(false);
        assertFalse(tag.emi$isDurabilityTool(stack));
    }

    @Test
    void fluidTagsCannotTreatAnItemWithTheSameTagNameAsATool() {
        var itemKey = TagKey.create(Registries.ITEM, new ResourceLocation("gtceu", "tools/crafting_hammers"));
        var fluidKey = TagKey.create(Registries.FLUID, itemKey.location());
        var tag = ingredient(fluidKey, List.of(mock(EmiStack.class)));
        var stack = tool(itemKey, 120, 2);

        assertFalse(tag.emi$isDurabilityTool(stack));
        verify(stack, never()).getItem();
    }

    private static TagEmiIngredient ingredient(TagKey<?> raw, List<EmiStack> alternatives) {
        var wrapped = mock(EmiTagKey.class);
        when(wrapped.id()).thenReturn(raw.location());
        when(wrapped.raw()).thenReturn(raw);
        try (var keys = mockStatic(EmiTagKey.class)) {
            keys.when(() -> EmiTagKey.of(raw)).thenReturn(wrapped);
            return new TagEmiIngredient(raw, alternatives, 1);
        }
    }

    private static ItemStack tool(TagKey<Item> key, int remaining, int cost) {
        var item = mock(Item.class, withSettings().extraInterfaces(EmiDurabilityTool.class));
        var provider = (EmiDurabilityTool) item;
        var stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        when(stack.is(key)).thenReturn(true);
        when(provider.emi$isDurabilityTool(stack)).thenReturn(true);
        when(provider.emi$getRemainingDurability(stack)).thenReturn(remaining);
        when(provider.emi$getCraftingDurabilityCost(stack)).thenReturn(cost);
        return stack;
    }
}
