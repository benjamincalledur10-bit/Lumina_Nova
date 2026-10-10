package io.github.benjamincalledur10.luminanova.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Sodium owns its render policies when installed. Its public menu integration remains available. */
public final class NovaMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        var loader = FabricLoader.getInstance();
        if (mixin.endsWith("BlockEntityVisibilityMixin")) {
            return io.github.benjamincalledur10.luminanova.compat.NovaRenderCompatibility.blockEntityOwner().isEmpty();
        }
        if (mixin.endsWith("FrustumMixin")) {
            return !loader.isModLoaded("sodium") && !loader.isModLoaded("vulkanmod");
        }
        boolean quality = mixin.endsWith("FluidQualityMixin") || mixin.endsWith("TerrainSamplerMixin") || mixin.endsWith("EntitySortMixin");
        return !quality || !FabricLoader.getInstance().isModLoaded("sodium");
    }
    @Override public void onLoad(String name) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
