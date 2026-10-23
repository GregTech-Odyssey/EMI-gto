package dev.emi.emi.api.stack;

import dev.emi.emi.runtime.EmiLog;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Function;

public class Comparison {
    public static final Comparison COMPARE_STACK_KEY = Comparison.of(
        (a, b) -> a.getStackKey() != null && a.getStackKey() == b.getStackKey(),
        stack -> stack.getStackKey().hashCode());
    public static final Comparison COMPARE_STRING = Comparison.of(
        (a, b) -> a.getKey() instanceof String text && text.equals(b.getKey()),
        stack -> ((String) stack.getKey()).hashCode());
    public static final Comparison COMPARE_REFERENCE = Comparison.of(
        (a, b) -> a.getKey() == b.getKey(),
        stack -> System.identityHashCode(stack.getKey()));
    @Deprecated
    private static final Comparison COMPARE_NBT = Comparison.of((a, b) -> {
        var ak = a.getStackKey();
        var bk = b.getStackKey();
        if (ak == null && bk == null) {
            return Objects.equals(a.getNbt(), b.getNbt());
        }
        return ak == bk;
    }, stack -> stack.getStackKey() != null ? stack.getStackKey().hashCode() : 0);
    public static final Comparison DEFAULT_COMPARISON = Comparison.of((a, b) -> true);
    private Predicate predicate;
    private HashFunction hash;

    private Comparison(Predicate comparator, HashFunction hash) {
        this.predicate = comparator;
        this.hash = hash;
    }

    public static Comparison of(Predicate comparator) {
        return of(comparator, i -> 0);
    }

    public static Comparison of(Predicate comparator, HashFunction hashFunction) {
        return new Comparison(comparator, hashFunction);
    }

    /**
     * Creates a comparison method based on data from a stack (String name, Potion, Integer id) Uses the data type's
     * equals and hashCode functions for comparison
     *
     * @param function A function that returns the comparable data from two stacks with equal keys
     */
    public static <T> Comparison compareData(Function<dev.emi.emi.api.stack.EmiStack, T> function) {
        return of((a, b) -> Objects.equals(function.apply(a), function.apply(b)), stack -> Objects.hashCode(function.apply(stack)));
    }

    /**
     * Creates a comparison method where stacks are distinct based on NBT.
     *
     * @deprecated Use the resource's dedicated {@link EmiStack#getStrictComparison()} instead.
     */
    @Deprecated
    public static Comparison compareNbt() {
        return COMPARE_NBT;
    }

    public boolean compare(EmiStack a, EmiStack b) {
        try {
            return predicate.test(a, b);
        } catch (Throwable t) {
            predicate = (na, nb) -> true;
            EmiLog.error("Comparison threw an exception, disabling", t);
        }
        return true;
    }

    @ApiStatus.Internal
    public int getHash(EmiStack stack) {
        try {
            return hash.hash(stack);
        } catch (Throwable t) {
            hash = s -> 0;
            EmiLog.error("Comparison threw an exception, disabling", t);
        }
        return 0;
    }

    public static interface Predicate {

        /**
         * @return Whether the two stacks should be treated as equivalent
         */
        public boolean test(EmiStack a, EmiStack b);
    }

    public static interface HashFunction {

        /**
         * @return The hash for the data being compared in the comparison
         */
        public int hash(EmiStack stack);
    }

    static class Builder {
    }
}
