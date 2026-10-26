package dev.emi.emi.runtime;

import java.util.AbstractList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.GsonHelper;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiHooks;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiResolutionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.bom.ChanceMaterialCost;
import dev.emi.emi.bom.FlatMaterialCost;
import dev.emi.emi.bom.MaterialNode;
import dev.emi.emi.bom.MaterialTree;
import dev.emi.emi.bom.TreeCost;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2LongLinkedOpenHashMap;

public class EmiFavorites {
    public static List<EmiFavorite> favorites = new ObjectArrayList<>();
    public static List<EmiFavorite.Synthetic> syntheticFavorites = new ObjectArrayList<>();
    public static List<EmiFavorite> favoriteSidebar = new CompoundList<>(favorites, syntheticFavorites);

    public static JsonArray save() {
        JsonArray arr = new JsonArray();
        for (EmiFavorite fav : favorites) {
            JsonElement stack = EmiIngredientSerializer.getSerialized(fav.getStack());
            if (stack != null) {
                JsonObject obj = new JsonObject();
                obj.add("stack", stack);
                if (fav.getRecipe() != null && fav.getRecipe().getId() != null) {
                    obj.addProperty("recipe", fav.getRecipe().getId().toString());
                }
                arr.add(obj);
            }
        }
        return arr;
    }

    public static void load(JsonArray arr) {
        favorites.clear();
        for (JsonElement el : arr) {
            if (el.isJsonObject()) {
                JsonObject json = el.getAsJsonObject();
                EmiRecipe recipe = null;
                if (GsonHelper.isStringValue(json, "recipe")) {
                    recipe = EmiApi.getRecipeManager().getRecipe(EmiPort.id(GsonHelper.getAsString(json, "recipe")));
                }
                if (GsonHelper.isValidNode(json, "stack")) {
                    EmiIngredient ingredient = EmiIngredientSerializer.getDeserialized(json.get("stack"));
                    if (ingredient.isEmpty()) {
                        continue;
                    }
                    if (ingredient instanceof EmiStack es) {
                        ingredient = es.copy();
                    }
                    favorites.add(new EmiFavorite(ingredient, recipe));
                }
            }
        }
        EmiHooks.onFavoritesChanged();
    }

    public static boolean canFavorite(EmiIngredient stack, EmiRecipe recipe) {
        stack = EmiIngredientSerializer.getDeserialized(EmiIngredientSerializer.getSerialized(stack));
        if (stack.isEmpty()) {
            return false;
        }
        if (recipe != null) {
            return recipe.getId() != null;
        }
        return true;
    }

