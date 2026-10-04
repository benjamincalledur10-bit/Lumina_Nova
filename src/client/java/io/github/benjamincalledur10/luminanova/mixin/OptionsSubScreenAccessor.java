package io.github.benjamincalledur10.luminanova.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.components.OptionsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionsSubScreen.class)
public interface OptionsSubScreenAccessor {
    @Accessor("lastScreen")
    Screen luminanova$parent();

    @Accessor("list")
    OptionsList luminanova$list();
}
