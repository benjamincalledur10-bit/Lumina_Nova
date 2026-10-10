package io.github.benjamincalledur10.luminanova.config;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

public final class NovaPerformanceSettings {
    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("luminanova-performance.properties");
    private static volatile NovaPerformanceConfig current = NovaPerformanceConfig.load(PATH, LoggerFactory.getLogger("luminanova"));
    private NovaPerformanceSettings() { }
    public static NovaPerformanceConfig current() { return current; }
    public static void apply(NovaPerformanceConfig requested) { current = requested; }
}
