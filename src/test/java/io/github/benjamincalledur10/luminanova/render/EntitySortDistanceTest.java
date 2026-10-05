package io.github.benjamincalledur10.luminanova.render;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EntitySortDistanceTest {
    @Test void cameraInsideBoundsAndDistanceToFaces() {
        assertEquals(0,EntitySortDistance.closest(0,0,0,2,3,0,1,0));
        assertEquals(4,EntitySortDistance.closest(0,0,0,2,3,3,1,0));
        assertEquals(1,EntitySortDistance.closest(0,0,0,2,3,0,4,0));
        assertEquals(3,EntitySortDistance.closest(0,0,0,2,3,2,4,2));
    }
    @Test void largeWorldCoordinatesRetainDistanceAndReverseAnOriginBasedOrder() {
        assertEquals(4,EntitySortDistance.closest(30_000_000,0,0,2,2,30_000_003,1,0));
        double large=EntitySortDistance.closest(10,0,0,18,2,0,1,0);
        double small=EntitySortDistance.closest(5,0,0,1,2,0,1,0);
        assertTrue(large<small,"Closest surface order differs from entity center order");
    }
}
