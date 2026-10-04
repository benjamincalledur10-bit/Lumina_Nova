package io.github.benjamincalledur10.luminanova.gui;

import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.Monitor;
import io.github.benjamincalledur10.luminanova.config.NovaConfig;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;

/** A staged General panel; Apply invokes the real Minecraft option callbacks. */
public final class NovaVideoSettingsScreen extends Screen {
    private static final int CYAN = 0xFF65EAFF;
    private final Screen parent;
    private final Options options;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("luminanova.properties");
    private final OptionInstance<Boolean> ultraOptimization;
    private final Monitor monitor;
    private FlatButton apply;
    private int left, right, top, bottom, sidebar, scroll;
    private String error = "";
    private boolean draggingScrollbar;

    public NovaVideoSettingsScreen(Screen parent, Minecraft minecraft, Options options) {
        super(Component.translatable("luminanova.video.title"));
        this.parent = parent;
        this.options = options;
        add("options.renderDistance", options.renderDistance(), 2, 50, Integer::valueOf,
                n -> Component.literal(n + " chunks"));
        add("options.simulationDistance", options.simulationDistance(), 5, 32, Integer::valueOf,
                n -> Component.literal(n + " chunks"));
        add("options.gamma", options.gamma(), 0, 100, n -> n / 100.0,
                n -> Component.literal(n + "%"));
        toggle("options.fullscreen", options.fullscreen());
        monitor = minecraft.getWindow().findBestMonitor();
        var window = minecraft.getWindow();
        int selected = monitor == null ? -1 : window.getPreferredFullscreenVideoMode().map(monitor::indexOfMode).orElse(-1);
        var resolution = new OptionInstance<Integer>("luminanova.options.fullscreen_resolution", OptionInstance.noTooltip(),
                (caption, value) -> Component.literal(""),
                new OptionInstance.IntRange(-1, monitor == null ? -1 : monitor.modeCount() - 1), selected,
                value -> { if (monitor != null) window.setPreferredFullscreenVideoMode(value == -1 ? Optional.empty() : Optional.of(monitor.mode(value))); });
        add("luminanova.options.fullscreen_resolution", resolution, -1, monitor == null ? -1 : monitor.modeCount() - 1,
                Integer::valueOf, n -> n == -1 ? Component.translatable("options.fullscreen.current") :
                        Component.literal(monitor.mode(n).getWidth() + "x" + monitor.mode(n).getHeight() + " @ " + monitor.mode(n).refreshRateLabel()));
        toggle("options.vsync", options.enableVsync());
        add("options.framerateLimit", options.framerateLimit(), 1, 26, n -> n * 10,
                n -> n == 26 ? Component.translatable("options.framerateLimit.max") : Component.literal(n * 10 + " FPS"));
        toggle("options.exclusiveFullscreen", options.exclusiveFullscreen());
        if (MacosUtil.IS_MACOS) toggle("options.macFullscreenMenuVisibility", options.macFullscreenMenuVisibility());
        ultraOptimization = OptionInstance.createBoolean("luminanova.options.ultra",
                NovaConfig.load(configPath, LoggerFactory.getLogger("luminanova")).ultraOptimization());
        toggle("luminanova.options.ultra", ultraOptimization);
    }

