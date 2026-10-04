package io.github.benjamincalledur10.luminanova.render;

import org.joml.FrustumIntersection;

/** Uses the same planes and float conversion as vanilla, without computing full containment. */
public final class BooleanFrustumTest {
    private BooleanFrustumTest() {}

    public static int classifyVisibility(FrustumIntersection intersection,
            double camX, double camY, double camZ,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return intersection.testAab(
                (float) (minX - camX), (float) (minY - camY), (float) (minZ - camZ),
                (float) (maxX - camX), (float) (maxY - camY), (float) (maxZ - camZ))
                ? FrustumIntersection.INTERSECT : FrustumIntersection.PLANE_NX;
    }
}
