package io.github.benjamincalledur10.luminanova.config;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.Properties;
import org.slf4j.Logger;

/** Conservative startup loading, with explicit UI saves for the preview preference. */
public record NovaConfig(boolean enabled, boolean fastFrustum, boolean ultraOptimization) {
    public NovaConfig(boolean enabled, boolean fastFrustum) {
        this(enabled, fastFrustum, false);
    }
    public static NovaConfig load(Path path, Logger logger) {
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, "# Lumina Nova. Restart Minecraft after editing.\nenabled=true\n"
                                + "# Experimental boolean frustum test; opt in until in-game validation.\nfast_frustum=false\n"
                                + "# Preview preference only; no Ultra engine is implemented in this alpha.\nultra_optimization=false\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            String enabled = properties.getProperty("enabled", "true").trim();
            String fastFrustum = properties.getProperty("fast_frustum", "false").trim();
            String ultra = properties.getProperty("ultra_optimization", "false").trim();
            if (!isBoolean(enabled) || !isBoolean(fastFrustum) || !isBoolean(ultra)) {
                logger.warn("Invalid boolean value in {}. Lumina Nova will remain disabled; file preserved.", path);
                return new NovaConfig(false, false);
            }
            return new NovaConfig(Boolean.parseBoolean(enabled), Boolean.parseBoolean(fastFrustum), Boolean.parseBoolean(ultra));
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("Cannot load {}. Lumina Nova will remain disabled; file preserved.", path, exception);
            return new NovaConfig(false, false);
        }
    }

    /** Explicit UI save: preserves property values, normalizes formatting, and replaces atomically. */
    public static boolean saveUltraOptimization(Path path, boolean requested, Logger logger) {
        Path temporary = null;
        try {
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            for (String key : new String[] {"enabled", "fast_frustum", "ultra_optimization"}) {
                if (!isBoolean(properties.getProperty(key, key.equals("enabled") ? "true" : "false").trim())) {
                    logger.warn("Cannot save preview preference: invalid {} in {}; file preserved.", key, path);
                    return false;
                }
            }
            properties.setProperty("ultra_optimization", Boolean.toString(requested));
            StringWriter content = new StringWriter();
            properties.store(content, "Lumina Nova. Ultra Optimization is a preview preference; no engine effect in this alpha.");
            Path destination = path.toAbsolutePath();
            temporary = Files.createTempFile(destination.getParent(), "luminanova-", ".tmp");
            Files.writeString(temporary, content.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("Cannot save preview preference in {}; file preserved.", path, exception);
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private static boolean isBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }
}
