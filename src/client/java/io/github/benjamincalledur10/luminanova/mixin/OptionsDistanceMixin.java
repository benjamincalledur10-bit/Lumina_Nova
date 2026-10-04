package io.github.benjamincalledur10.luminanova.mixin;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Options.class)
public abstract class OptionsDistanceMixin {
    @ModifyArgs(method = "<init>",
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;optionsFile:Ljava/io/File;", opcode = 181)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance$IntRange;<init>(IIZ)V", ordinal = 0),
            require = 1)
    private void luminanova$renderRange(Args args) {
        args.set(0, 2);
        args.set(1, 50);
    }

    @ModifyArgs(method = "<init>",
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;optionsFile:Ljava/io/File;", opcode = 181)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance$IntRange;<init>(IIZ)V", ordinal = 1),
            require = 1)
    private void luminanova$simulationRange(Args args) {
        args.set(0, 5);
        args.set(1, 32);
    }
}
