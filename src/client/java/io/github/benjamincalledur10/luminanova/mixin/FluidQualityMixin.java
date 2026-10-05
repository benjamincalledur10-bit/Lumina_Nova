package io.github.benjamincalledur10.luminanova.mixin;

import io.github.benjamincalledur10.luminanova.config.NovaQualitySettings;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidRenderer.class)
public abstract class FluidQualityMixin {
    @Shadow private static boolean isFaceOccludedBySelf(BlockState state, Direction direction) { throw new AssertionError(); }

    /** Avoid top surfaces entirely hidden by the waterlogged block's own top face. */
    @Redirect(method = "tesselate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/FluidRenderer;isNeighborSameFluid(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/material/FluidState;)Z", ordinal = 0), require = 1)
    private boolean luminanova$hiddenTop(FluidState fluid, FluidState above, BlockAndTintGetter level,
            BlockPos pos, FluidRenderer.Output output, BlockState state, FluidState original) {
        return fluid.getType().isSame(above.getType())
                || (NovaQualitySettings.current().fluidCulling() && isFaceOccludedBySelf(state, Direction.UP));
    }

    /** Alternative visual surface: level corners adjacent to waterlogged blocks to the tallest fluid sample. */
    @Inject(method = "calculateAverageHeight", at = @At("HEAD"), cancellable = true, require = 1)
    private void luminanova$waterloggedCorner(BlockAndTintGetter level, Fluid type, float self,
            float a, float b, BlockPos corner, CallbackInfoReturnable<Float> result) {
        if (!NovaQualitySettings.current().alternativeFluids()) return;
        // Scan the corner neighborhood; neither fluid simulation nor unwaterlogged corners change.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockState state = level.getBlockState(corner.offset(dx, 0, dz));
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)
                    && type.isSame(state.getFluidState().getType())) {
                float height = Math.max(state.getFluidState().getOwnHeight(), Math.max(self, Math.max(a, b)));
                if (Float.isFinite(height) && height >= 0) result.setReturnValue(Math.min(1, height));
                return;
            }
        }
    }
}
