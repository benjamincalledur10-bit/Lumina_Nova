package io.github.benjamincalledur10.luminanova.mixin;

import com.mojang.renderpearl.api.textures.FilterMode;
import io.github.benjamincalledur10.luminanova.config.NovaQualitySettings;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LevelRenderer.class)
public abstract class TerrainSamplerMixin {
    @ModifyArgs(method = "lambda$addMainPass$0", at = @At(value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/device/GpuDevice;createSampler(Lcom/mojang/renderpearl/api/textures/AddressMode;Lcom/mojang/renderpearl/api/textures/AddressMode;Lcom/mojang/renderpearl/api/textures/FilterMode;Lcom/mojang/renderpearl/api/textures/FilterMode;ILjava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/textures/GpuSampler;"), require = 1)
    private void luminanova$texelFilter(Args args) {
        if (!NovaQualitySettings.current().linearTexels()) {
            args.set(2, FilterMode.NEAREST);
            args.set(3, FilterMode.NEAREST);
        }
    }
}
