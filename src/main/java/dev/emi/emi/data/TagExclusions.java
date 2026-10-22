package dev.emi.emi.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

public class TagExclusions {
    public final Set<ResourceLocation> globalExclusions = new ObjectOpenHashSet<>();
    public final Map<ResourceLocation, Set<ResourceLocation>> exclusions = new Object2ObjectOpenHashMap<>();

    public void add(ResourceLocation id) {
        globalExclusions.add(id);
    }

    public void add(ResourceLocation type, ResourceLocation id) {
        exclusions.computeIfAbsent(type, t -> new ObjectOpenHashSet<>()).add(id);
    }

    public void clear() {
        globalExclusions.clear();
        exclusions.clear();
    }

    public boolean contains(ResourceLocation type, ResourceLocation id) {
        return globalExclusions.contains(id) || (exclusions.containsKey(type) && exclusions.get(type).contains(id));
    }
}
