package io.github.benjamincalledur10.luminanova.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class BooleanFrustumTestTest {
    @Test
    void matchesActualVanillaAcrossCameraProjectionAndBoxChanges() {
        Random random = new Random(263);
        for (int camera = 0; camera < 100; camera++) {
            Matrix4f view = new Matrix4f().rotateXYZ(random.nextFloat() * 6,
                    random.nextFloat() * 6, random.nextFloat() * 6);
            Matrix4f projection = new Matrix4f().perspective(
                    (float) Math.toRadians(30 + random.nextInt(120)),
                    0.5f + random.nextFloat() * 3, 0.05f, 64 + random.nextInt(960));
            Frustum vanilla = new Frustum(view, projection);
            FrustumIntersection intersection = new FrustumIntersection(new Matrix4f(projection).mul(view));
            double x = random.nextDouble() * 60_000_000 - 30_000_000;
            double y = random.nextDouble() * 1024 - 512;
            double z = random.nextDouble() * 60_000_000 - 30_000_000;
            vanilla.prepare(x, y, z);
            for (int box = 0; box < 10_000; box++) {
                double bx = x + random.nextGaussian() * 256;
                double by = y + random.nextGaussian() * 256;
                double bz = z + random.nextGaussian() * 256;
                AABB bounds = new AABB(bx, by, bz, bx + random.nextDouble() * 64,
                        by + random.nextDouble() * 64, bz + random.nextDouble() * 64);
                assertEquals(vanilla.isVisible(bounds), visible(intersection, x, y, z, bounds));
            }
        }
    }

    @Test
    void matchesPlaneBoundariesDegenerateBoxesAndNonFiniteValues() {
        Matrix4f identity = new Matrix4f();
        Frustum vanilla = new Frustum(identity, identity);
        FrustumIntersection intersection = new FrustumIntersection(identity);
        double[] values = {-Double.MAX_VALUE, -Double.POSITIVE_INFINITY, -2, -1,
                Math.nextDown(-1f), Math.nextUp(-1f), -0.0, 0, 1,
                Math.nextDown(1f), Math.nextUp(1f), 2, Double.MAX_VALUE,
                Double.POSITIVE_INFINITY, Double.NaN};
        for (double min : values) {
            for (double max : values) {
                for (int axis = 0; axis < 3; axis++) {
                    AABB bounds = new AABB(axis == 0 ? min : -0.5, axis == 1 ? min : -0.5,
                            axis == 2 ? min : -0.5, axis == 0 ? max : 0.5,
                            axis == 1 ? max : 0.5, axis == 2 ? max : 0.5);
                    assertEquals(vanilla.isVisible(bounds), visible(intersection, 0, 0, 0, bounds));
                }
            }
        }
    }

    private static boolean visible(FrustumIntersection intersection, double x, double y, double z, AABB box) {
        int result = BooleanFrustumTest.classifyVisibility(intersection, x, y, z,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        return result == FrustumIntersection.INSIDE || result == FrustumIntersection.INTERSECT;
    }
}
