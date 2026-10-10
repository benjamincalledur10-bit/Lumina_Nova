package io.github.benjamincalledur10.luminanova.config;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.Properties;
import org.slf4j.Logger;

public record NovaPerformanceConfig(boolean blockEntityCulling) {
    public static final NovaPerformanceConfig DEFAULT = new NovaPerformanceConfig(false);
    private static final String KEY = "block_entity_culling";

    public static NovaPerformanceConfig load(Path path, Logger logger) {
        if (Files.notExists(path)) return DEFAULT;
        try {
            Properties properties = read(path);
            validate(properties);
            return new NovaPerformanceConfig(Boolean.parseBoolean(properties.getProperty(KEY, "false")));
        } catch (IOException | IllegalArgumentException failure) {
            logger.warn("Cannot load performance settings {}; optimizations disabled, file preserved.", path, failure);
            return DEFAULT;
        }
    }

    public boolean save(Path path, Logger logger) {
        Path temporary = null;
        try {
            Properties properties = Files.exists(path) ? read(path) : new Properties();
            validate(properties);
            properties.setProperty(KEY, Boolean.toString(blockEntityCulling));
            StringWriter content = new StringWriter();
            properties.store(content, "Lumina Nova experimental performance settings. Changes apply immediately.");
            Path destination = path.toAbsolutePath();
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), "luminanova-performance-", ".tmp");
            Files.writeString(temporary, content.toString());
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | IllegalArgumentException failure) {
            logger.warn("Cannot save performance settings {}; file preserved.", path, failure);
            return false;
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private static Properties read(Path path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path)) { properties.load(reader); }
        return properties;
    }

    private static void validate(Properties properties) {
        String value = properties.getProperty(KEY, "false");
        if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Invalid " + KEY);
    }
}
