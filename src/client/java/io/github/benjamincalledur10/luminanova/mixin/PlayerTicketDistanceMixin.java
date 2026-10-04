package io.github.benjamincalledur10.luminanova.mixin;

import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DistanceManager.class)
public abstract class PlayerTicketDistanceMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 32), require = 1, allow = 1)
    private int luminanova$localTicketRadius(int vanillaMaximum) {
        return 50;
    }
}
