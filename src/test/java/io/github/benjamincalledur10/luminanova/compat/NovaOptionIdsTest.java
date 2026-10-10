package io.github.benjamincalledur10.luminanova.compat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NovaOptionIdsTest {
    @Test void nativeTranslationKeysBecomeValidResourcePathsInAnyLocale() {
        Locale previous=Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            for (String key : new String[]{"options.renderDistance", "options.simulationDistance", "options.guiScale",
                    "options.exclusiveFullscreen", "options.renderClouds", "options.maxAnisotropy", "options.textureFiltering"}) {
                var id=NovaOptionIds.of(key);
                assertTrue(id.getPath().matches("[a-z0-9/._-]+"),id.toString());
                assertEquals("luminanova",id.getNamespace());
                assertEquals(key.toLowerCase(Locale.ROOT),id.getPath());
            }
        } finally { Locale.setDefault(previous); }
    }
}
