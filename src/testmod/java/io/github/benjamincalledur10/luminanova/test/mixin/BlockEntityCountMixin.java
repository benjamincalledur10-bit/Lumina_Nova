package io.github.benjamincalledur10.luminanova.test.mixin;

import io.github.benjamincalledur10.luminanova.test.FrameSamples;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=BlockEntityRenderDispatcher.class,priority=900)
public class BlockEntityCountMixin {
    @Inject(method="tryExtractRenderState",at=@At("RETURN"),require=1)
    private void luminanova$count(CallbackInfoReturnable<BlockEntityRenderState> result) {
        FrameSamples.extracted(result.getReturnValue()!=null);
    }
}
