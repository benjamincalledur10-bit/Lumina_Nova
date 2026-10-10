package io.github.benjamincalledur10.luminanova.compat;

import com.mojang.renderpearl.api.textures.FilterMode;
import io.github.benjamincalledur10.luminanova.config.NovaConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import org.slf4j.LoggerFactory;

/** Loaded only by Sodium's optional config entrypoint. No Sodium classes are bundled. */
public final class NovaSodiumIntegration implements ConfigEntryPoint {
    private ConfigBuilder builder;
    private Options options;

    public static Screen createScreen(Screen parent) {
        var config=net.caffeinemc.mods.sodium.client.config.ConfigManager.CONFIG;
        if (config!=null) {
            var lumina=config.getModOptions().stream().filter(mod -> mod.configId().equals("luminanova")).findFirst();
            if (lumina.isPresent() && lumina.get().pages().getFirst() instanceof net.caffeinemc.mods.sodium.client.config.structure.OptionPage page) {
                return net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen.createScreen(parent,page);
            }
        }
        return net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen.createScreen(parent);
    }

    @Override public void registerConfigLate(ConfigBuilder builder) {
        this.builder=builder;
        options=Minecraft.getInstance().options;
        var mod=builder.registerOwnModOptions().setNonTintedIcon(id("icon.png")).setColorTheme(builder.createColorTheme().setBaseThemeRGB(0x80E9CD));
        var general=builder.createOptionPage().setName(text("luminanova.video.general"));
        general.addOption(integer("options.renderDistance",options.renderDistance(),2,50,1,n -> Component.literal(n+" chunks")));
        general.addOption(integer("options.simulationDistance",options.simulationDistance(),5,32,1,n -> Component.literal(n+" chunks")));
        general.addOption(builder.createIntegerOption(id("brightness")).setName(text("options.gamma")).setTooltip(tooltip("options.gamma"))
                .setRange(0,100,1).setDefaultValue(50).setBinding(n -> options.gamma().set(n/100.0),() -> (int)Math.round(options.gamma().get()*100))
                .setValueFormatter(n -> Component.literal(n+"%")).setStorageHandler(options::save));
        general.addOption(integer("options.guiScale",options.guiScale(),0,
                ((OptionInstance.ClampingLazyMaxIntRange)options.guiScale().values()).maxInclusive(),1,
                n -> n==0?text("options.guiScale.auto"):Component.literal(n+"x")));
        bool(general,"options.fullscreen",options.fullscreen(),OptionFlag.REQUIRES_VIDEOMODE_RELOAD);
        var window=Minecraft.getInstance().getWindow();
        var monitor=window.findBestMonitor();
        general.addOption(builder.createIntegerOption(id("fullscreen_resolution")).setName(text("luminanova.options.fullscreen_resolution"))
                .setTooltip(text("options.fullscreen.exclusive.mode.tooltip")).setRange(-1,monitor==null?0:monitor.modeCount()-1,1)
                .setEnabled(monitor!=null).setDefaultValue(-1)
                .setBinding(value -> { if (monitor!=null) window.setPreferredFullscreenVideoMode(value==-1?java.util.Optional.empty():java.util.Optional.of(monitor.mode(value))); },
                        () -> monitor==null?-1:window.getPreferredFullscreenVideoMode().map(monitor::indexOfMode).orElse(-1))
                .setValueFormatter(value -> value==-1?text("options.fullscreen.current"):Component.literal(monitor.mode(value).getWidth()+"x"+monitor.mode(value).getHeight()+" @ "+monitor.mode(value).refreshRateLabel()))
                .setStorageHandler(options::save).setFlags(OptionFlag.REQUIRES_VIDEOMODE_RELOAD));
        bool(general,"options.exclusiveFullscreen",options.exclusiveFullscreen(),OptionFlag.REQUIRES_VIDEOMODE_RELOAD);
        if (com.mojang.blaze3d.platform.MacosUtil.IS_MACOS) bool(general,"options.macFullscreenMenuVisibility",options.macFullscreenMenuVisibility());
        bool(general,"options.vsync",options.enableVsync());
        general.addOption(integer("options.framerateLimit",options.framerateLimit(),10,260,10,
                n -> n==260?text("options.framerateLimit.max"):Component.literal(n+" FPS")));
        Path path=FabricLoader.getInstance().getConfigDir().resolve("luminanova.properties");
        general.addOption(builder.createBooleanOption(id("ultra_optimization")).setName(text("luminanova.options.ultra").copy().append(text("luminanova.options.ultra.preview")))
                .setTooltip(text("luminanova.options.ultra.tooltip")).setStorageHandler(() -> {}).setDefaultValue(false)
                .setBinding(value -> {
                    if (!NovaConfig.saveUltraOptimization(path,value,LoggerFactory.getLogger("luminanova"))) {
                        throw new IllegalStateException("Cannot save Lumina Nova preview preference");
                    }
                },() -> NovaConfig.load(path,LoggerFactory.getLogger("luminanova")).ultraOptimization()));
        mod.addPage(general);

        var quality=builder.createOptionPage().setName(text("luminanova.video.quality"));
        bool(quality,"options.improvedTransparency",options.improvedTransparency(),OptionFlag.REQUIRES_RENDERER_RELOAD);
        enumeration(quality,"options.renderClouds",options.cloudStatus(),CloudStatus.class,CloudStatus::caption);
        quality.addOption(integer("options.renderCloudsDistance",options.cloudRange(),2,128,1,n -> Component.literal(n+" chunks")));
        quality.addOption(integer("options.weatherRadius",options.weatherRadius(),3,10,1,n -> Component.literal(n.toString())));
        bool(quality,"options.cutoutLeaves",options.cutoutLeaves(),OptionFlag.REQUIRES_RENDERER_RELOAD);
        enumeration(quality,"options.particles",options.particles(),ParticleStatus.class,ParticleStatus::caption);
        bool(quality,"options.ao",options.ambientOcclusion(),OptionFlag.REQUIRES_RENDERER_RELOAD);
        quality.addOption(integer("options.biomeBlendRadius",options.biomeBlendRadius(),0,7,1,n -> Component.literal((n*2+1)+"x"+(n*2+1)),OptionFlag.REQUIRES_RENDERER_RELOAD));
        quality.addOption(builder.createIntegerOption(id("entity_distance")).setName(text("options.entityDistanceScaling")).setTooltip(tooltip("options.entityDistanceScaling"))
                .setRange(50,500,25).setDefaultValue(100).setBinding(n -> options.entityDistanceScaling().set(n/100.0),() -> (int)Math.round(options.entityDistanceScaling().get()*100))
                .setValueFormatter(n -> Component.literal(n+"%")).setStorageHandler(options::save));
        bool(quality,"options.entityShadows",options.entityShadows());
        bool(quality,"options.vignette",options.vignette());
        quality.addOption(builder.createIntegerOption(id("chunk_fade")).setName(text("options.chunkFade")).setTooltip(tooltip("options.chunkFade"))
                .setRange(0,40,1).setDefaultValue(15).setBinding(n -> options.chunkSectionFadeInTime().set(n/20.0),() -> (int)Math.round(options.chunkSectionFadeInTime().get()*20))
                .setValueFormatter(n -> text("luminanova.quality.seconds",String.format(java.util.Locale.ROOT,"%.2f",n/20.0))).setStorageHandler(options::save));
        quality.addOption(integer("options.mipmapLevels",options.mipmapLevels(),0,4,1,n -> Component.literal(n+"x"),OptionFlag.REQUIRES_ASSET_RELOAD)
                .setApplyHook(state -> Minecraft.getInstance().updateMaxMipLevel(options.mipmapLevels().get())));
        enumeration(quality,"options.textureFiltering",options.textureFiltering(),TextureFilteringMethod.class,TextureFilteringMethod::caption,OptionFlag.REQUIRES_ASSET_RELOAD);
        quality.addOption(integer("options.maxAnisotropy",options.maxAnisotropyBit(),1,3,1,n -> Component.literal((1<<n)+"x"),OptionFlag.REQUIRES_ASSET_RELOAD)
                .setEnabledProvider(state -> state.readEnumOption(id("options.textureFiltering"),TextureFilteringMethod.class)==TextureFilteringMethod.ANISOTROPIC,id("options.textureFiltering")));
        var sodium=SodiumClientMod.options().quality;
        quality.addOption(builder.createEnumOption(id("texel_interpolation"),FilterMode.class)
                .setName(text("luminanova.quality.texel_interpolation")).setTooltip(text("sodium.options.pixel_filtering_mode.tooltip")).setDefaultValue(FilterMode.NEAREST)
                .setBinding(value -> sodium.pixelFilteringMode=value,() -> sodium.pixelFilteringMode)
                .setElementNameProvider(value -> text("luminanova.quality."+(value==FilterMode.LINEAR?"linear":"nearest")))
                .setStorageHandler(this::saveSodium).setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD));
        policy(quality,"fluid_culling",value -> sodium.hiddenFluidCulling=value,() -> sodium.hiddenFluidCulling,"default","optimized",OptionFlag.REQUIRES_RENDERER_RELOAD);
        policy(quality,"fluid_shaping",value -> sodium.improvedFluidShaping=value,() -> sodium.improvedFluidShaping,"vanilla","alternative",OptionFlag.REQUIRES_RENDERER_RELOAD);
        policy(quality,"entity_sorting",value -> sodium.useClosestPointEntitySort=value,() -> sodium.useClosestPointEntitySort,"default","enhanced");
        mod.addPage(quality);
        var performance=builder.createOptionPage().setName(text("luminanova.video.optimization"));
        performance.addOption(builder.createBooleanOption(id("block_entity_culling"))
                .setName(text("luminanova.options.block_entity_culling"))
                .setTooltip(NovaRenderCompatibility.blockEntityOwner().isEmpty()
                        ? text("luminanova.options.block_entity_culling.tooltip")
                        : text("luminanova.options.culling_owner",NovaRenderCompatibility.blockEntityOwner()))
                .setEnabled(NovaRenderCompatibility.blockEntityOwner().isEmpty()).setDefaultValue(false)
                .setStorageHandler(() -> {}).setBinding(value -> {
                    var requested=new io.github.benjamincalledur10.luminanova.config.NovaPerformanceConfig(value);
                    if (!requested.save(io.github.benjamincalledur10.luminanova.config.NovaPerformanceSettings.PATH,LoggerFactory.getLogger("luminanova"))) {
                        throw new IllegalStateException("Cannot save Lumina Nova performance settings");
                    }
                    io.github.benjamincalledur10.luminanova.config.NovaPerformanceSettings.apply(requested);
                },() -> io.github.benjamincalledur10.luminanova.config.NovaPerformanceSettings.current().blockEntityCulling()));
        mod.addPage(performance);
    }

    private void policy(OptionPageBuilder page,String key,Consumer<Boolean> setter,Supplier<Boolean> getter,String off,String on,OptionFlag... flags) {
        // Two-value enums give the same textual control as the Sodium Quality page.
        page.addOption(builder.createEnumOption(id(key),Policy.class).setName(text("luminanova.quality."+key)).setTooltip(text("sodium.options."+switch(key) { case "fluid_culling" -> "hidden_fluid_culling"; case "fluid_shaping" -> "improved_fluid_shaping"; default -> "closest_point_entity_sort"; }+".tooltip"))
                .setDefaultValue(Policy.DEFAULT).setBinding(value -> setter.accept(value==Policy.ALTERNATIVE),() -> getter.get()?Policy.ALTERNATIVE:Policy.DEFAULT)
                .setElementNameProvider(value -> text("luminanova.quality."+(value==Policy.ALTERNATIVE?on:off)))
                .setStorageHandler(this::saveSodium).setFlags(flags));
    }

    private IntegerOptionBuilder integer(String key,OptionInstance<Integer> option,int min,int max,int step,Function<Integer,Component> display,OptionFlag... flags) {
        return builder.createIntegerOption(id(key)).setName(text(key)).setTooltip(tooltip(key)).setRange(min,max,step)
                .setDefaultValue(option.get()).setBinding(option::set,option::get).setValueFormatter(display::apply)
                .setStorageHandler(options::save).setFlags(flags);
    }

    private void bool(OptionPageBuilder page,String key,OptionInstance<Boolean> option,OptionFlag... flags) {
        page.addOption(builder.createBooleanOption(id(key)).setName(text(key)).setTooltip(tooltip(key)).setDefaultValue(option.get())
                .setBinding(option::set,option::get).setStorageHandler(options::save).setFlags(flags));
    }

    private <T extends Enum<T>> void enumeration(OptionPageBuilder page,String key,OptionInstance<T> option,Class<T> type,Function<T,Component> caption,OptionFlag... flags) {
        page.addOption(builder.createEnumOption(id(key),type).setName(text(key)).setTooltip(tooltip(key)).setDefaultValue(option.get())
                .setBinding(option::set,option::get).setElementNameProvider(caption).setStorageHandler(options::save).setFlags(flags));
    }

    private void saveSodium() {
        try { SodiumOptions.writeToDisk(SodiumClientMod.options()); }
        catch (IOException failure) { throw new IllegalStateException("Cannot save Sodium render settings",failure); }
    }

    private static Component tooltip(String key) {
        return net.minecraft.locale.Language.getInstance().has(key+".tooltip")?text(key+".tooltip"):text(key);
    }
    private static Identifier id(String key) { return NovaOptionIds.of(key); }
    private static Component text(String key,Object... values) { return Component.translatable(key,values); }
    public enum Policy { DEFAULT, ALTERNATIVE }
}
