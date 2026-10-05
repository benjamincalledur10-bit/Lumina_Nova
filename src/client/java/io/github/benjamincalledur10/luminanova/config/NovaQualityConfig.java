package io.github.benjamincalledur10.luminanova.config;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import org.slf4j.Logger;

/** Independent visual policies for the vanilla renderer; defaults preserve vanilla. */
public record NovaQualityConfig(boolean linearTexels, boolean fluidCulling,
                                boolean alternativeFluids, boolean enhancedEntities) {
    public static final NovaQualityConfig DEFAULT = new NovaQualityConfig(true, false, false, false);
    private static final String[] KEYS = {"linear_texels", "fluid_culling", "alternative_fluids", "enhanced_entities"};

    public static NovaQualityConfig load(Path path, Logger logger) {
        if (Files.notExists(path)) return DEFAULT;
        try {
            Properties values = read(path);
            validate(values);
            return new NovaQualityConfig(Boolean.parseBoolean(values.getProperty(KEYS[0], "true")),
                    Boolean.parseBoolean(values.getProperty(KEYS[1], "false")),
                    Boolean.parseBoolean(values.getProperty(KEYS[2], "false")),
                    Boolean.parseBoolean(values.getProperty(KEYS[3], "false")));
        } catch (IOException | IllegalArgumentException failure) {
            logger.warn("Cannot load quality settings {}; keeping vanilla defaults, file preserved.", path, failure);
            return DEFAULT;
        }
    }

    public boolean save(Path path, Logger logger) {
        Path temporary = null;
        try {
            Properties values = Files.exists(path) ? read(path) : new Properties();
            validate(values);
            boolean[] requested = {linearTexels, fluidCulling, alternativeFluids, enhancedEntities};
            for (int i = 0; i < KEYS.length; i++) values.setProperty(KEYS[i], Boolean.toString(requested[i]));
            StringWriter content = new StringWriter();
            values.store(content, "Lumina Nova quality: vanilla renderer visual policies.");
            Path destination = path.toAbsolutePath();
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), "luminanova-quality-", ".tmp");
            Files.writeString(temporary, content.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | IllegalArgumentException failure) {
            logger.warn("Cannot save quality settings {}; file preserved.", path, failure);
            return false;
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private static Properties read(Path path) throws IOException {
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { values.load(reader); }
        return values;
    }

    private static void validate(Properties values) {
        for (String key : KEYS) {
            String value = values.getProperty(key);
            if (value != null && !value.equals("true") && !value.equals("false")) {
                throw new IllegalArgumentException("Invalid boolean: " + key);
            }
        }
    }
}
