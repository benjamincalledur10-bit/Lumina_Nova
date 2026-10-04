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
            var render = (OptionInstance.IntRange) minecraft.options.renderDistance().values();
            var simulation = (OptionInstance.IntRange) minecraft.options.simulationDistance().values();
            check(render.minInclusive() == 2 && render.maxInclusive() == 50, "Render range");
            check(simulation.minInclusive() == 5 && simulation.maxInclusive() == 32, "Simulation range");
            verifyIntegratedServerLimits();
            minecraft.gui.setScreen(new VideoSettingsScreen(parent, minecraft, minecraft.options));
            check(minecraft.gui.screen() instanceof NovaVideoSettingsScreen, "Video routing");
            check(((OptionsSubScreenAccessor) minecraft.gui.screen()).luminanova$parent() == parent, "Parent screen");
            phase++;
        } else if (phase == 1) {
            var screen = (NovaVideoSettingsScreen) minecraft.gui.screen();
            OptionsList list = ((OptionsSubScreenAccessor) (Object) screen).luminanova$list();
            check(list.findOption(minecraft.options.renderDistance()) != null, "Render widget");
            check(list.findOption(minecraft.options.simulationDistance()) != null, "Simulation widget");
            check(list.findOption(minecraft.options.gamma()) != null, "Brightness widget");
            check(list.findOption(minecraft.options.fullscreen()) != null, "Fullscreen widget");
            check(list.findOption(minecraft.options.enableVsync()) != null, "VSync widget");
            check(list.findOption(minecraft.options.framerateLimit()) != null, "FPS widget");
            capture(minecraft, "general-top.png");
            phase++;
        } else if (phase == 2) {
            var screen = (NovaVideoSettingsScreen) minecraft.gui.screen();
            OptionsList list = ((OptionsSubScreenAccessor) (Object) screen).luminanova$list();
            Field field = NovaVideoSettingsScreen.class.getDeclaredField("ultraOptimization");
            field.setAccessible(true);
            OptionInstance<?> ultra = (OptionInstance<?>) field.get(screen);
            ((CycleButton<?>) list.findOption(ultra)).onPress(new MouseButtonInfo(1, 0));
            check(Boolean.TRUE.equals(ultra.get()), "Ultra preview toggle");
            list.setScrollAmount(list.maxScrollAmount());
            slider(list.findOption(minecraft.options.renderDistance()), 1);
            slider(list.findOption(minecraft.options.simulationDistance()), 1);
            slider(list.findOption(minecraft.options.gamma()), 0.6);
            slider(list.findOption(minecraft.options.framerateLimit()), 12.0 / 25.0);
            phase++;
        } else if (phase == 3) {
            capture(minecraft, "general-ultra.png");
            minecraft.gui.screen().onClose();
            check(minecraft.gui.screen() == parent, "Return navigation");
            Path config = FabricLoader.getInstance().getConfigDir().resolve("luminanova.properties");
            check(NovaConfig.load(config, LoggerFactory.getLogger("ui-smoke")).ultraOptimization(), "Ultra saved");
            String options = Files.readString(minecraft.gameDirectory.toPath().resolve("options.txt"));
            check(options.contains("renderDistance:50"), "Render saved");
            check(options.contains("simulationDistance:32"), "Simulation saved");
            minecraft.options.renderDistance().set(12);
            minecraft.options.simulationDistance().set(12);
            minecraft.options.load();
            check(minecraft.options.renderDistance().get() == 50, "Render reload");
            check(minecraft.options.simulationDistance().get() == 32, "Simulation reload");
            minecraft.gui.setScreen(new NovaVideoSettingsScreen(parent, minecraft, minecraft.options));
            Field field = NovaVideoSettingsScreen.class.getDeclaredField("ultraOptimization");
            field.setAccessible(true);
            check(Boolean.TRUE.equals(((OptionInstance<?>) field.get(minecraft.gui.screen())).get()), "Ultra reopens");
            if (FabricLoader.getInstance().isModLoaded("modmenu")) {
                check(com.terraformersmc.modmenu.ModMenu.ROOT_MODS.containsKey("luminanova"), "Mod Menu listing");
                check(com.terraformersmc.modmenu.ModMenu.hasConfigScreen("luminanova"), "Mod Menu entrypoint");
                check(com.terraformersmc.modmenu.ModMenu.getConfigScreen("luminanova", parent) instanceof NovaVideoSettingsScreen,
                        "Mod Menu configuration factory");
                minecraft.gui.setScreen(com.terraformersmc.modmenu.api.ModMenuApi.createModsScreen(parent));
                for (var child : minecraft.gui.screen().children()) {
                    if (child instanceof net.minecraft.client.gui.components.EditBox search) search.setValue("Lumina Nova");
                }
            } else {
                minecraft.options.guiScale().set(3);
                minecraft.resizeGui();
            }
            phase++;
        } else if (phase == 4) {
            capture(minecraft, FabricLoader.getInstance().isModLoaded("modmenu") ? "mod-menu.png" : "general-scale3.png");
            phase++;
        } else if (phase == 5) {
            System.out.println("LUMINA_UI_OK modmenu=" + FabricLoader.getInstance().isModLoaded("modmenu"));
            minecraft.stop();
            phase++;
        }
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