    private void toggle(String key, OptionInstance<Boolean> option) {
        add(key, option, 0, 1, n -> n == 1, n -> n == 1 ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private <T> void add(String key, OptionInstance<T> option, int min, int max, Function<Integer,T> decode, Function<Integer,Component> display) {
        settings.add(new Setting<>(key, option, min, max, decode, display));
    }

    @Override protected void init() {
        left = 12; right = width - 12; top = 43; bottom = height - 48;
        sidebar = Math.min(210, Math.max(104, width / 3));
        var logo = ImageWidget.texture(20, 20, Identifier.fromNamespaceAndPath("luminanova", "logo-small.png"), 28, 28);
        logo.setX(left + 8); logo.setY(top + 8); addRenderableOnly(logo);
        for (var setting : settings) addRenderableWidget(new Row(setting));
        apply = addRenderableWidget(new FlatButton(right - 208, height - 34,
                Component.translatable("luminanova.video.apply"), b -> applyChanges()));
        addRenderableWidget(new FlatButton(right - 100, height - 34,
                Component.translatable("luminanova.video.accept"), b -> { if (applyChanges()) minecraft.gui.setScreen(parent); }));
        placeRows();
    }

    private void placeRows() {
        int index = 0;
        for (var child : children()) if (child instanceof Row row) {
            row.setX(left + sidebar + 8); row.setY(top + index++ * 28 - scroll);
            row.setWidth(right - row.getX() - 12);
            row.visible = row.getY() >= top && row.getBottom() <= bottom;
        }
        if (apply != null) apply.active = dirty();
    }

    private boolean dirty() { return settings.stream().anyMatch(Setting::dirty); }

    private boolean applyChanges() {
        var ultra = settings.getLast();
        if (ultra.dirty() && !NovaConfig.saveUltraOptimization(configPath, (Boolean)ultra.pending,
                LoggerFactory.getLogger("luminanova"))) {
            error = Component.translatable("luminanova.video.save_error").getString(); return false;
        }
        // Fullscreen can resize/reinitialize the screen; commit the other options first.
        for (var setting : settings) if (setting.option != options.fullscreen()) setting.commit();
        for (var setting : settings) if (setting.option == options.fullscreen()) setting.commit();
        minecraft.getWindow().changeFullscreenVideoMode();
        options.save();
        error = ""; placeRows(); return true;
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == 266 || event.key() == 267) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll + (event.key() == 267 ? 1 : -1) * 84));
            placeRows(); return true;
        }
        return super.keyPressed(event);
    }

    private int maxScroll() { return Math.max(0, settings.size()*28-(bottom-top)); }

    private void scrollTo(double y) {
        scroll = (int)Math.round(Math.max(0,Math.min(1,(y-top)/(bottom-top))) * maxScroll());
        placeRows();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button()==0 && maxScroll()>0 && event.x()>=right-8 && event.x()<=right
                && event.y()>=top && event.y()<=bottom) {
            draggingScrollbar=true; scrollTo(event.y()); return true;
        }
        return super.mouseClicked(event,doubleClick);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
        if (draggingScrollbar) { scrollTo(event.y()); return true; }
        return super.mouseDragged(event,dx,dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar) { draggingScrollbar=false; return true; }
        return super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (x >= left + sidebar && x <= right && y >= top && y <= bottom) {
            scroll = Math.max(0, Math.min(Math.max(0, settings.size() * 28 - (bottom - top)), scroll - (int)(dy * 28)));
            placeRows(); return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(left, 8, right, 35, 0xD008101D);
        g.text(font, "Lumina Nova", left + 10, 17, CYAN);
        g.text(font, "v0.0.3-alpha", right - font.width("v0.0.3-alpha") - 10, 17, 0xFF94A5BA);
        g.fill(left, top, left + sidebar - 5, bottom, 0xD008101D);
        g.fill(left + sidebar, top, right, bottom, 0xB808101D);
        g.text(font, "Lumina Nova", left + 32, top + 14, CYAN);
        g.fill(left, top + 38, left + sidebar - 5, top + 65, 0x443AB9CF);
        g.fill(left, top + 38, left + 3, top + 65, CYAN);
        g.text(font, Component.translatable("luminanova.video.general"), left + 12, top + 47, CYAN);
        if (settings.size() * 28 > bottom - top) {
            int track = bottom - top, handle = Math.max(16, track * track / (settings.size() * 28));
            int pos = top + scroll * (track - handle) / Math.max(1, settings.size() * 28 - track);
            g.fill(right - 6, top, right - 2, bottom, 0x55405060);
            g.fill(right - 6, pos, right - 2, pos + handle, 0xFF91A6B8);
        }
        if (!error.isEmpty()) g.text(font, error, left, height - 44, 0xFFFF7777);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private final class FlatButton extends AbstractWidget {
        private final java.util.function.Consumer<FlatButton> press;
        FlatButton(int x, int y, Component text, java.util.function.Consumer<FlatButton> press) { super(x,y,100,22,text); this.press=press; }
        @Override public void onClick(MouseButtonEvent event, boolean doubleClick) { if (active) press.accept(this); }
        @Override public boolean keyPressed(KeyEvent event) {
            if (active && isFocused() && (event.key()==257 || event.key()==32)) { press.accept(this); return true; }
            return false;
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(getX(),getY(),getRight(),getBottom(),active && isHoveredOrFocused() ? 0xE03D657C : 0xD0182334);
            if (isFocused()) g.outline(getX(),getY(),width,height,CYAN);
            g.centeredText(font,getMessage(),getX()+width/2,getY()+7,active ? 0xFFF1F5FA : 0xFF718093);
        }
    }

    private static final class Setting<T> {
        final String key; final OptionInstance<T> option; final int min, max;
        final Function<Integer,T> decode; final Function<Integer,Component> display;
        T pending, saved; int step;
        Setting(String key, OptionInstance<T> option, int min, int max, Function<Integer,T> decode, Function<Integer,Component> display) {
            this.key=key; this.option=option; this.min=min; this.max=max; this.decode=decode; this.display=display;
            pending=saved=option.get(); step=min;
            double nearest = Double.POSITIVE_INFINITY;
            for (int i=min; i<=max; i++) {
                T candidate = decode.apply(i);
                if (Objects.equals(candidate, pending)) { step=i; break; }
                if (candidate instanceof Number a && pending instanceof Number b) {
                    double distance = Math.abs(a.doubleValue()-b.doubleValue());
                    if (distance < nearest) { nearest=distance; step=i; }
                }
            }
        }
        boolean dirty() { return !Objects.equals(pending, saved); }
        void commit() { option.set(pending); saved=pending; }
    }

    private final class Row extends AbstractSliderButton {
        private final Setting<?> setting;
        Row(Setting<?> setting) {
            super(0, 0, 100, 26, Component.translatable(setting.key), setting.max == setting.min ? 0 :
                    (double)(setting.step-setting.min)/(setting.max-setting.min));
            this.setting=setting;
            active=setting.max > setting.min;
            if (setting.option == ultraOptimization) setTooltip(Tooltip.create(Component.translatable("luminanova.options.ultra.tooltip")));
        }
        @Override public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (setting.pending instanceof Boolean) setValue(value < 0.5 ? 1 : 0);
            else super.onClick(event, doubleClick);
        }
        @Override public boolean keyPressed(KeyEvent event) {
            if (isFocused() && active) {
                int direction = event.key() == 263 ? -1 : event.key() == 262 ? 1 : 0;
                if (direction != 0) {
                    setValue(Math.max(0, Math.min(1, (double)(setting.step + direction - setting.min) / (setting.max - setting.min))));
                    return true;
                }
                if (setting.pending instanceof Boolean && (event.key() == 257 || event.key() == 32)) {
                    setValue(value < 0.5 ? 1 : 0); return true;
                }
            }
            return super.keyPressed(event);
        }
        @Override protected void updateMessage() {}
        @Override protected void applyValue() { stage(setting); placeRows(); }
        private <T> void stage(Setting<T> s) { s.step=s.min+(int)Math.round(value*(s.max-s.min)); s.pending=s.decode.apply(s.step); }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (isHoveredOrFocused()) g.fill(getX(), getY(), getRight(), getBottom(), 0x553D657C);
            if (isFocused()) g.outline(getX(), getY(), width, height, CYAN);
            Component label=Component.translatable(setting.key);
            Component shown=setting.option == ultraOptimization ? Component.translatable("luminanova.options.ultra.value", setting.display.apply(setting.step)) : setting.display.apply(setting.step);
            int valueWidth=font.width(shown);
            int labelWidth=Math.max(20, width-valueWidth-22);
            g.enableScissor(getX()+6,getY(),getX()+6+labelWidth,getBottom());
            g.text(font,label,getX()+6,getY()+8,0xFFE4EAF2); g.disableScissor();
            g.text(font,shown,getRight()-valueWidth-8,getY()+8,setting.dirty()?CYAN:0xFFE4EAF2);
            if (!(setting.pending instanceof Boolean) && isHoveredOrFocused()) {
                g.fill(getX()+4,getBottom()-3,getRight()-4,getBottom()-2,0xFF425367);
                int thumb=getX()+4+(int)(value*(width-8)); g.fill(thumb-2,getBottom()-5,thumb+2,getBottom(),CYAN);
            }
        }
    }
}
