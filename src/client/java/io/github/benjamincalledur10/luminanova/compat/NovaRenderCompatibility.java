package io.github.benjamincalledur10.luminanova.compat;

import net.fabricmc.loader.api.FabricLoader;

/** Keep ownership of overlapping culling and alternate render passes with their renderer. */
public final class NovaRenderCompatibility {
    private NovaRenderCompatibility() { }
    public static String blockEntityOwner() {
        var loader = FabricLoader.getInstance();
        if (loader.isModLoaded("entityculling")) return "Entity Culling";
        if (loader.isModLoaded("moreculling")) return "More Culling";
        if (loader.isModLoaded("iris")) return "Iris";
        if (loader.isModLoaded("vulkanmod")) return "VulkanMod";
        return "";
    }
}
