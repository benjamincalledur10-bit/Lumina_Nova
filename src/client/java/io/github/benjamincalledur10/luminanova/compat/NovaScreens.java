package io.github.benjamincalledur10.luminanova.compat;

import io.github.benjamincalledur10.luminanova.gui.NovaVideoSettingsScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class NovaScreens {
    private NovaScreens() { }
    public static Screen create(Screen parent) {
        if (FabricLoader.getInstance().isModLoaded("sodium")) return NovaSodiumIntegration.createScreen(parent);
        Minecraft minecraft = Minecraft.getInstance();
        return new NovaVideoSettingsScreen(parent, minecraft, minecraft.options);
    }
}