    private static int indexOf(EmiIngredient stack) {
        EmiRecipe context = EmiApi.getRecipeContext(stack);
        for (int i = 0; i < favorites.size(); i++) {
            EmiFavorite favorite = favorites.get(i);
            if (favorite == stack || favorite.getRecipe() == context && favorite.strictEquals(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean removeFavorite(EmiIngredient stack) {
        int index = indexOf(stack);
        if (index != -1) {
            favorites.remove(index);
            EmiHooks.onFavoritesChanged();
            return true;
        }
        return false;
    }

    public static void addFavorite(EmiIngredient stack) {
        addFavorite(stack, null);
    }

    public static void addFavoriteAt(EmiIngredient stack, int offset) {
        if (stack instanceof EmiFavorite.Synthetic) {
            return;
        }
        if (stack instanceof EmiFavorite.Craftable craftable) {
            stack = craftable.stack;
        }
        EmiFavorite favorite;
        if (stack instanceof EmiFavorite fav) {
            int original = indexOf(stack);
            if (original != -1) {
                if (original < offset) {
                    offset--;
                }
                favorites.remove(original);
            }
            favorite = fav;
        } else {
            stack = EmiIngredientSerializer.getDeserialized(EmiIngredientSerializer.getSerialized(stack));
            if (stack.isEmpty()) {
                return;
            }
            for (int i = 0; i < favorites.size(); i++) {
                EmiFavorite fav = favorites.get(i);
                if (fav.getRecipe() == null && fav.strictEquals(stack)) {
                    favorites.remove(i--);
                }
            }
            favorite = new EmiFavorite(stack, null);
        }
        if (offset < 0) {
            offset = 0;
        }
        if (offset >= favorites.size()) {
            favorites.add(favorite);
        } else {
            favorites.add(offset, favorite);
        }
        EmiHooks.onFavoritesChanged();
        EmiPersistentData.save();
    }

    public static void addFavorite(EmiIngredient stack, EmiRecipe context) {
        if (stack instanceof EmiFavorite.Synthetic) {
            return;
        }
        if (stack instanceof EmiFavorite.Craftable craftable) {
            stack = craftable.stack;
        }
        if (stack instanceof EmiFavorite f) {
            if (!removeFavorite(f)) {
                favorites.add(f);
            }
        } else {
            stack = EmiIngredientSerializer.getDeserialized(EmiIngredientSerializer.getSerialized(stack));
            if (stack instanceof EmiStack es && context != null && context.getId() != null) {
                es = es.copy();
                if (es instanceof ItemEmiStack ies) {
                    ies.getItemStack().setCount(1);
                }
                if (!es.isEmpty()) {
                    for (int i = 0; i < favorites.size(); i++) {
                        EmiFavorite fav = favorites.get(i);
                        if (fav.getRecipe() == context && fav.strictEquals(es)) {
                            return;
                        }
                    }
                    favorites.add(new EmiFavorite(es, context));
                }
            } else {
                if (stack.isEmpty()) {
                    return;
                }
                for (int i = 0; i < favorites.size(); i++) {
                    EmiFavorite fav = favorites.get(i);
                    if (fav.getRecipe() == null && fav.strictEquals(stack)) {
                        return;
                    }
                }
                favorites.add(new EmiFavorite(stack, null));
            }
        }
        EmiHooks.onFavoritesChanged();
        EmiPersistentData.save();
    }

    public static void updateSynthetic(EmiPlayerInventory inv) {
        EmiTreeProgress.update(inv);
    }

    static SyntheticResult calculateSynthetic(MaterialTree tree, EmiPlayerInventory inv, TreeCost total) {
        var result = new ObjectArrayList<EmiFavorite.Synthetic>();
        var originalCosts = total.costs;
        var chancedCosts = total.chanceCosts;
        Reference2LongLinkedOpenHashMap<EmiRecipe> originalBatches = new Reference2LongLinkedOpenHashMap<>();
        Reference2LongLinkedOpenHashMap<EmiRecipe> originalAmounts = new Reference2LongLinkedOpenHashMap<>();
        EmiPlayerInventory emptyInventory = EmiPlayerInventory.empty();
        tree.calculateProgress(emptyInventory);
        countRecipes(originalBatches, originalAmounts, tree.goal);
        tree.calculateProgress(inv);
        Reference2LongLinkedOpenHashMap<EmiRecipe> batches = new Reference2LongLinkedOpenHashMap<>();
        Reference2LongLinkedOpenHashMap<EmiRecipe> amounts = new Reference2LongLinkedOpenHashMap<>();
        countRecipes(batches, amounts, tree.goal);
        boolean hasSomething = false;
        for (var entries = batches.reference2LongEntrySet().fastIterator(); entries.hasNext();) {
            var entry = entries.next();
            EmiRecipe recipe = entry.getKey();
            long amount = amounts.getLong(recipe);
            long batch = entry.getLongValue();
            if (amount == 0) {
                continue;
            }
            hasSomething = true;
            int state = 0;
            var calculationRecipe = tree.getCalculationRecipe(recipe);
            if (inv.canCraftInputs(calculationRecipe.getCalculationInputs(), batch)) {
                state = 2;
            } else if (inv.canCraftInputs(calculationRecipe.getCalculationInputs(), 1)) {
                state = 1;
            }
            result.add(new EmiFavorite.Synthetic(calculationRecipe.getCalculationOutputs().getFirst(), recipe, batch,
                    amount,
                    originalAmounts.getOrDefault(recipe, amount), state));
        }
        if (!hasSomething) {
            return new SyntheticResult(result, true);
        } else {
            for (FlatMaterialCost cost : tree.cost.costs.values()) {
                if (cost.amount > 0) {
                    result.add(new EmiFavorite.Synthetic(cost.ingredient, cost.amount,
                            originalCosts.getOrDefault(cost.ingredient, cost).amount));
                }
            }
            for (ChanceMaterialCost cost : tree.cost.chanceCosts.values()) {
                if (cost.getEffectiveAmount() > 0) {
                    long needed = cost.getEffectiveAmount();
                    if (chancedCosts.containsKey(cost.ingredient)) {
                        ChanceMaterialCost original = chancedCosts.get(cost.ingredient);
                        long done = (long) Math.ceil(original.amount * original.chance - cost.amount * cost.chance);
                        needed = original.getEffectiveAmount() - done;
                    }
                    if (needed > 0) {
                        result.add(new EmiFavorite.Synthetic(cost.ingredient, needed, needed));
                    }
                }
            }
        }
        return new SyntheticResult(result, false);
    }

    record SyntheticResult(ObjectArrayList<EmiFavorite.Synthetic> favorites, boolean complete) {}

    public static void countRecipes(Reference2LongLinkedOpenHashMap<EmiRecipe> batches,
            Reference2LongLinkedOpenHashMap<EmiRecipe> amounts,
            MaterialNode node) {
        if (node.recipe instanceof EmiResolutionRecipe recipe) {
            countRecipes(batches, amounts, node.children.get(0));
            return;
        }
        // Include empty costs for proper sorting
        if (node.recipe != null) {
            batches.putAndMoveToLast(node.recipe, batches.getLong(node.recipe) + node.neededBatches);
            amounts.putAndMoveToLast(node.recipe, amounts.getLong(node.recipe) + node.totalNeeded);
            for (MaterialNode child : node.children) {
                countRecipes(batches, amounts, child);
            }
        }
    }

    private static class CompoundList<T> extends AbstractList<T> {
        private List<? extends T> a, b;

        public CompoundList(List<? extends T> a, List<? extends T> b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public T get(int index) {
            if (index >= a.size()) {
                return b.get(index - a.size());
            }
            return a.get(index);
        }

        @Override
        public int size() {
            return a.size() + b.size();
        }
    }
}
