package io.github.benjamincalledur10.luminanova.mixin;

import io.github.benjamincalledur10.luminanova.config.NovaSettings;
import io.github.benjamincalledur10.luminanova.render.BooleanFrustumTest;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Frustum.class)
public abstract class FrustumMixin {
    @Shadow @Final private FrustumIntersection intersection;
    @Shadow private double camX;
    @Shadow private double camY;
    @Shadow private double camZ;

    @Shadow
    private int cubeInFrustum(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        throw new AssertionError("Mixin shadow");
    }

    // Only the boolean caller is redirected. Other callers still receive vanilla's full classification.
    @Redirect(method = "isVisible(Lnet/minecraft/world/phys/AABB;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/culling/Frustum;cubeInFrustum(DDDDDD)I"),
            require = 1)
    private int luminanova$booleanVisibility(Frustum instance, double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        if (!NovaSettings.FAST_FRUSTUM_ENABLED) {
            return cubeInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
        }
        return BooleanFrustumTest.classifyVisibility(intersection, camX, camY, camZ,
                minX, minY, minZ, maxX, maxY, maxZ);
    }
}
