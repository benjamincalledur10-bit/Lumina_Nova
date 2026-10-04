package io.github.benjamincalledur10.luminanova.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import org.slf4j.Logger;

/** Startup-only configuration. Never replaces an existing user configuration. */
public record NovaConfig(boolean enabled, boolean fastFrustum) {
    public static NovaConfig load(Path path, Logger logger) {
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, "# Lumina Nova. Restart Minecraft after editing.\nenabled=true\n"
                                + "# Experimental boolean frustum test; opt in until in-game validation.\nfast_frustum=false\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            String enabled = properties.getProperty("enabled", "true").trim();
            String fastFrustum = properties.getProperty("fast_frustum", "false").trim();
            if (!isBoolean(enabled) || !isBoolean(fastFrustum)) {
                logger.warn("Invalid boolean value in {}. Lumina Nova will remain disabled; file preserved.", path);
                return new NovaConfig(false, false);
            }
            return new NovaConfig(Boolean.parseBoolean(enabled), Boolean.parseBoolean(fastFrustum));
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("Cannot load {}. Lumina Nova will remain disabled; file preserved.", path, exception);
            return new NovaConfig(false, false);
        }
    }

    private static boolean isBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }
}
