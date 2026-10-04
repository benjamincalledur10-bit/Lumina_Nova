package io.github.benjamincalledur10.luminanova.render;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.openjdk.jmh.annotations.*;

/** Synthetic CPU benchmark of the actual vanilla caller against the replacement caller. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class FrustumBenchmark {
    private static final int BOXES = 4096;
    @Param({"visible", "mixed", "outside"})
    public String distribution;
    private Frustum vanilla;
    private FrustumIntersection intersection;
    private AABB[] boxes;
    private double camX = 29_999_000.25;
    private double camY = 80.5;
    private double camZ = -29_999_000.25;

    @Setup
    public void setup() {
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70), 16f / 9f, 0.05f, 512);
        Matrix4f view = new Matrix4f();
        vanilla = new Frustum(view, projection);
        vanilla.prepare(camX, camY, camZ);
        intersection = new FrustumIntersection(new Matrix4f(projection).mul(view));
        boxes = new AABB[BOXES];
        Random random = new Random(263);
        int visible = 0;
        for (int i = 0; i < BOXES; i++) {
            double depth = 32 + random.nextDouble() * 400;
            double x = switch (distribution) {
                case "visible" -> (random.nextDouble() - 0.5) * depth * 0.5;
                case "outside" -> depth * 3;
                default -> (random.nextDouble() - 0.5) * depth * 5;
            };
            double y = (random.nextDouble() - 0.5) * depth * 0.5;
            boxes[i] = new AABB(camX + x, camY + y, camZ - depth,
                    camX + x + 2, camY + y + 2, camZ - depth + 2);
            boolean expected = vanilla.isVisible(boxes[i]);
            if (expected != optimized(boxes[i])) throw new AssertionError("Visibility mismatch");
            if (expected) visible++;
        }
        System.out.println("Dataset " + distribution + ": " + visible + "/" + BOXES + " visible");
    }

    @Benchmark
    @OperationsPerInvocation(BOXES)
    public int vanilla() {
        int visible = 0;
        for (AABB box : boxes) if (vanilla.isVisible(box)) visible++;
        return visible;
    }

    @Benchmark
    @OperationsPerInvocation(BOXES)
    public int nova() {
        int visible = 0;
        for (AABB box : boxes) if (optimized(box)) visible++;
        return visible;
    }

    private boolean optimized(AABB box) {
        int result = BooleanFrustumTest.classifyVisibility(intersection, camX, camY, camZ,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        return result == FrustumIntersection.INSIDE || result == FrustumIntersection.INTERSECT;
    }
}
