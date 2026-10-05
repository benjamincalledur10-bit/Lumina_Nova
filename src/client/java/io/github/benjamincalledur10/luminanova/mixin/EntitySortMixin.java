package io.github.benjamincalledur10.luminanova.mixin;

import io.github.benjamincalledur10.luminanova.config.NovaQualitySettings;
import io.github.benjamincalledur10.luminanova.render.EntitySortDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelFeatureRenderer.Submit.class)
public abstract class EntitySortMixin<S> {
    @Shadow @Final private S state;

    @Inject(method = "distanceToCameraSq", at = @At("HEAD"), cancellable = true, require = 1)
    private void luminanova$nearestEntity(CallbackInfoReturnable<Float> result) {
        if (!NovaQualitySettings.current().enhancedEntities() || !(state instanceof EntityRenderState entity)) return;
        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        double distance = EntitySortDistance.closest(entity.x, entity.y, entity.z,
                entity.boundingBoxWidth, entity.boundingBoxHeight, camera.x, camera.y, camera.z);
        if (entity.boundingBoxWidth >= 0 && entity.boundingBoxHeight >= 0 && Double.isFinite(distance)) {
            result.setReturnValue((float)distance);
        }
    }
}
