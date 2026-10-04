package io.github.benjamincalledur10.luminanova.test;

import io.github.benjamincalledur10.luminanova.config.NovaSettings;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/** Test-only mod: verifies the transformed game class, then exits before playing. Never packaged. */
public final class FrustumSmoke implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (System.getProperty("luminanova.smoke.fast") == null) return;
        try {
            boolean expected = Boolean.getBoolean("luminanova.smoke.fast")
                    && Boolean.getBoolean("luminanova.smoke.enabled");
            check(expected == NovaSettings.FAST_FRUSTUM_ENABLED, "Startup toggle mismatch");
            Method hook = Arrays.stream(Frustum.class.getDeclaredMethods())
                    .filter(method -> method.getName().contains("luminanova$booleanVisibility"))
                    .findFirst().orElseThrow(() -> new AssertionError("Mixin was not applied"));
            hook.setAccessible(true);
            Method baseline = Frustum.class.getDeclaredMethod("cubeInFrustum", double.class,
                    double.class, double.class, double.class, double.class, double.class);
            baseline.setAccessible(true);
            Frustum identity = new Frustum(new Matrix4f(), new Matrix4f());
            int inside = (int) hook.invoke(identity, identity, -0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
            check(inside == (expected ? -1 : -2), "Hook did not select the requested path");
            Random random = new Random(263);
            int checked = 0;
            for (int camera = 0; camera < 30; camera++) {
                Matrix4f view = new Matrix4f().rotateXYZ(random.nextFloat() * 6,
                        random.nextFloat() * 6, random.nextFloat() * 6);
                Matrix4f projection = new Matrix4f().perspective(
                        (float) Math.toRadians(30 + random.nextInt(120)), 16f / 9f, 0.05f, 512);
                Frustum original = new Frustum(view, projection);
                original.prepare(29_999_000.25, 90.5, -29_999_000.25);
                Frustum frustum = new Frustum(original);
                frustum.offset(4);
                frustum.set(original);
                frustum.offsetToFullyIncludeCameraCube(16);
                for (int i = 0; i < 10_000; i++) {
                    double x = frustum.getCamX() + random.nextGaussian() * 256;
                    double y = frustum.getCamY() + random.nextGaussian() * 256;
                    double z = frustum.getCamZ() + random.nextGaussian() * 256;
                    AABB box = new AABB(x, y, z, x + 16, y + 16, z + 16);
                    int result = (int) baseline.invoke(frustum, box.minX, box.minY, box.minZ,
                            box.maxX, box.maxY, box.maxZ);
                    check(frustum.isVisible(box) == (result == -1 || result == -2), "Visibility mismatch");
                    checked++;
                }
            }
            System.out.println("LUMINA_SMOKE_OK fast=" + expected + " comparisons=" + checked);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
