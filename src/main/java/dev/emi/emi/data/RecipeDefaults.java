package dev.emi.emi.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.gto.fastcollection.fastutil.O2OOpenCacheHashMap;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.recipe.EmiResolutionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class RecipeDefaults {
    public final List<ResourceLocation> added = new ObjectArrayList<>();
    public final List<Resolution> resolutions = new ObjectArrayList<>();
    public final List<Tag> tags = new ObjectArrayList<>();

    public void add(ResourceLocation id) {
        added.add(id);
    }

    public void add(ResourceLocation id, JsonArray arr) {
        resolutions.add(new Resolution(id, arr.asList()));
    }

    public void addTag(JsonElement tag, JsonElement stack) {
        tags.add(new Tag(tag, stack));
    }

    public void remove(ResourceLocation id) {
        added.remove(id);
        resolutions.removeIf(r -> r.recipe.equals(id));
    }

    public void clear() {
        added.clear();
        resolutions.clear();
        tags.clear();
    }

    public O2OOpenCacheHashMap<EmiIngredient, EmiRecipe> bake() {
        var map = new O2OOpenCacheHashMap<EmiIngredient, EmiRecipe>(added.size() + resolutions.size() + tags.size());
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return map;
        }
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        for (ResourceLocation id : added) {
            EmiRecipe recipe = manager.getRecipe(id);
            if (recipe != null) {
                for (EmiIngredient stack : recipe.getOutputs()) {
                    map.put(stack, recipe);
                }
            }
        }
        for (Resolution r : resolutions) {
            EmiRecipe recipe = manager.getRecipe(r.recipe);
            if (recipe != null) {
                for (JsonElement el : r.stacks) {
                    EmiIngredient stack = EmiIngredientSerializer.getDeserialized(el);
                    if (!stack.isEmpty()) {
                        map.put(stack, recipe);
                    }
                }
            }
        }
        for (Tag t : tags) {
            EmiIngredient tag = EmiIngredientSerializer.getDeserialized(t.tag);
            EmiIngredient stack = EmiIngredientSerializer.getDeserialized(t.stack);
            if (!tag.isEmpty() && !stack.isEmpty() && stack.getEmiStacks().size() == 1 && tag.getEmiStacks().containsAll(stack.getEmiStacks())) {
                map.put(tag, new EmiResolutionRecipe(tag, stack.getEmiStacks().get(0)));
            }
        }
        return map;
    }

    public static record Resolution(ResourceLocation recipe, List<JsonElement> stacks) {
    }

    public static record Tag(JsonElement tag, JsonElement stack) {
    }
}
