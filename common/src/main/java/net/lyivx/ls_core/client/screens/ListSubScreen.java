package net.lyivx.ls_core.client.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.lyivx.ls_core.client.gui.layouts.SidebarLayout;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Base screen that mirrors Mojang's OptionsSubScreen flow, but uses
 * {@link SidebarSplitLayout} and only requires implementors to provide
 * {@link #addContents()} (no addOptions step).
 */
@Environment(EnvType.CLIENT)
public abstract class ListSubScreen extends Screen {
    protected final Screen lastScreen;
    protected final Options options;
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public ListSubScreen(Screen lastScreen, Options options, Component title) {
        super(title);
        this.lastScreen = lastScreen;
        this.options = options;
    }

    @Override
    protected void init() {
        this.addTitle();
        this.addContents();
        this.addFooter();
        // Add any widgets contributed via layout frames
        this.layout.visitChildren((LayoutElement el) -> {
            if (el instanceof AbstractWidget w) {
                this.addRenderableWidget(w);
            }
        });
        this.repositionElements();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blit(RenderType::guiTexturedOverlay, MENU_BACKGROUND, 0, this.layout.getHeaderHeight(), 0, 0, this.layout.getWidth(), this.layout.getContentHeight(), 16, 16);
        guiGraphics.blit(RenderType::guiTexturedOverlay, MENU_BACKGROUND, 0, this.layout.getHeaderHeight(), 0, 0, this.layout.getWidth(), this.layout.getContentHeight(), 16, 16);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.blit(RenderType::guiTextured, INWORLD_HEADER_SEPARATOR, 0, this.layout.getHeaderHeight() - 2, 0, 0, this.layout.getWidth(), 2, 32, 2);
        guiGraphics.blit(RenderType::guiTextured, INWORLD_FOOTER_SEPARATOR, 0, this.layout.getHeight() - this.layout.getFooterHeight(), 0, 0, this.layout.getWidth(), 2, 32, 2);
    }

    protected void addTitle() {
        this.layout.addTitleHeader(this.title, this.font);
    }

    protected abstract void addContents();

    protected void addFooter() {
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, (button) -> this.onClose()).width(200).build());
    }

    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void removed() {
        this.minecraft.options.save();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}


