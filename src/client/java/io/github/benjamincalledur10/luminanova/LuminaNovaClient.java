package io.github.benjamincalledur10.luminanova;

import io.github.benjamincalledur10.luminanova.config.NovaConfig;
import io.github.benjamincalledur10.luminanova.config.NovaSettings;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuminaNovaClient implements ClientModInitializer {
    public static final String MOD_ID = "luminanova";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        NovaConfig config = NovaSettings.CONFIG;
        if (!config.enabled()) {
            LOGGER.info("Lumina Nova is disabled by configuration.");
            return;
        }
        LOGGER.info("Lumina Nova initialized. Experimental boolean frustum test: {}.", config.fastFrustum());
    }
}
