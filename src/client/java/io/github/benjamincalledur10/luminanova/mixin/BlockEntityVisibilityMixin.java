package io.github.benjamincalledur10.luminanova.mixin;

import io.github.benjamincalledur10.luminanova.config.NovaPerformanceSettings;
import io.github.benjamincalledur10.luminanova.config.NovaSettings;
import io.github.benjamincalledur10.luminanova.render.BlockEntityVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityVisibilityMixin {
    @Unique private final BlockPos.MutableBlockPos luminanova$scratch = new BlockPos.MutableBlockPos();
    @Shadow public abstract <E extends BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(E entity);

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true, require = 1)
    private void luminanova$visibleBlock(BlockEntity entity, float partialTick,
            ModelFeatureRenderer.CrumblingOverlay overlay, boolean offscreen,
            CallbackInfoReturnable<BlockEntityRenderState> result) {
        if (!NovaSettings.CONFIG.enabled() || !NovaPerformanceSettings.current().blockEntityCulling()
                || offscreen || overlay != null || !entity.hasLevel()) return;
        if (entity.isRemoved() || !entity.getType().isValid(entity.getBlockState())) return;
        Class<?> blockClass = entity.getBlockState().getBlock().getClass();
        if (blockClass != net.minecraft.world.level.block.ChestBlock.class
                && blockClass != net.minecraft.world.level.block.TrappedChestBlock.class
                && blockClass != net.minecraft.world.level.block.EnderChestBlock.class
                && blockClass != net.minecraft.world.level.block.ShulkerBoxBlock.class
                && blockClass != net.minecraft.world.level.block.DecoratedPotBlock.class) return;
        Class<?> entityClass = entity.getClass();
        if (entityClass != ChestBlockEntity.class
                && entityClass != net.minecraft.world.level.block.entity.TrappedChestBlockEntity.class
                && entityClass != net.minecraft.world.level.block.entity.EnderChestBlockEntity.class
                && entityClass != net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity.class
                && entityClass != net.minecraft.world.level.block.entity.DecoratedPotBlockEntity.class) return;
        var renderer = getRenderer(entity);
        // Exact classes preserve custom/modded renderers and renderers with unbounded effects.
        if (renderer == null || (renderer.getClass() != ChestRenderer.class
                && renderer.getClass() != ShulkerBoxRenderer.class
                && renderer.getClass() != DecoratedPotRenderer.class) || renderer.shouldRenderOffScreen()) return;
        Minecraft minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.mainCamera();
        if (minecraft.level != entity.getLevel() || !camera.isInitialized() || camera.isPanoramicMode()) return;
        BlockPos pos = entity.getBlockPos();
        // Padding covers double chests, opening lids, shulker expansion and pot wobble.
        var frustum = camera.getCapturedFrustum() != null ? camera.getCapturedFrustum() : camera.getCullFrustum();
        if (!frustum.isVisible(new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2))) {
            result.setReturnValue(null);
            return;
        }
        // Only closed single vanilla chests fit wholly inside the enclosed block cell.
        if (entity.getClass() == ChestBlockEntity.class && ((ChestBlockEntity) entity).getOpenNess(partialTick) == 0
                && entity.getBlockState().hasProperty(net.minecraft.world.level.block.ChestBlock.TYPE)
                && entity.getBlockState().getValue(net.minecraft.world.level.block.ChestBlock.TYPE)
                    == net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                && BlockEntityVisibility.enclosed(entity.getLevel(), pos, camera.position(), luminanova$scratch)) {
            result.setReturnValue(null);
        }
    }
}
