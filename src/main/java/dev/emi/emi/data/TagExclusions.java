package dev.emi.emi.data;

import com.gto.fastcollection.fastutil.O2OOpenCacheHashMap;
import com.gto.fastcollection.fastutil.OpenCacheHashSet;

import net.minecraft.resources.ResourceLocation;

public class TagExclusions {
    public final OpenCacheHashSet<ResourceLocation> globalExclusions = new OpenCacheHashSet<>();
    public final O2OOpenCacheHashMap<ResourceLocation, OpenCacheHashSet<ResourceLocation>> exclusions = new O2OOpenCacheHashMap<>();

    public void add(ResourceLocation id) {
        globalExclusions.add(id);
    }

    public void add(ResourceLocation type, ResourceLocation id) {
        exclusions.computeIfAbsent(type, t -> new OpenCacheHashSet<>()).add(id);
    }

    public void clear() {
        globalExclusions.clear();
        exclusions.clear();
    }

    public boolean contains(ResourceLocation type, ResourceLocation id) {
        if (globalExclusions.contains(id)) {
            return true;
        }
        var excluded = exclusions.get(type);
        return excluded != null && excluded.contains(id);
    }
}
