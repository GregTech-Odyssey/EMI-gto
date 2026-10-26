package dev.emi.emi.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import dev.emi.emi.TestBootstrap;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;

@SuppressWarnings({ "unchecked", "rawtypes" })
class EmiInventorySnapshotCacheTest {
    @BeforeAll
    static void bootstrap() {
        TestBootstrap.init();
    }

    @Test
    void framesShareInventoryUntilNextTickWhileCraftActionsStillReadFreshData() {
        var cache = new EmiInventorySnapshotCache();
        var screen = mock(AbstractContainerScreen.class);
        var handler = mock(EmiRecipeHandler.class);
        var first = EmiPlayerInventory.empty();
        var second = EmiPlayerInventory.empty();
        when(handler.getInventory(screen)).thenReturn(first, second);

        for (int frame = 0; frame < 100; frame++) {
            assertSame(first, cache.get(handler, screen));
        }
        verify(handler, times(1)).getInventory(screen);
        assertSame(second, handler.getInventory(screen));
        assertSame(first, cache.get(handler, screen));
        cache.clear();
        assertSame(second, cache.get(handler, screen));
        verify(handler, times(3)).getInventory(screen);
    }

    @Test
    void handlersAndScreensNeverShareInventories() {
        var cache = new EmiInventorySnapshotCache();
        var screen = mock(AbstractContainerScreen.class);
        var otherScreen = mock(AbstractContainerScreen.class);
        var handler = mock(EmiRecipeHandler.class);
        var otherHandler = mock(EmiRecipeHandler.class);
        var first = EmiPlayerInventory.empty();
        var second = EmiPlayerInventory.empty();
        var third = EmiPlayerInventory.empty();
        when(handler.getInventory(screen)).thenReturn(first);
        when(otherHandler.getInventory(screen)).thenReturn(second);
        when(handler.getInventory(otherScreen)).thenReturn(third);

        assertSame(first, cache.get(handler, screen));
        assertSame(second, cache.get(otherHandler, screen));
        assertSame(third, cache.get(handler, otherScreen));
        assertSame(first, cache.get(handler, screen));
        verify(handler, times(2)).getInventory(screen);
    }
}
