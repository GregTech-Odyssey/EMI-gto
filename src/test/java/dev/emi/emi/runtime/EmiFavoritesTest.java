package dev.emi.emi.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import it.unimi.dsi.fastutil.objects.Reference2LongLinkedOpenHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;

import dev.emi.emi.TestBootstrap;
import dev.emi.emi.api.EmiHooks;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.stack.EmiItemKey;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.bom.MaterialNode;

class EmiFavoritesTest {
    @BeforeAll
    static void bootstrap() {
        TestBootstrap.init();
    }

    @AfterEach
    void clearFavorites() {
        EmiFavorites.favorites.clear();
    }

    @Test
    void favoriteShortcutRemovesAnExistingItemFavoriteAndPersistsTheChange() {
        var favorite = new EmiFavorite(ItemEmiStack.ofKey(new EmiItemKey(Items.STONE, null), 1), null);
        EmiFavorites.favorites.add(favorite);

        try (var data = mockStatic(EmiPersistentData.class); var hooks = mockStatic(EmiHooks.class)) {
            EmiFavorites.addFavorite(favorite);

            assertTrue(EmiFavorites.favorites.isEmpty());
            data.verify(EmiPersistentData::save);
            hooks.verify(EmiHooks::onFavoritesChanged, atLeastOnce());
        }
    }

    @Test
    void favoriteShortcutRemovesAnExistingCustomResourceFavorite() {
        var favorite = new EmiFavorite(customStack(null), null);
        EmiFavorites.favorites.add(favorite);

        try (var data = mockStatic(EmiPersistentData.class)) {
            EmiFavorites.addFavorite(favorite);

            assertTrue(EmiFavorites.favorites.isEmpty());
            data.verify(EmiPersistentData::save);
        }
    }

    @Test
    void copiedRecipeFavoriteRemovesOnlyTheMatchingRecipeEntry() {
        var recipe = mock(EmiRecipe.class);
        var ingredient = customStack(null);
        var itemFavorite = new EmiFavorite(ingredient, null);
        var recipeFavorite = new EmiFavorite(ingredient, recipe);
        EmiFavorites.favorites.add(itemFavorite);
        EmiFavorites.favorites.add(recipeFavorite);

        try (var data = mockStatic(EmiPersistentData.class)) {
            EmiFavorites.addFavorite(recipeFavorite.copy());

            assertEquals(1, EmiFavorites.favorites.size());
            assertSame(itemFavorite, EmiFavorites.favorites.getFirst());
        }
    }

    @Test
    void customStringResourceFavoritesMatchByContentWithoutReadingNbt() {
        var firstTag = new CompoundTag();
        firstTag.putString("variant", "first");
        var secondTag = new CompoundTag();
        secondTag.putString("variant", "second");
        var first = new EmiFavorite(customStack(firstTag), null);
        var matchingStack = customStack(secondTag.copy());
        var second = new EmiFavorite(customStack(secondTag), null);
        EmiFavorites.favorites.add(first);
        EmiFavorites.favorites.add(second);

        assertTrue(EmiFavorites.removeFavorite(new EmiFavorite(matchingStack, null)));
        assertEquals(1, EmiFavorites.favorites.size());
        assertSame(first, EmiFavorites.favorites.getFirst());
        verify(matchingStack, never()).getNbt();
    }

    @Test
    void recipeCountsMergeRepeatedInstancesInLastVisitOrder() {
        var firstRecipe = mock(EmiRecipe.class);
        var secondRecipe = mock(EmiRecipe.class);
        var key = new EmiItemKey(Items.STONE, null);
        var root = new MaterialNode(ItemEmiStack.ofKey(key, 1));
        var second = new MaterialNode(ItemEmiStack.ofKey(key, 1));
        var repeated = new MaterialNode(ItemEmiStack.ofKey(key, 1));
        root.recipe = firstRecipe;
        root.neededBatches = 2;
        root.totalNeeded = 5;
        second.recipe = secondRecipe;
        second.neededBatches = 3;
        second.totalNeeded = 7;
        repeated.recipe = firstRecipe;
        repeated.neededBatches = 4;
        repeated.totalNeeded = 11;
        root.children = List.of(second, repeated);
        second.children = List.of();
        repeated.children = List.of();
        var batches = new Reference2LongLinkedOpenHashMap<EmiRecipe>();
        var amounts = new Reference2LongLinkedOpenHashMap<EmiRecipe>();

        EmiFavorites.countRecipes(batches, amounts, root);

        assertEquals(List.of(secondRecipe, firstRecipe), List.copyOf(batches.keySet()));
        assertEquals(List.of(secondRecipe, firstRecipe), List.copyOf(amounts.keySet()));
        assertEquals(6, batches.getLong(firstRecipe));
        assertEquals(16, amounts.getLong(firstRecipe));
        assertEquals(3, batches.getLong(secondRecipe));
        assertEquals(7, amounts.getLong(secondRecipe));
    }

    private static EmiStack customStack(CompoundTag tag) {
        var stack = mock(EmiStack.class, CALLS_REAL_METHODS);
        when(stack.getKey()).thenReturn(tag == null ? "custom_resource" : new String(tag.getString("variant")));
        when(stack.getStrictComparison()).thenReturn(Comparison.COMPARE_STRING);
        doThrow(new AssertionError("String comparison must not serialize NBT")).when(stack).getNbt();
        return stack;
    }
}
