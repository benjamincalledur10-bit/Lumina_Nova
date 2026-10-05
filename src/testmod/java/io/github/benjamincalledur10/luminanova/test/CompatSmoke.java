package io.github.benjamincalledur10.luminanova.test;

import io.github.benjamincalledur10.luminanova.compat.NovaScreens;
import io.github.benjamincalledur10.luminanova.compat.NovaSodiumIntegration;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.Identifier;

/** Optional real Sodium/Iris menu test; excluded from the production JAR. */
public final class CompatSmoke implements ClientModInitializer {
    private int ticks,phase;
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("luminanova.compatSmoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.gui.overlay()!=null || mc.gui.screen()==null || ++ticks<30) return;
            ticks=0;
            try { step(mc); } catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
        });
    }
    private void step(Minecraft mc) {
        var config=ConfigManager.CONFIG;
        if (phase==0) {
            check(config.getModOptions().stream().anyMatch(mod -> mod.configId().equals("luminanova")),"Lumina module registered");
            check(config.getModOptions().stream().anyMatch(mod -> mod.configId().equals("iris")),"Iris module registered");
            var screen=NovaScreens.create(mc.gui.screen());
            check(screen instanceof net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen,"Shared Sodium screen");
            mc.gui.setScreen(screen);
            var lumina=config.getModOptions().stream().filter(mod -> mod.configId().equals("luminanova")).findFirst().orElseThrow();
            ((net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen)screen).jumpToPage(lumina.pages().get(1));
            phase++;
        } else if (phase==1) {
            Screenshot.grab(mc.gameDirectory,"alpha4-sodium-iris.png",mc.gameRenderer.mainRenderTarget(),1,c -> {});
            modify("options.renderClouds",CloudStatus.OFF);
            modify("options.framerateLimit",120);
            modify("texel_interpolation",com.mojang.renderpearl.api.textures.FilterMode.LINEAR);
            modify("fluid_culling",NovaSodiumIntegration.Policy.ALTERNATIVE);
            modify("fluid_shaping",NovaSodiumIntegration.Policy.ALTERNATIVE);
            modify("entity_sorting",NovaSodiumIntegration.Policy.ALTERNATIVE);
            config.applyAllOptions();
            check(mc.options.cloudStatus().get()==CloudStatus.OFF,"Shared clouds apply");
            check(mc.options.framerateLimit().get()==120,"Shared FPS apply");
            var quality=SodiumClientMod.options().quality;
            check(quality.pixelFilteringMode==com.mojang.renderpearl.api.textures.FilterMode.LINEAR,"Sodium texel policy");
            check(quality.hiddenFluidCulling && quality.improvedFluidShaping && quality.useClosestPointEntitySort,"Sodium visual policies");
            check(java.nio.file.Files.exists(mc.gameDirectory.toPath().resolve("config/sodium-options.json")),"Sodium config saved");
            System.out.println("LUMINA_COMPAT_OK sodium=0.9.2 iris=1.11.7");
            mc.stop(); phase++;
        }
    }
    @SuppressWarnings("unchecked") private static <T> void modify(String key,T value) {
        var option=ConfigManager.CONFIG.getOption(Identifier.fromNamespaceAndPath("luminanova",key));
        ((StatefulOption<T>)option).modifyValue(value);
    }
    private static void check(boolean success,String message) { if (!success) throw new AssertionError(message); }
}
