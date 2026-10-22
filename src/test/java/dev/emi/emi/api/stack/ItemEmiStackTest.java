package dev.emi.emi.api.stack;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;

class ItemEmiStackTest {
    @Test
    void largeAmountsDoNotOverflowTheItemStackRenderingBridge() {
        var key = mock(EmiItemKey.class);
        var rendered = mock(ItemStack.class);
        when(key.toStack(Integer.MAX_VALUE)).thenReturn(rendered);
        assertSame(rendered, new ItemEmiStack(key, 3_000_000_000L).getItemStack());
        assertSame(rendered, new ItemEmiStack(key, Long.MAX_VALUE).getItemStack());
    }
}
