package io.github.benjamincalledur10.luminanova.test;

import io.github.benjamincalledur10.luminanova.config.NovaConfig;
import io.github.benjamincalledur10.luminanova.config.NovaQualityConfig;
import io.github.benjamincalledur10.luminanova.config.NovaQualitySettings;
import io.github.benjamincalledur10.luminanova.gui.NovaVideoSettingsScreen;
import io.github.benjamincalledur10.luminanova.mixin.OptionsSubScreenAccessor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.level.TicketStorage;
import org.slf4j.LoggerFactory;

/** Test-only client: checks real widgets/persistence and captures the game framebuffer. */
public final class UiSmoke implements ClientModInitializer {
    private int phase;
    private int ticks;
    private Screen parent;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("luminanova.uiSmoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            try { step(minecraft); }
            catch (Throwable failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        });
    }

    private void step(Minecraft minecraft) throws Exception {
        if (minecraft.gui.overlay() != null || minecraft.gui.screen() == null) return;
        if (++ticks < 30) return;
        ticks = 0;
        if (phase == 0) {
            parent = minecraft.gui.screen();
            verifyIntegratedServerLimits();
            minecraft.options.framerateLimit().set(250);
            minecraft.gui.setScreen(new VideoSettingsScreen(parent, minecraft, minecraft.options));
            check(minecraft.gui.screen() instanceof NovaVideoSettingsScreen, "Video routing");
            phase++;
        } else if (phase == 1) {
            capture(minecraft,"alpha5-general.png");
            var screen = (NovaVideoSettingsScreen) minecraft.gui.screen();
            int original = minecraft.options.renderDistance().get();
            slider(row(screen, "options.renderDistance"), 1);
            slider(row(screen, "options.simulationDistance"), 1);
            slider(row(screen, "options.gamma"), 0.6);
            slider(row(screen, "options.framerateLimit"), 11.0 / 25.0);
            check(minecraft.options.renderDistance().get() == original, "Staged settings before Apply");
            AbstractWidget vsync = row(screen, "options.vsync");
            if (minecraft.options.enableVsync().get()) slider(vsync, 1);
            press(screen, "luminanova.video.apply");
            check(minecraft.options.renderDistance().get() == 50, "Render applies");
            check(minecraft.options.simulationDistance().get() == 32, "Simulation applies");
            check(Math.abs(minecraft.options.gamma().get() - 0.6) < 0.001, "Brightness applies");
            check(!minecraft.options.enableVsync().get(), "VSync applies");
            check(minecraft.options.framerateLimit().get() == 120, "FPS option applies 120");
            var tracker = minecraft.getFramerateLimitTracker();
            Field limit = tracker.getClass().getDeclaredField("framerateLimit"); limit.setAccessible(true);
            check(limit.getInt(tracker) == 120, "Live engine frame limiter 120");
            check(tracker.getFramerateLimit() <= 120, "Effective limiter respects configured maximum");
            slider(row(screen, "options.vsync"), 1);
            press(screen, "luminanova.video.apply");
            check(minecraft.options.enableVsync().get(), "VSync enable applies");
            slider(row(screen, "options.vsync"), 1);
            slider(row(screen, "options.fullscreen"), 1);
            press(screen, "luminanova.video.apply");
            Field requested = minecraft.getWindow().getClass().getDeclaredField("fullscreenRequested"); requested.setAccessible(true);
            check(minecraft.options.fullscreen().get() && requested.getBoolean(minecraft.getWindow()), "Fullscreen callback requests enter");
            minecraft.getWindow().updateFullscreenIfChanged();
            slider(row(screen, "options.fullscreen"), 1);
            press(screen, "luminanova.video.apply");
            check(!requested.getBoolean(minecraft.getWindow()), "Fullscreen callback requests exit");
            minecraft.getWindow().updateFullscreenIfChanged();
            screen.mouseScrolled(screen.width - 30, 60, 0, -30);
            slider(row(screen, "luminanova.options.ultra"), 1);
            phase++;
        } else if (phase == 2) {
            var screen=minecraft.gui.screen();
            press(screen,"luminanova.video.quality");
            check(row(screen,"options.renderClouds").visible,"Quality page shows cloud control");
            slider(row(screen,"options.improvedTransparency"),1);
            slider(row(screen,"options.renderClouds"),0); // Fancy -> Off.
            check(minecraft.options.cloudStatus().get()!=net.minecraft.client.CloudStatus.OFF,"Clouds staged");
            // Configure the range before disabling it through the pending cloud policy.
            press(screen,"luminanova.video.apply");
            check(minecraft.options.cloudStatus().get()==net.minecraft.client.CloudStatus.OFF,"Clouds off applies");
            slider(row(screen,"options.renderClouds"),0); // Off -> Fast, enables distance.
            slider(row(screen,"options.renderCloudsDistance"),30.0/126.0);
            slider(row(screen,"options.weatherRadius"),2.0/7.0);
            slider(row(screen,"options.cutoutLeaves"),0);
            slider(row(screen,"options.particles"),1);
            slider(row(screen,"options.ao"),0);
            slider(row(screen,"options.biomeBlendRadius"),1.0/7.0);
            slider(row(screen,"options.entityDistanceScaling"),1.0/18.0);
            slider(row(screen,"options.entityShadows"),0);
            slider(row(screen,"options.vignette"),0);
            slider(row(screen,"options.chunkFade"),15.0/40.0);
            slider(row(screen,"options.mipmapLevels"),0.5);
            slider(row(screen,"options.textureFiltering"),1); // None -> RGSS.
            slider(row(screen,"options.textureFiltering"),1); // RGSS -> Anisotropic.
            slider(row(screen,"options.maxAnisotropy"),1);
            slider(row(screen,"luminanova.quality.texel_interpolation"),0);
            slider(row(screen,"luminanova.quality.fluid_culling"),1);
            slider(row(screen,"luminanova.quality.fluid_shaping"),1);
            slider(row(screen,"luminanova.quality.entity_sorting"),1);
            press(screen,"luminanova.video.apply");
            check(minecraft.options.improvedTransparency().get(),"Improved transparency applies");
            check(minecraft.options.cloudRange().get()==32,"Cloud range");
            check(minecraft.options.weatherRadius().get()==5,"Weather radius");
            check(!minecraft.options.cutoutLeaves().get(),"Opaque leaves");
            check(minecraft.options.particles().get()==net.minecraft.server.level.ParticleStatus.DECREASED,"Particles");
            check(!minecraft.options.ambientOcclusion().get(),"Smooth lighting off");
            check(minecraft.options.biomeBlendRadius().get()==1,"Biome blend 3x3");
            check(minecraft.options.entityDistanceScaling().get()==0.75,"Entity distance 75%");
            check(!minecraft.options.entityShadows().get(),"Entity shadows off");
            check(!minecraft.options.vignette().get(),"Vignette off");
            check(minecraft.options.chunkSectionFadeInTime().get()==0.75,"Chunk fade");
            check(minecraft.options.mipmapLevels().get()==2,"Mipmap levels");
            check(minecraft.options.textureFiltering().get()==net.minecraft.client.TextureFilteringMethod.ANISOTROPIC,"Texture filter");
            check(minecraft.options.maxAnisotropyBit().get()==3,"Anisotropy 8x");
            check(NovaQualitySettings.current().equals(new NovaQualityConfig(false,true,true,true)),"Quality policies active");
            check(NovaQualityConfig.load(NovaQualitySettings.PATH,LoggerFactory.getLogger("quality-smoke")).equals(NovaQualitySettings.current()),"Quality policies persisted");
            // Leave clouds disabled as the user's concrete example.
            slider(row(screen,"options.renderClouds"),1); // Fast -> Fancy.
            slider(row(screen,"options.renderClouds"),1); // Fancy -> Off.
            press(screen,"luminanova.video.accept");
            check(minecraft.gui.screen() == parent, "Accept returns to parent");
            Path config = FabricLoader.getInstance().getConfigDir().resolve("luminanova.properties");
            check(NovaConfig.load(config, LoggerFactory.getLogger("ui-smoke")).ultraOptimization(), "Ultra saved");
            minecraft.options.renderDistance().set(12);
            minecraft.options.simulationDistance().set(12);
            minecraft.options.framerateLimit().set(250);
            minecraft.options.load();
            check(minecraft.options.renderDistance().get() == 50, "Render reload");
            check(minecraft.options.simulationDistance().get() == 32, "Simulation reload");
            check(minecraft.options.framerateLimit().get() == 120, "FPS reload");
            check(minecraft.options.cloudStatus().get()==net.minecraft.client.CloudStatus.OFF,"Clouds off reload");
            check(minecraft.options.mipmapLevels().get()==2,"Mipmaps reload");
            minecraft.gui.setScreen(new NovaVideoSettingsScreen(parent, minecraft, minecraft.options));
            slider(row(minecraft.gui.screen(), "options.renderDistance"), 0);
            minecraft.gui.screen().onClose();
            check(minecraft.options.renderDistance().get() == 50, "Escape discards pending edits");
            minecraft.gui.setScreen(new NovaVideoSettingsScreen(parent, minecraft, minecraft.options));
            var scaleRow=row(minecraft.gui.screen(),"options.guiScale");
            int guiMaximum=((OptionInstance.ClampingLazyMaxIntRange)minecraft.options.guiScale().values()).maxInclusive();
            slider(scaleRow,3.0/guiMaximum);
            press(minecraft.gui.screen(),"luminanova.video.apply");
            check(minecraft.options.guiScale().get()==3,"GUI scale applies through native callback");
            phase++;
        } else if (phase == 3) {
            capture(minecraft, "alpha5-scale3.png");
            if (FabricLoader.getInstance().isModLoaded("modmenu")) {
                check(com.terraformersmc.modmenu.ModMenu.hasConfigScreen("luminanova"), "Mod Menu entrypoint");
                check(com.terraformersmc.modmenu.ModMenu.getConfigScreen("luminanova", parent) instanceof NovaVideoSettingsScreen, "Mod Menu factory");
            }
            press(minecraft.gui.screen(),"luminanova.video.quality");
            for (var child:minecraft.gui.screen().children()) if (child instanceof net.minecraft.client.gui.components.EditBox search) {
                search.setValue("nubes");
                check(row(minecraft.gui.screen(),"options.renderClouds").visible,"Search matches quality");
                check(!row(minecraft.gui.screen(),"options.renderDistance").visible,"Search hides unrelated general option");
                search.setValue("");
            }
            phase++;
        } else if (phase==4) {
            capture(minecraft,"alpha5-quality-top.png");
            minecraft.gui.screen().mouseScrolled(minecraft.gui.screen().width-20,60,0,-100);
            phase++;
        } else if (phase==5) {
            capture(minecraft,"alpha5-quality-bottom.png");
            press(minecraft.gui.screen(),"luminanova.video.optimization");
            slider(row(minecraft.gui.screen(),"luminanova.options.block_entity_culling"),1);
            press(minecraft.gui.screen(),"luminanova.video.apply");
            check(io.github.benjamincalledur10.luminanova.config.NovaPerformanceSettings.current().blockEntityCulling(),"Performance option live");
            check(io.github.benjamincalledur10.luminanova.config.NovaPerformanceConfig.load(
                    io.github.benjamincalledur10.luminanova.config.NovaPerformanceSettings.PATH,LoggerFactory.getLogger("performance-smoke")).blockEntityCulling(),"Performance option persists");
            phase++;
        } else if (phase==6) {
            capture(minecraft,"alpha5-optimization.png");
            System.out.println("LUMINA_UI_OK modmenu="+FabricLoader.getInstance().isModLoaded("modmenu"));
            minecraft.stop();phase++;
        }
    }

    private static AbstractWidget row(Screen screen, String key) {
        return screen.children().stream().filter(child -> child instanceof AbstractWidget)
                .map(child -> (AbstractWidget)child)
                .filter(widget -> widget.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable(key).getString()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing row " + key));
    }

    private static void press(Screen screen, String key) {
        var widget = row(screen,key);
        widget.onClick(new MouseButtonEvent(widget.getX()+5,widget.getY()+5,new MouseButtonInfo(0,0)),false);
    }

    private static void capture(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.gameRenderer.mainRenderTarget(), 1,
                message -> System.out.println("UI_SCREENSHOT " + name + " " + message.getString()));
    }

    private static void slider(AbstractWidget widget, double value) {
        int controlWidth=Math.min(140,widget.getWidth()/2);
        widget.onClick(new MouseButtonEvent(widget.getRight()-controlWidth+4+value*(controlWidth-8),
                widget.getY() + 10, new MouseButtonInfo(1, 0)), false);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void verifyIntegratedServerLimits() throws Exception {
        check(net.minecraft.server.level.ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT>52,"Extended ticket priorities fit queue");
        var queue=new net.minecraft.server.level.ChunkTaskPriorityQueue("lumina-priority-test");
        var submit=queue.getClass().getDeclaredMethod("submit",Runnable.class,long.class,int.class);
        var resort=queue.getClass().getDeclaredMethod("resortChunkTasks",int.class,net.minecraft.world.level.ChunkPos.class,int.class);
        submit.setAccessible(true);resort.setAccessible(true);
        submit.invoke(queue,(Runnable)() -> {},0L,40);
        resort.invoke(queue,40,new net.minecraft.world.level.ChunkPos(0,0),52);
        var cap = Arrays.stream(ChunkMap.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("luminanova$localRenderLimit"))
                .findFirst().orElseThrow();
        cap.setAccessible(true);
        check((int) cap.invoke(null, 32) == 50, "Integrated server cap");
        DistanceManager manager = new DistanceManager(new TicketStorage(), Runnable::run, Runnable::run) {
            @Override protected boolean isChunkToRemove(long position) { return false; }
            @Override protected ChunkHolder getChunk(long position) { return null; }
            @Override protected ChunkHolder updateChunkScheduling(long position, int level, ChunkHolder holder, int oldLevel) {
                return null;
            }
        };
        Field trackerField = DistanceManager.class.getDeclaredField("playerTicketManager");
        trackerField.setAccessible(true);
        Object tracker = trackerField.get(manager);
        Field radius = tracker.getClass().getSuperclass().getDeclaredField("maxDistance");
        radius.setAccessible(true);
        check(radius.getInt(tracker) == 50, "Integrated server ticket radius");
    }
}
