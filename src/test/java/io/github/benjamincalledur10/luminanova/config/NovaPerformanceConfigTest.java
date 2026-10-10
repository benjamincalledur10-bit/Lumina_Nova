package io.github.benjamincalledur10.luminanova.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

class NovaPerformanceConfigTest {
    @TempDir Path directory;
    private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("performance-test");
    @Test void experimentalDefaultIsOptInAndSavingPreservesForeignProperties() throws Exception {
        Path path=directory.resolve("nested/performance.properties");
        assertEquals(NovaPerformanceConfig.DEFAULT,NovaPerformanceConfig.load(path,LOG));
        assertFalse(Files.exists(path));
        assertTrue(new NovaPerformanceConfig(true).save(path,LOG));
        Files.writeString(path,Files.readString(path)+"other_mod_key=keep\n");
        assertTrue(new NovaPerformanceConfig(false).save(path,LOG));
        assertEquals(NovaPerformanceConfig.DEFAULT,NovaPerformanceConfig.load(path,LOG));
        assertTrue(Files.readString(path).contains("other_mod_key=keep"));
    }
    @Test void malformedFilesDisableTheFeatureAndAreNeverOverwritten() throws Exception {
        Path path=directory.resolve("performance.properties");
        for (String invalid : new String[]{"block_entity_culling=oops\n","block_entity_culling=\\uZZZZ\n"}) {
            Files.writeString(path,invalid);
            assertEquals(NovaPerformanceConfig.DEFAULT,NovaPerformanceConfig.load(path,LOG));
            assertFalse(new NovaPerformanceConfig(true).save(path,LOG));
            assertEquals(invalid,Files.readString(path));
        }
    }
}
