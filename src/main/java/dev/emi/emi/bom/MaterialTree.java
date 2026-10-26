package dev.emi.emi.bom;

import com.gto.fastcollection.fastutil.O2OOpenCacheHashMap;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.bom.MaterialNode;
import dev.emi.emi.bom.TreeCost;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class MaterialTree {
    public MaterialNode goal;
    public TreeCost cost = new TreeCost();
    public O2OOpenCacheHashMap<EmiIngredient, EmiRecipe> resolutions = new O2OOpenCacheHashMap<>();
    public long batches = 1;
    private long revision;
    private final Reference2ObjectOpenHashMap<EmiRecipe, MaterialNode> calculationRecipes = new Reference2ObjectOpenHashMap<>();

    private MaterialTree(MaterialNode goal, long batches) {
        this.goal = goal;
        this.batches = batches;
        indexCalculationRecipes(goal);
    }

    private void indexCalculationRecipes(MaterialNode node) {
        if (node.recipe != null) {
            calculationRecipes.put(node.recipe, node);
        }
        if (node.children != null) {
            for (var child : node.children) {
                indexCalculationRecipes(child);
            }
        }
    }

    public MaterialNode getCalculationRecipe(EmiRecipe recipe) {
        return calculationRecipes.get(recipe);
    }

    public long getRevision() {
        return revision;
    }

    public MaterialTree copyForCalculation() {
        return new MaterialTree(goal.copyForCalculation(), batches);
    }

    public MaterialTree(EmiRecipe recipe) {
        EmiStack output = recipe.getOutputs().getFirst();
        goal = new MaterialNode(output);
        goal.defineRecipe(recipe);
        recalculate();
    }

    public EmiRecipe getRecipe(EmiIngredient stack) {
        EmiRecipe recipe = resolutions.get(stack);
        if (recipe == null && !resolutions.containsKey(stack)) {
            recipe = BoM.getRecipe(stack);
        }
        return recipe;
    }

    public void addResolution(EmiIngredient ingredient, EmiRecipe recipe) {
        resolutions.put(ingredient, recipe);
        if (ingredient.equals(goal.ingredient)) {
            goal.defineRecipe(recipe);
            goal.amount = ingredient.getAmount();
        }
        recalculate();
    }

    public void recalculate() {
        revision++;
        goal.recalculate(this);
    }

    public void calculateProgress(EmiPlayerInventory inventory) {
        cost.calculateProgress(goal, batches, inventory);
    }

    public void calculateCost() {
        cost.calculate(goal, batches);
    }
}
