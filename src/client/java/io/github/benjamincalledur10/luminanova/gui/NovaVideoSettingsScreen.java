package io.github.benjamincalledur10.luminanova.gui;

import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import io.github.benjamincalledur10.luminanova.config.NovaConfig;
import java.nio.file.Path;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** General settings backed by the game's own options and display-mode implementation. */
public final class NovaVideoSettingsScreen extends VideoSettingsScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger("luminanova");
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("luminanova", "logo-small.png");
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("luminanova.properties");
    private final OptionInstance<Boolean> ultraOptimization;
    private boolean savedUltra;
    private Monitor monitor;
    private StringWidget status;

    public NovaVideoSettingsScreen(Screen parent, Minecraft minecraft, Options options) {
        super(parent, minecraft, options);
        savedUltra = NovaConfig.load(configPath, LOGGER).ultraOptimization();
        ultraOptimization = OptionInstance.createBoolean("luminanova.options.ultra",
                OptionInstance.cachedConstantTooltip(Component.translatable("luminanova.options.ultra.tooltip")),
                (caption, value) -> Component.translatable("luminanova.options.ultra.value",
                        value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF),
                savedUltra, value -> {});
    }

    @Override
    public Component getTitle() {
        return Component.translatable("luminanova.video.title");
    }

    @Override
    protected void addTitle() {
        layout.setHeaderHeight(76);
        LinearLayout header = LinearLayout.vertical().spacing(5);
        header.defaultCellSetting().alignHorizontallyCenter();
        LinearLayout brand = LinearLayout.horizontal().spacing(8);
        brand.defaultCellSetting().alignVerticallyMiddle();
        brand.addChild(ImageWidget.texture(28, 28, LOGO, 28, 28));
        brand.addChild(new StringWidget(Component.literal("Lumina Nova").withColor(0x65EAFF), font));
        header.addChild(brand);
        Button general = Button.builder(Component.translatable("luminanova.video.general").withColor(0x65EAFF), button -> {})
                .width(120).build();
        general.active = false;
        header.addChild(general);
        layout.addToHeader(header);
    }

    @Override
    protected void addOptions() {
        list.addSmall(options.renderDistance(), options.simulationDistance());
        list.addSmall(options.gamma(), options.fullscreen());
        list.addSmall(options.enableVsync(), options.framerateLimit());
        list.addBig(options.exclusiveFullscreen());
        list.addBig(fullscreenResolution());
        if (monitor == null) list.findOption(resolutionOption).active = false;
        if (MacosUtil.IS_MACOS) {
            list.addBig(options.macFullscreenMenuVisibility());
        }
        list.addHeader(Component.translatable("luminanova.video.optimization").withStyle(ChatFormatting.BOLD));
        list.addBig(ultraOptimization);
        list.addHeader(Component.translatable("luminanova.options.ultra.pending").withColor(0xE8BD70));
    }

    private OptionInstance<Integer> resolutionOption;

    private OptionInstance<Integer> fullscreenResolution() {
        Window window = minecraft.getWindow();
        monitor = window.findBestMonitor();
        int selected = monitor == null ? -1 : window.getPreferredFullscreenVideoMode()
                .map(monitor::indexOfMode).orElse(-1);
        resolutionOption = new OptionInstance<>("luminanova.options.fullscreen_resolution",
                OptionInstance.cachedConstantTooltip(Component.translatable("options.fullscreen.exclusive.mode.tooltip")),
                (caption, value) -> {
                    if (monitor == null) return Component.translatable("options.fullscreen.unavailable");
                    if (value == -1) return Options.genericValueLabel(caption,
                            Component.translatable("options.fullscreen.current"));
                    VideoMode mode = monitor.mode(value);
                    return Options.genericValueLabel(caption, Component.translatable("options.fullscreen.entry",
                            mode.getWidth(), mode.getHeight(), mode.refreshRateLabel(),
                            mode.getRedBits() + mode.getGreenBits() + mode.getBlueBits()));
                }, new OptionInstance.IntRange(-1, monitor == null ? -1 : monitor.modeCount() - 1), selected,
                value -> {
                    if (monitor != null) window.setPreferredFullscreenVideoMode(value == -1
                            ? Optional.empty() : Optional.of(monitor.mode(value)));
                });
        return resolutionOption;
    }

    @Override
    protected void addFooter() {
        layout.setFooterHeight(42);
        LinearLayout footer = LinearLayout.vertical().spacing(4);
        footer.defaultCellSetting().alignHorizontallyCenter();
        status = new StringWidget(Component.translatable("luminanova.video.alpha").withColor(0x9DA9B8), font);
        footer.addChild(status);
        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.addToFooter(footer);
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) list.updateSize(width, layout);
    }

    @Override
    public void tick() {
        if (minecraft.getWindow().findBestMonitor() != monitor) {
            if (list != null) list.applyUnsavedChanges();
            layout.removeChildren();
            rebuildWidgets();
        }
        updateFullscreenButton(options.fullscreen().get());
    }

    @Override
    public void onClose() {
        if (ultraOptimization.get() != savedUltra) {
            if (!NovaConfig.saveUltraOptimization(configPath, ultraOptimization.get(), LOGGER)) {
                status.setMessage(Component.translatable("luminanova.video.save_error").withColor(0xFF7777));
                return;
            }
            savedUltra = ultraOptimization.get();
        }
        super.onClose();
    }
}
