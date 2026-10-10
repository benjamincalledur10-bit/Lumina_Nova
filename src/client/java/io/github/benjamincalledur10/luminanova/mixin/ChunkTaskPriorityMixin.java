package io.github.benjamincalledur10.luminanova.mixin;

import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkTaskPriorityQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-only queue capacity for the already-expanded integrated-server ticket radius. */
@Mixin(ChunkTaskPriorityQueue.class)
public abstract class ChunkTaskPriorityMixin {
    @Shadow @Final @Mutable public static int PRIORITY_LEVEL_COUNT;

    @Inject(method="<clinit>",at=@At("TAIL"),require=1)
    private static void luminanova$extendedPriorities(CallbackInfo callback) {
        // Preserve the vanilla generation margin while raising view distance from 32 to 50.
        PRIORITY_LEVEL_COUNT=Math.max(PRIORITY_LEVEL_COUNT,ChunkLevel.MAX_LEVEL+1+(50-32));
    }
}
