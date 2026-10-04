package io.github.benjamincalledur10.luminanova.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.benjamincalledur10.luminanova.gui.NovaVideoSettingsScreen;
import net.minecraft.client.Minecraft;

/** Only invoked by Mod Menu; the mod remains usable without it. */
public final class NovaModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new NovaVideoSettingsScreen(parent, Minecraft.getInstance(), Minecraft.getInstance().options);
    }
}
