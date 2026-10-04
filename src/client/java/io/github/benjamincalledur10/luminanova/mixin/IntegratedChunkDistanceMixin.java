package io.github.benjamincalledur10.luminanova.mixin;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Client-only mixin: raises the integrated server cap, never a remote server's distance. */
@Mixin(ChunkMap.class)
public abstract class IntegratedChunkDistanceMixin {
    @ModifyArg(method = "setServerViewDistance", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;clamp(III)I"), index = 2, require = 1)
    private static int luminanova$localRenderLimit(int vanillaMaximum) {
        return 50;
    }
}
