package dev.emi.emi.bom;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MaterialNode {
    public final EmiIngredient ingredient;
    public final EmiStack remainder;
    public @Nullable EmiRecipe recipe;
    public @Nullable List<MaterialNode> children;
    public float consumeChance = 1, produceChance = 1;
    public long amount = 1;
    public long divisor = 1;
    public long remainderAmount = 0;
    public boolean catalyst = false;
    // Should these be decoupled from material nodes?
    public FoldState state = FoldState.EXPANDED;
    public ProgressState progress = ProgressState.UNSTARTED;
    public long neededBatches = 0, totalNeeded = 0;
    private @Nullable ObjectArrayList<EmiStack> calculationOutputs;
    private @Nullable ObjectArrayList<EmiIngredient> calculationInputs;

    public MaterialNode(EmiIngredient ingredient) {
        this.amount = ingredient.getAmount();
        this.ingredient = ingredient.copy().setAmount(1).setChance(1);
        if (this.ingredient.getEmiStacks().size() == 1) {
            this.remainder = this.ingredient.getEmiStacks().getFirst().getRemainder();
            this.remainderAmount = remainder.getAmount();
            this.remainder.setAmount(1);
        } else {
            this.remainder = EmiStack.EMPTY;
        }
        out: {
            for (EmiStack stack : ingredient.getEmiStacks()) {
                if (!stack.getRemainder().equals(stack)) {
                    break out;
                }
            }
            catalyst = true;
        }
    }

    public MaterialNode(MaterialNode node) {
        this.ingredient = node.ingredient.copy();
        this.remainder = node.remainder.copy();
        this.recipe = node.recipe;
        this.amount = node.amount;
        this.divisor = node.divisor;
        this.remainderAmount = node.remainderAmount;
    }

    public MaterialNode copyForCalculation() {
        MaterialNode copy = new MaterialNode(this);
        copy.consumeChance = consumeChance;
        copy.produceChance = produceChance;
        copy.catalyst = catalyst;
        if (recipe != null) {
            var inputs = recipe.getInputs();
            copy.calculationInputs = new ObjectArrayList<>(inputs.size());
            for (var input : inputs) {
                copy.calculationInputs.add(input.copy());
            }
            var outputs = recipe.getOutputs();
            copy.calculationOutputs = new ObjectArrayList<>(outputs.size());
            for (var output : outputs) {
                copy.calculationOutputs.add(output.copy());
            }
        }
        if (children != null) {
            var copiedChildren = new ObjectArrayList<MaterialNode>(children.size());
            for (var child : children) {
                copiedChildren.add(child.copyForCalculation());
            }
            copy.children = copiedChildren;
        }
        return copy;
    }

    public List<EmiStack> getCalculationOutputs() {
        return calculationOutputs != null ? calculationOutputs : recipe.getOutputs();
    }

    public List<EmiIngredient> getCalculationInputs() {
        return calculationInputs != null ? calculationInputs : recipe.getInputs();
    }

    public void applyProgress(MaterialNode calculated) {
        progress = calculated.progress;
        neededBatches = calculated.neededBatches;
        totalNeeded = calculated.totalNeeded;
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                children.get(i).applyProgress(calculated.children.get(i));
            }
        }
    }

    public void recalculate(MaterialTree tree) {
        recalculate(tree, new ObjectArrayList<>());
    }

    private void recalculate(MaterialTree tree, List<EmiRecipe> used) {
        EmiRecipe recipe = this.recipe;
        if (!used.isEmpty()) {
            recipe = tree.getRecipe(ingredient);
        }
        if (recipe != null) {
            if (used.contains(recipe)) {
                return;
            }
            used.add(recipe);
            defineRecipe(recipe);
            for (MaterialNode node : children) {
                node.recalculate(tree, used);
            }
            used.removeLast();
        }
    }

    public void defineRecipe(EmiRecipe recipe) {
        produceChance = 1;
        if (recipe == null) {
            return;
        }
        this.recipe = recipe;
        divisor = 0;
        for (EmiStack stack : recipe.getOutputs()) {
            if (stack.equals(ingredient)) {
                if (divisor > 0) {
                    if (produceChance != 1 || stack.getChance() != 1) {
                        produceChance = (stack.getAmount() * stack.getChance() + divisor * produceChance) / (divisor + stack.getAmount());
                    }
                    divisor += stack.getAmount();
                } else {
                    divisor = stack.getAmount();
                    produceChance = stack.getChance();
                }
            }
        }
        if (divisor <= 0) {
            divisor = 1;
        }
        this.children = new ObjectArrayList<>();
        outer:
        for (EmiIngredient i : recipe.getInputs()) {
            EmiStack remainder = EmiStack.EMPTY;
            if (i.getEmiStacks().size() == 1) {
                remainder = i.getEmiStacks().getFirst().getRemainder();
            }
            for (MaterialNode node : children) {
                if (EmiIngredient.areEqual(i, node.ingredient) && EmiIngredient.areEqual(remainder, node.remainder)) {
                    node.amount += i.getAmount();
                    node.remainderAmount += remainder.getAmount();
                    continue outer;
                }
            }
            if (!i.isEmpty()) {
                MaterialNode node = new MaterialNode(i);
                node.consumeChance = i.getChance();
                children.add(node);
            }
        }
    }
}
