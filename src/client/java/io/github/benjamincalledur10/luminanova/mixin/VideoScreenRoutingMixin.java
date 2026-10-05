package io.github.benjamincalledur10.luminanova.mixin;

import io.github.benjamincalledur10.luminanova.compat.NovaScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public abstract class VideoScreenRoutingMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 1)
    private Screen luminanova$videoSettings(Screen screen) {
        if (screen != null && screen.getClass() == VideoSettingsScreen.class) {
            return NovaScreens.create(((OptionsSubScreenAccessor)screen).luminanova$parent());
        }
        return screen;
    }
}
