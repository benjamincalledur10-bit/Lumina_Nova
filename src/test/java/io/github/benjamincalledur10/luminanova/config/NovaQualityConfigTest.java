package io.github.benjamincalledur10.luminanova.config;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class NovaQualityConfigTest {
    @TempDir Path directory;
    private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("quality-test");

    @Test void defaultsPreserveVanillaWithoutWriting() {
        Path path=directory.resolve("quality.properties");
        assertEquals(NovaQualityConfig.DEFAULT,NovaQualityConfig.load(path,LOG));
        assertFalse(Files.exists(path));
    }
    @Test void allPoliciesRoundTripAndPreserveForeignValues() throws Exception {
        Path path=directory.resolve("quality.properties");
        Files.writeString(path,"external_setting=hola\n");
        var requested=new NovaQualityConfig(false,true,true,true);
        assertTrue(requested.save(path,LOG));
        assertEquals(requested,NovaQualityConfig.load(path,LOG));
        assertTrue(Files.readString(path).contains("external_setting=hola"));
    }
    @Test void invalidConfigIsPreservedOnLoadAndSave() throws Exception {
        Path path=directory.resolve("quality.properties");
        String invalid="fluid_culling=yes\n";
        Files.writeString(path,invalid);
        assertEquals(NovaQualityConfig.DEFAULT,NovaQualityConfig.load(path,LOG));
        assertFalse(new NovaQualityConfig(false,true,true,true).save(path,LOG));
        assertEquals(invalid,Files.readString(path));
    }
}
