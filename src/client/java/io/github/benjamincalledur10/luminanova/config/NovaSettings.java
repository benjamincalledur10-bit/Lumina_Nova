package io.github.benjamincalledur10.luminanova.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

/** Immutable startup settings shared by the entrypoint and rendering hook. */
public final class NovaSettings {
    public static final NovaConfig CONFIG = NovaConfig.load(
            FabricLoader.getInstance().getConfigDir().resolve("luminanova.properties"),
            LoggerFactory.getLogger("luminanova"));
    public static final boolean FAST_FRUSTUM_ENABLED = CONFIG.enabled() && CONFIG.fastFrustum();

    private NovaSettings() {}
}
