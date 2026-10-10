package io.github.benjamincalledur10.luminanova.test.mixin;

import io.github.benjamincalledur10.luminanova.test.FrameSamples;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class FrameSampleMixin {
    @Inject(method="stop",at=@At("HEAD"),require=1)
    private void luminanova$stopDiagnostic(CallbackInfo callback) {
        if (Boolean.getBoolean("luminanova.performanceWorld")) {
            System.out.println("LUMINA_CLIENT_STOP " + StackWalker.getInstance().walk(frames ->
                    frames.limit(8).map(Object::toString).collect(java.util.stream.Collectors.joining(" | "))));
        }
    }
    @Inject(method="runTick",at=@At("TAIL"),require=1)
    private void luminanova$frameSample(boolean render,CallbackInfo callback) {
        FrameSamples.frame((Minecraft)(Object)this,render);
    }
}
