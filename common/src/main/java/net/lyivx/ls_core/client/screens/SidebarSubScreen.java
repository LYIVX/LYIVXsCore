package net.lyivx.ls_core.client.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.lyivx.ls_core.client.gui.layouts.SidebarLayout;

/**
 * Base screen that mirrors Mojang's OptionsSubScreen flow, but uses
 * {@link SidebarSplitLayout} and only requires implementors to provide
 * {@link #addContents()} (no addOptions step).
 */
@Environment(EnvType.CLIENT)
public abstract class SidebarSubScreen extends Screen {
    protected final Screen lastScreen;
    protected final Options options;
    public final SidebarLayout layout = new SidebarLayout(this);

    private int navScroll = 0;
    private int navContentHeight = 0;

    public SidebarSubScreen(Screen lastScreen, Options options, Component title) {
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

        // Update button visibility based on viewport bounds
        int navTop = this.layout.getHeaderHeight() + 6;
        int navBottom = (this.height - this.layout.getFooterHeight()) - 6;

        // Draw sidebar separator
        guiGraphics.fill(this.layout.getSidebarWidth() - 1, 33, this.layout.getSidebarWidth(), this.height - 33, 0x75000000);

        // Draw scrollbar if needed
        int navViewport = Math.max(0, navBottom - navTop);
        if (navContentHeight > navViewport) {
            int trackX1 = this.layout.getSidebarWidth() - 4;
            int trackX2 = this.layout.getSidebarWidth() - 2;
            guiGraphics.fill(trackX1, navTop, trackX2, navBottom, 0x33000000);
            float ratio = (float)navViewport / (float)navContentHeight;
            int thumbH = Math.max(12, (int)(navViewport * ratio));
            int maxScroll = Math.max(0, navContentHeight - navViewport);
            int thumbY = navTop + (maxScroll == 0 ? 0 : (int)((float)navScroll / (float)maxScroll * (navViewport - thumbH)));
            guiGraphics.fill(trackX1, thumbY, trackX2, thumbY + thumbH, 0x99000000);
        }

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


