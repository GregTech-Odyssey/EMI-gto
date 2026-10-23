package dev.emi.emi.stack;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraftforge.fml.loading.LoadingModList;

/**
 * Skips EMI's own key storage on items and fluids when Applied Energistics is loaded and its keys are used instead.
 */
@ApiStatus.Internal
public final class StackKeyMixinPlugin implements IMixinConfigPlugin {
    private static final String KEY_MIXINS = "dev.emi.emi.mixin.key.";

    public static boolean useAeKeys() {
        var mods = LoadingModList.get();
        if (mods != null) {
            return mods.getModFileById("ae2") != null;
        }
        return StackKeyMixinPlugin.class.getClassLoader().getResource("appeng/api/stacks/AEKey.class") != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !mixinClassName.startsWith(KEY_MIXINS) || !useAeKeys();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
