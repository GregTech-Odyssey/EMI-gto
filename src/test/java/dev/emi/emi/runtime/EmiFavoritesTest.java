package dev.emi.emi.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;

import dev.emi.emi.TestBootstrap;
import dev.emi.emi.api.EmiHooks;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.stack.EmiItemKey;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;

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

    private static EmiStack customStack(CompoundTag tag) {
        var stack = mock(EmiStack.class, CALLS_REAL_METHODS);
        when(stack.getKey()).thenReturn(tag == null ? "custom_resource" : new String(tag.getString("variant")));
        when(stack.getStrictComparison()).thenReturn(Comparison.COMPARE_STRING);
        doThrow(new AssertionError("String comparison must not serialize NBT")).when(stack).getNbt();
        return stack;
    }
}
