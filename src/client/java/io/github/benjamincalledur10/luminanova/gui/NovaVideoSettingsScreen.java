package io.github.benjamincalledur10.luminanova.gui;

import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.Monitor;
import io.github.benjamincalledur10.luminanova.config.NovaConfig;
import io.github.benjamincalledur10.luminanova.config.NovaQualityConfig;
import io.github.benjamincalledur10.luminanova.config.NovaQualitySettings;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;

/** General and Quality pages with staged native options and explicit render-policy controls. */
public final class NovaVideoSettingsScreen extends Screen {
    private static final int CYAN = 0xFF65EAFF;
    private final Screen parent;
    private final Options options;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("luminanova.properties");
    private final OptionInstance<Boolean> ultraOptimization;
    private final Monitor monitor;
    private final OptionInstance<Boolean> linearTexels, fluidCulling, alternativeFluids, enhancedEntities;
    private int buildingPage, activePage, group;
    private String query = "";
    private EditBox search;
    private final String version = FabricLoader.getInstance().getModContainer("luminanova").orElseThrow()
            .getMetadata().getVersion().getFriendlyString();
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
        add("options.guiScale",options.guiScale(),0,((OptionInstance.ClampingLazyMaxIntRange)options.guiScale().values()).maxInclusive(),Integer::valueOf,
                n -> n==0?Component.translatable("options.guiScale.auto"):Component.literal(n+"x"));
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
        buildingPage = 1;
        quality();
        NovaQualityConfig quality = NovaQualitySettings.current();
        linearTexels = policy("luminanova.quality.texel_interpolation", quality.linearTexels(), "nearest", "linear");
        group++;
        fluidCulling = policy("luminanova.quality.fluid_culling", quality.fluidCulling(), "default", "optimized");
        alternativeFluids = policy("luminanova.quality.fluid_shaping", quality.alternativeFluids(), "vanilla", "alternative");
        enhancedEntities = policy("luminanova.quality.entity_sorting", quality.enhancedEntities(), "default", "enhanced");
    }

    private void quality() {
        toggle("options.improvedTransparency", options.improvedTransparency());
        cycle("options.renderClouds", options.cloudStatus(), CloudStatus.values(), CloudStatus::caption);
        add("options.renderCloudsDistance", options.cloudRange(), 2, 128, Integer::valueOf, n -> Component.literal(n + " chunks"));
        add("options.weatherRadius", options.weatherRadius(), 3, 10, Integer::valueOf, n -> Component.literal(n.toString()));
        toggle("options.cutoutLeaves", options.cutoutLeaves());
        cycle("options.particles", options.particles(), ParticleStatus.values(), ParticleStatus::caption);
        toggle("options.ao", options.ambientOcclusion());
        add("options.biomeBlendRadius", options.biomeBlendRadius(), 0, 7, Integer::valueOf,
                n -> Component.literal((n*2+1) + "x" + (n*2+1) + " ").append(Component.translatable("luminanova.quality.blocks")));
        add("options.entityDistanceScaling", options.entityDistanceScaling(), 2, 20, n -> n/4.0,
                n -> Component.literal(n*25 + "%"));
        toggle("options.entityShadows", options.entityShadows());
        toggle("options.vignette", options.vignette());
        add("options.chunkFade", options.chunkSectionFadeInTime(), 0, 40, n -> n/20.0,
                n -> Component.translatable("luminanova.quality.seconds", String.format(java.util.Locale.ROOT,"%.2f",n/20.0)));
        group++;
        add("options.mipmapLevels", options.mipmapLevels(), 0, 4, Integer::valueOf,
                n -> n == 0 ? CommonComponents.OPTION_OFF : Component.literal(n + "x"));
        cycle("options.textureFiltering", options.textureFiltering(), TextureFilteringMethod.values(), TextureFilteringMethod::caption);
        add("options.maxAnisotropy", options.maxAnisotropyBit(), 1, 3, Integer::valueOf, n -> Component.literal((1<<n) + "x"));
    }

    private OptionInstance<Boolean> policy(String key, boolean initial, String off, String on) {
        var option = OptionInstance.createBoolean(key, initial);
        add(key, option, 0, 1, n -> n == 1, n -> Component.translatable("luminanova.quality." + (n == 1 ? on : off)));
        settings.getLast().control = Control.CYCLE;
        return option;
    }

    private <T> void cycle(String key, OptionInstance<T> option, T[] values, Function<T,Component> caption) {
        add(key,option,0,values.length-1,n -> values[n],n -> caption.apply(values[n]));
        settings.getLast().control = Control.CYCLE;
    }

    private void toggle(String key, OptionInstance<Boolean> option) {
        add(key, option, 0, 1, n -> n == 1, n -> n == 1 ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private <T> void add(String key, OptionInstance<T> option, int min, int max, Function<Integer,T> decode, Function<Integer,Component> display) {
        settings.add(new Setting<>(key, option, min, max, decode, display, buildingPage, group));
    }

    @Override protected void init() {
        left = 10; right = width - 10; top = 34; bottom = height - 36;
        sidebar = Math.min(180, Math.max(100, width * 3 / 10));
        search = new EditBox(font,left+8,12,right-left-40,16,Component.translatable("luminanova.video.search"));
        search.setBordered(false);
        search.setHint(Component.translatable("luminanova.video.search"));
        search.setValue(query);
        search.setResponder(value -> { query=value; scroll=0; placeRows(); });
        addRenderableWidget(search);
        addRenderableWidget(new FlatButton(right-24,8,24,22,Component.literal("×"),b -> onClose()));
        var logo = ImageWidget.texture(16,16,Identifier.fromNamespaceAndPath("luminanova","logo-small.png"),28,28);
        logo.setX(left+6); logo.setY(top+6); addRenderableOnly(logo);
        addRenderableWidget(new FlatButton(left,top+30,sidebar-6,24,Component.translatable("luminanova.video.general"),b -> selectPage(0)));
        addRenderableWidget(new FlatButton(left,top+54,sidebar-6,24,Component.translatable("luminanova.video.quality"),b -> selectPage(1)));
        for (var setting : settings) addRenderableWidget(new Row(setting));
        apply = addRenderableWidget(new FlatButton(right-174,height-27,84,22,
                Component.translatable("luminanova.video.apply"),b -> applyChanges()));
        addRenderableWidget(new FlatButton(right-84,height-27,84,22,
                Component.translatable("luminanova.video.accept"),b -> { if (applyChanges()) minecraft.gui.setScreen(parent); }));
        placeRows();
    }

    private void selectPage(int page) {
        activePage=page; query=""; scroll=0; search.setValue(""); placeRows();
    }

    private List<Setting<?>> shownSettings() {
        String normalized = normalize(query);
        return settings.stream().filter(s -> normalized.isEmpty() ? s.page==activePage
                : normalize(Component.translatable(s.key).getString()).contains(normalized)).toList();
    }

    private static String normalize(String text) {
        return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }

    private int contentHeight() {
        var shown=shownSettings();
        int height=0, previous=-1;
        for (var s : shown) { if (previous!=-1 && s.group!=previous) height+=7; height+=24; previous=s.group; }
        return height;
    }

    private Object pending(OptionInstance<?> option) {
        return settings.stream().filter(s -> s.option==option).findFirst().map(s -> (Object)s.pending).orElse(option.get());
    }

    private void placeRows() {
        var shown=shownSettings();
        int y=top, previous=-1;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,contentHeight()-(bottom-top))));
        for (var child : children()) if (child instanceof Row row) {
            row.visible=false;
            if (!shown.contains(row.setting)) continue;
            if (previous!=-1 && row.setting.group!=previous) y+=7;
            previous=row.setting.group;
            row.setX(left+sidebar); row.setY(y-scroll); row.setWidth(right-row.getX()-10); y+=24;
            row.visible=row.getY()>=top && row.getBottom()<=bottom;
            row.active=row.setting.max>row.setting.min;
            if (row.setting.option==options.maxAnisotropyBit()) row.active=pending(options.textureFiltering())==TextureFilteringMethod.ANISOTROPIC;
            if (row.setting.option==options.cloudRange()) row.active=pending(options.cloudStatus())!=CloudStatus.OFF;
        }
        if (apply!=null) apply.active=dirty();
    }

    private boolean dirty() { return settings.stream().anyMatch(Setting::dirty); }

    private boolean applyChanges() {
        var ultra = settings.stream().filter(s -> s.option==ultraOptimization).findFirst().orElseThrow();
        if (ultra.dirty() && !NovaConfig.saveUltraOptimization(configPath, (Boolean)ultra.pending,
                LoggerFactory.getLogger("luminanova"))) {
            error = Component.translatable("luminanova.video.save_error").getString(); return false;
        }
        var quality = new NovaQualityConfig((Boolean)pending(linearTexels),(Boolean)pending(fluidCulling),
                (Boolean)pending(alternativeFluids),(Boolean)pending(enhancedEntities));
        if (!quality.equals(NovaQualitySettings.current()) && !quality.save(NovaQualitySettings.PATH,LoggerFactory.getLogger("luminanova"))) {
            error=Component.translatable("luminanova.video.save_error").getString(); return false;
        }
        int oldMipmaps=options.mipmapLevels().get();
        int oldAnisotropy=options.maxAnisotropyBit().get();
        var oldFiltering=options.textureFiltering().get();
        // Fullscreen can resize/reinitialize the screen; commit the other options first.
        for (var setting : settings) if (setting.option != options.fullscreen()) setting.commit();
        for (var setting : settings) if (setting.option == options.fullscreen()) setting.commit();
        NovaQualitySettings.apply(quality);
        if (oldMipmaps!=options.mipmapLevels().get() || oldAnisotropy!=options.maxAnisotropyBit().get()
                || oldFiltering!=options.textureFiltering().get()) {
            minecraft.updateMaxMipLevel(options.mipmapLevels().get());
            minecraft.delayTextureReload();
        }
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

    private int maxScroll() { return Math.max(0, contentHeight()-(bottom-top)); }

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
            scroll = Math.max(0, Math.min(Math.max(0, contentHeight() - (bottom - top)), scroll - (int)(dy * 28)));
            placeRows(); return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        g.fill(left,8,right,30,0xB8000000);
        g.fill(left,top,left+sidebar-6,bottom,0xC0000000);
        g.fill(left+sidebar,top,right,bottom,0xB0000000);
        g.text(font,"Lumina Nova",left+25,top+10,0xFF80E9CD);
        int selected=top+30+activePage*24;
        g.fill(left,selected,left+3,selected+24,0xFF80E9CD);
        if (maxScroll()>0) {
            int track=bottom-top, handle=Math.max(16,track*track/contentHeight());
            int pos=top+scroll*(track-handle)/maxScroll();
            g.fill(right-7,top,right-1,bottom,0xFF59605E);
            g.fill(right-6,pos,right-2,pos+handle,0xFFACB5B1);
        }
        g.text(font,error.isEmpty()?"v"+version:error,left,height-20,error.isEmpty()?0xFF81948E:0xFFFF7777);
        super.extractRenderState(g,mouseX,mouseY,delta);
        if (shownSettings().isEmpty()) g.text(font,Component.translatable("luminanova.video.no_results"),left+sidebar+8,top+8,0xFF81948E);
    }

    private final class FlatButton extends AbstractWidget {
        private final java.util.function.Consumer<FlatButton> press;
        FlatButton(int x,int y,int width,int height,Component text,java.util.function.Consumer<FlatButton> press) { super(x,y,width,height,text); this.press=press; }
        @Override public void onClick(MouseButtonEvent event, boolean doubleClick) { if (active) press.accept(this); }
        @Override public boolean keyPressed(KeyEvent event) {
            if (active && isFocused() && (event.key()==257 || event.key()==32)) { press.accept(this); return true; }
            return false;
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(getX(),getY(),getRight(),getBottom(),active && isHoveredOrFocused() ? 0x994A5D56 : 0x55000000);
            if (isFocused()) g.outline(getX(),getY(),width,height,CYAN);
            boolean tab=getMessage().getString().equals(Component.translatable("luminanova.video.general").getString())
                    || getMessage().getString().equals(Component.translatable("luminanova.video.quality").getString());
            if (tab) {
                boolean selected=getMessage().getString().equals(Component.translatable(activePage==0?"luminanova.video.general":"luminanova.video.quality").getString());
                g.text(font,getMessage(),getX()+12,getY()+8,selected?0xFF80E9CD:0xFF538578);
                if (selected) g.fill(getX(),getY(),getX()+3,getBottom(),0xFF80E9CD);
            }
            else g.centeredText(font,getMessage(),getX()+width/2,getY()+7,active?0xFFFFFFFF:0xFF718093);
        }
    }

    private enum Control { SLIDER, CHECKBOX, CYCLE }

    private static final class Setting<T> {
        final String key; final OptionInstance<T> option; final int min, max;
        final Function<Integer,T> decode; final Function<Integer,Component> display;
        T pending,saved; int step; final int page,group; Control control;
        Setting(String key, OptionInstance<T> option, int min, int max, Function<Integer,T> decode, Function<Integer,Component> display,int page,int group) {
            this.page=page; this.group=group; this.control=option.get() instanceof Boolean ? Control.CHECKBOX : Control.SLIDER;
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
            super(0, 0, 100, 24, Component.translatable(setting.key), setting.max == setting.min ? 0 :
                    (double)(setting.step-setting.min)/(setting.max-setting.min));
            this.setting=setting;
            active=setting.max > setting.min;
            String tooltip = setting.option==ultraOptimization ? "luminanova.options.ultra.tooltip" : setting.key+".tooltip";
            if (net.minecraft.locale.Language.getInstance().has(tooltip)) setTooltip(Tooltip.create(Component.translatable(tooltip)));
        }
        private int controlWidth() { return Math.min(140,width/2); }
        private void slide(double x) { setValue((x-(getRight()-controlWidth()+4))/(controlWidth()-8)); }
        @Override public void onClick(MouseButtonEvent event,boolean doubleClick) {
            if (!active) return;
            if (setting.control==Control.CHECKBOX) setValue(value<0.5?1:0);
            else if (setting.control==Control.CYCLE) {
                int next=setting.step>=setting.max?setting.min:setting.step+1;
                setValue((double)(next-setting.min)/(setting.max-setting.min));
            } else if (event.x()>=getRight()-controlWidth()) slide(event.x());
        }
        @Override protected void onDrag(MouseButtonEvent event,double dx,double dy) {
            if (active && setting.control==Control.SLIDER) slide(event.x());
        }
        @Override public boolean keyPressed(KeyEvent event) {
            if (isFocused() && active) {
                int direction = event.key() == 263 ? -1 : event.key() == 262 ? 1 : 0;
                if (direction != 0) {
                    setValue(Math.max(0, Math.min(1, (double)(setting.step + direction - setting.min) / (setting.max - setting.min))));
                    return true;
                }
                if (setting.control!=Control.SLIDER && (event.key()==257 || event.key()==32)) {
                    int next=setting.step>=setting.max?setting.min:setting.step+1;
                    setValue((double)(next-setting.min)/(setting.max-setting.min)); return true;
                }
            }
            return super.keyPressed(event);
        }
        @Override protected void updateMessage() {}
        @Override protected void applyValue() { stage(setting); placeRows(); }
        private <T> void stage(Setting<T> s) { s.step=s.min+(int)Math.round(value*(s.max-s.min)); s.pending=s.decode.apply(s.step); }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
            if (isHoveredOrFocused()) g.fill(getX(),getY(),getRight(),getBottom(),0x604D5A58);
            if (isFocused()) g.outline(getX(),getY(),width,height,0xFF80E9CD);
            int color=active?0xFFFFFFFF:0xFF77817E;
            Component label=Component.translatable(setting.key);
            if (setting.option==ultraOptimization) label=label.copy().append(Component.translatable("luminanova.options.ultra.preview"));
            Component shown=setting.option==ultraOptimization ? Component.translatable("luminanova.options.ultra.value",setting.display.apply(setting.step))
                    : setting.display.apply(setting.step);
            int valueWidth=setting.control==Control.CHECKBOX?12:font.width(shown);
            int labelWidth=Math.max(20,width-valueWidth-20);
            g.enableScissor(getX()+6,getY(),getX()+6+labelWidth,getBottom());
            g.text(font,label,getX()+6,getY()+8,color); g.disableScissor();
            if (!active) g.horizontalLine(getX()+6,getX()+6+Math.min(font.width(label),labelWidth),getY()+12,color);
            if (setting.control==Control.CHECKBOX) {
                int x=getRight()-18,y=getY()+6;
                g.outline(x,y,12,12,color);
                if (setting.step==1) g.fill(x+2,y+2,x+10,y+10,0xFF80E9CD);
            } else if (setting.control==Control.SLIDER && active && isHoveredOrFocused() && mx>=getRight()-controlWidth()) {
                int x=getRight()-controlWidth()+4,y=getY()+12;
                g.fill(x,y-1,getRight()-4,y+1,0xFFADB7B3);
                int thumb=x+(int)(value*(controlWidth()-8)); g.fill(thumb-2,y-5,thumb+2,y+5,0xFF80E9CD);
                g.text(font,shown,getRight()-valueWidth-4,getY()+1,0xFFFFFFFF);
            } else g.text(font,shown,getRight()-valueWidth-6,getY()+8,setting.dirty()?0xFF80E9CD:color);
        }
    }
}
