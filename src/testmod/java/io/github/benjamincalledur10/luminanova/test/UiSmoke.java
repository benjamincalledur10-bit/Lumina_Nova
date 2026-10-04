package io.github.benjamincalledur10.luminanova.test;

import io.github.benjamincalledur10.luminanova.config.NovaConfig;
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
            capture(minecraft, "alpha3-general.png");
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
            capture(minecraft, "alpha3-ultra.png");
            press(minecraft.gui.screen(), "luminanova.video.accept");
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
            minecraft.gui.setScreen(new NovaVideoSettingsScreen(parent, minecraft, minecraft.options));
            slider(row(minecraft.gui.screen(), "options.renderDistance"), 0);
            minecraft.gui.screen().onClose();
            check(minecraft.options.renderDistance().get() == 50, "Escape discards pending edits");
            minecraft.gui.setScreen(new NovaVideoSettingsScreen(parent, minecraft, minecraft.options));
            minecraft.options.guiScale().set(3); minecraft.resizeGui();
            phase++;
        } else if (phase == 3) {
            capture(minecraft, "alpha3-scale3.png");
            if (FabricLoader.getInstance().isModLoaded("modmenu")) {
                check(com.terraformersmc.modmenu.ModMenu.hasConfigScreen("luminanova"), "Mod Menu entrypoint");
                check(com.terraformersmc.modmenu.ModMenu.getConfigScreen("luminanova", parent) instanceof NovaVideoSettingsScreen, "Mod Menu factory");
            }
            System.out.println("LUMINA_UI_OK modmenu=" + FabricLoader.getInstance().isModLoaded("modmenu"));
            minecraft.stop(); phase++;
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
        widget.onClick(new MouseButtonEvent(widget.getX() + 4 + value * (widget.getWidth() - 8),
                widget.getY() + 10, new MouseButtonInfo(1, 0)), false);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void verifyIntegratedServerLimits() throws Exception {
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
