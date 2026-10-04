package io.github.benjamincalledur10.luminanova.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class NovaConfigTest {
    @TempDir Path directory;

    private NovaConfig load(Path path) {
        return NovaConfig.load(path, LoggerFactory.getLogger("config-test"));
    }

    @Test
    void createsOptInDefaultsAndPreservesLegacyConfiguration() throws Exception {
        Path fresh = directory.resolve("nested/nova.properties");
        assertEquals(new NovaConfig(true, false), load(fresh));
        assertTrue(Files.readString(fresh).contains("fast_frustum=false"));
        Path legacy = directory.resolve("legacy.properties");
        Files.writeString(legacy, "# user settings\nenabled=false\n");
        String original = Files.readString(legacy);
        assertEquals(new NovaConfig(false, false), load(legacy));
        assertEquals(original, Files.readString(legacy));
    }

    @Test
    void parsesOptInAndDisablesInvalidFilesWithoutOverwriting() throws Exception {
        Path path = directory.resolve("nova.properties");
        Files.writeString(path, "enabled=TRUE\nfast_frustum=true\n");
        assertEquals(new NovaConfig(true, true), load(path));
        for (String content : new String[] {"enabled=oops\nfast_frustum=true\n",
                "enabled=true\nfast_frustum=oops\n", "enabled=\\uZZZZ\n"}) {
            Files.writeString(path, content);
            assertEquals(new NovaConfig(false, false), load(path));
            assertEquals(content, Files.readString(path));
        }
    }

    @Test
    void disablesUnreadableConfiguration() throws Exception {
        Path path = directory.resolve("directory.properties");
        Files.createDirectory(path);
        assertEquals(new NovaConfig(false, false), load(path));
        assertTrue(Files.isDirectory(path));
    }

    @Test
    void savesPreviewPreferenceAndPreservesOtherPropertyValues() throws Exception {
        Path path = directory.resolve("nova.properties");
        Files.writeString(path, "enabled=false\nfast_frustum=true\ncustom_key=valor único\n");
        assertTrue(NovaConfig.saveUltraOptimization(path, true, LoggerFactory.getLogger("config-test")));
        assertEquals(new NovaConfig(false, true, true), load(path));
        java.util.Properties properties = new java.util.Properties();
        try (var reader = Files.newBufferedReader(path)) { properties.load(reader); }
        assertEquals("valor único", properties.getProperty("custom_key"));
        assertTrue(NovaConfig.saveUltraOptimization(path, false, LoggerFactory.getLogger("config-test")));
        assertFalse(load(path).ultraOptimization());
    }

    @Test
    void previewSaveRefusesInvalidConfigurationWithoutChangingIt() throws Exception {
        Path path = directory.resolve("nova.properties");
        String invalid = "enabled=true\nfast_frustum=oops\nultra_optimization=false\n";
        Files.writeString(path, invalid);
        assertFalse(NovaConfig.saveUltraOptimization(path, true, LoggerFactory.getLogger("config-test")));
        assertEquals(invalid, Files.readString(path));
    }
}
