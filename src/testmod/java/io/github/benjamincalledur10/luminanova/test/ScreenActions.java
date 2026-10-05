package io.github.benjamincalledur10.luminanova.test;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

final class ScreenActions {
    private ScreenActions() { }
    static void press(Screen screen,String key) {
        AbstractWidget widget=screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(w -> w.getMessage().getString().equals(Component.translatable(key).getString())).findFirst().orElseThrow();
        widget.onClick(new MouseButtonEvent(widget.getRight()-5,widget.getY()+10,new MouseButtonInfo(0,0)),false);
    }
}
