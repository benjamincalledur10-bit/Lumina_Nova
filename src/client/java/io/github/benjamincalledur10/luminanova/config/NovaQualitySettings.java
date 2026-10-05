package io.github.benjamincalledur10.luminanova.config;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

/** Immutable snapshot shared with chunk workers; updates happen on the client thread. */
public final class NovaQualitySettings {
    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("luminanova-quality.properties");
    private static volatile NovaQualityConfig current = NovaQualityConfig.load(PATH, LoggerFactory.getLogger("luminanova"));

    private NovaQualitySettings() { }
    public static NovaQualityConfig current() { return current; }

    public static void apply(NovaQualityConfig requested) {
        NovaQualityConfig previous = current;
        current = requested;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelExtractor == null) return;
        if (previous.linearTexels() != requested.linearTexels()) minecraft.levelExtractor.resetSampler();
        if (previous.fluidCulling() != requested.fluidCulling()
                || previous.alternativeFluids() != requested.alternativeFluids()) minecraft.levelExtractor.allChanged();
    }
}
