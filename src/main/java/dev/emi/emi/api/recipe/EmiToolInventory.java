package dev.emi.emi.api.recipe;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;

/** Snapshot of crafting uses, independent of the count-only inventory and of live item callbacks. */
@ApiStatus.Internal
public final class EmiToolInventory {
    private final Reference2ObjectOpenHashMap<Item, ObjectArrayList<Entry>> byItem = new Reference2ObjectOpenHashMap<>();
    private final ObjectArrayList<Entry> entries = new ObjectArrayList<>();
    private long reservation;

    public static long uses(EmiStack stack) {
        if (!(stack instanceof ItemEmiStack item)) return 0;
        var raw = item.getReadOnlyStack();
        if (!(raw.getItem() instanceof EmiDurabilityTool tool)) return 1;
        if (!tool.emi$isDurabilityTool(raw)) return raw.getMaxDamage() > 0 ? Long.MAX_VALUE : 1;
        return tool.emi$getRemainingDurability(raw) / tool.emi$getCraftingDurabilityCost(raw);
    }

    /** A consumed armor piece is not a reusable crafting tool, even though it has durability. */
    public static boolean isToolIngredient(EmiIngredient ingredient) {
        for (var stack : ingredient.getEmiStacks()) {
            if (isReusableTool(stack)) return true;
        }
        return false;
    }

    public static boolean isReusableTool(EmiStack stack) {
        if (!(stack instanceof ItemEmiStack item) || stack.getRemainder().isEmpty()
                || stack.getRemainder().getKey() != stack.getKey()) return false;
        var raw = item.getReadOnlyStack();
        return raw.getItem() instanceof EmiDurabilityTool tool
            && (tool.emi$isDurabilityTool(raw) || raw.getMaxDamage() > 0);
    }

    public static long multiply(long a, long b) {
        return b > 0 && a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }

    public static long ceilDiv(long amount, long divisor) {
        return amount / divisor + (amount % divisor == 0 ? 0 : 1);
    }

    public void capture(EmiStack stack) {
        long uses = uses(stack);
        if (uses > 0) add(stack.copy().setAmount(1), stack.getAmount(), multiply(uses, stack.getAmount()));
    }

    public void add(EmiStack stack, long count, long uses) {
        var entry = new Entry(stack, count, uses);
        entries.add(entry);
        var item = (Item) stack.getKey();
        var group = byItem.get(item);
        if (group == null) {
            group = new ObjectArrayList<>();
            byItem.put(item, group);
        }
        group.add(entry);
    }

    public void copyFrom(EmiToolInventory source) {
        clear();
        for (var entry : source.entries) add(entry.stack, entry.count, entry.totalUses);
    }

    public void clear() {
        entries.clear();
        byItem.clear();
    }

    public void reset() {
        for (var entry : entries) {
            entry.remaining = entry.totalUses;
            entry.slots = 0;
        }
    }

    /** Each physical tool can occupy only one input slot in a batch. */
    public long reserve(EmiIngredient ingredient, long desired, long batches, boolean reserveSlots) {
        long currentReservation = ++reservation;
        long given = 0;
        for (var candidate : ingredient.getEmiStacks()) {
            var group = byItem.get(candidate.getKey());
            if (group == null) continue;
            for (var entry : group) {
                if (entry.reservation == currentReservation) continue;
                entry.reservation = currentReservation;
                long count = entry.count - (reserveSlots ? entry.slots : 0);
                long taken = Math.min(desired - given, Math.min(entry.remaining, multiply(count, batches)));
                if (taken <= 0) continue;
                if (entry.remaining != Long.MAX_VALUE) entry.remaining -= taken;
                if (reserveSlots) entry.slots += ceilDiv(taken, batches);
                given += taken;
                if (given == desired) return given;
            }
        }
        return given;
    }

    /** A tool already occupying a recipe slot cannot also be consumed by another slot. */
    public boolean reserveItems(EmiStack stack, long desired) {
        var group = byItem.get(stack.getKey());
        if (group == null) return true;
        long available = 0;
        for (var entry : group) available += entry.count - entry.slots;
        if (available < desired) return false;
        for (var entry : group) {
            long taken = Math.min(desired, entry.count - entry.slots);
            entry.slots += taken;
            long uses = multiply(taken, entry.totalUses / entry.count);
            entry.remaining -= Math.min(entry.remaining, uses);
            desired -= taken;
            if (desired == 0) return true;
        }
        return true;
    }

    public boolean isEqual(EmiToolInventory other) {
        if (entries.size() != other.entries.size()) return false;
        for (int i = 0; i < entries.size(); i++) {
            var a = entries.get(i);
            var b = other.entries.get(i);
            if (a.count != b.count || a.totalUses != b.totalUses || !a.stack.isStrictEqual(b.stack)) return false;
        }
        return true;
    }

    private static final class Entry {
        final EmiStack stack;
        final long count;
        final long totalUses;
        long remaining;
        long slots;
        long reservation;

        Entry(EmiStack stack, long count, long uses) {
            this.stack = stack;
            this.count = count;
            this.totalUses = uses;
            this.remaining = uses;
        }
    }
}
