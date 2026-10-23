package dev.emi.emi.api.stack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.stack.EmiItemKey;

class ComparisonTest {
    @Test
    void stringsCompareContentAndHashWithoutSerializingNbt() {
        var first = customStack(new String("search"), Comparison.COMPARE_STRING);
        var equal = customStack(new String("search"), Comparison.COMPARE_STRING);
        var different = customStack("different", Comparison.COMPARE_STRING);
        var strategy = new EmiStackList.StrictHashStrategy();

        assertTrue(first.isStrictEqual(equal));
        assertFalse(first.isStrictEqual(different));
        assertTrue(strategy.equals(first, equal));
        assertEquals(strategy.hashCode(first), strategy.hashCode(equal));
        assertEquals("search".hashCode(), Comparison.COMPARE_STRING.getHash(first));
        verify(first, never()).getNbt();
        verify(equal, never()).getNbt();
    }

    @Test
    void referenceComparisonKeepsEqualButDistinctRegisteredObjectsSeparate() {
        var key = new String("registered");
        var first = customStack(key, Comparison.COMPARE_REFERENCE);
        var equal = customStack(key, Comparison.COMPARE_REFERENCE);
        var distinct = customStack(new String("registered"), Comparison.COMPARE_REFERENCE);

        assertTrue(first.isStrictEqual(equal));
        assertFalse(first.isStrictEqual(distinct));
        assertEquals(System.identityHashCode(key), Comparison.COMPARE_REFERENCE.getHash(first));
        verify(first, never()).getNbt();
        verify(equal, never()).getNbt();
        verify(distinct, never()).getNbt();
    }

    @Test
    void standardStacksUseInternedIdentityWithoutReadingNbt() {
        var first = customStack("item", Comparison.COMPARE_STACK_KEY);
        var equal = customStack("item", Comparison.COMPARE_STACK_KEY);
        var different = customStack("item", Comparison.COMPARE_STACK_KEY);
        var key = mock(EmiItemKey.class);
        when(first.getStackKey()).thenReturn(key);
        when(equal.getStackKey()).thenReturn(key);
        when(different.getStackKey()).thenReturn(mock(EmiItemKey.class));

        assertTrue(first.isStrictEqual(equal));
        assertFalse(first.isStrictEqual(different));
        assertEquals(Comparison.COMPARE_STACK_KEY.getHash(first), Comparison.COMPARE_STACK_KEY.getHash(equal));
        verify(first, never()).getNbt();
        verify(equal, never()).getNbt();
        verify(different, never()).getNbt();
    }

    private static EmiStack customStack(Object key, Comparison comparison) {
        var stack = mock(EmiStack.class, CALLS_REAL_METHODS);
        when(stack.getKey()).thenReturn(key);
        when(stack.getStrictComparison()).thenReturn(comparison);
        doThrow(new AssertionError("Dedicated comparison must not serialize NBT")).when(stack).getNbt();
        return stack;
    }
}
