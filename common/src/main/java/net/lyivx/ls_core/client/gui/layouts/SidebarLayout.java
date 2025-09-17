package net.lyivx.ls_core.client.gui.layouts;

import java.util.function.Consumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Header + footer + left sidebar + single right content column.
 */
@Environment(EnvType.CLIENT)
public class SidebarLayout implements Layout {
    public static final int DEFAULT_HEADER_AND_FOOTER_HEIGHT = 33;
    public static final int DEFAULT_SIDEBAR_WIDTH = 155;

    protected static final int CONTENT_MARGIN_TOP = 30; // match vanilla spacing
    protected static final int OUTER_GUTTER = 10;        // gap between sidebar and right content

    protected final FrameLayout headerFrame;
    protected final FrameLayout footerFrame;
    protected final FrameLayout leftSidebarFrame;
    protected final FrameLayout rightContentFrame;

    protected final Screen screen;

    protected int headerHeight;
    protected int footerHeight;
    protected int sidebarWidth;

    public SidebarLayout(Screen screen) {
        this(screen, DEFAULT_HEADER_AND_FOOTER_HEIGHT, DEFAULT_HEADER_AND_FOOTER_HEIGHT, DEFAULT_SIDEBAR_WIDTH);
    }

    public SidebarLayout(Screen screen, int headerHeight, int footerHeight, int sidebarWidth) {
        this.headerFrame = new FrameLayout();
        this.footerFrame = new FrameLayout();
        this.leftSidebarFrame = new FrameLayout();
        this.rightContentFrame = new FrameLayout();

        this.screen = screen;
        this.headerHeight = headerHeight;
        this.footerHeight = footerHeight;
        this.sidebarWidth = sidebarWidth;

        this.headerFrame.defaultChildLayoutSetting().align(0.5F, 0.5F);
        this.footerFrame.defaultChildLayoutSetting().align(0.5F, 0.5F);
    }

    @Override
    public void setX(int x) {}

    @Override
    public void setY(int y) {}

    @Override
    public int getX() { return 0; }

    @Override
    public int getY() { return 0; }

    @Override
    public int getWidth() { return this.screen.width; }

    @Override
    public int getHeight() { return this.screen.height; }

    public int getHeaderHeight() { return this.headerHeight; }
    public void setHeaderHeight(int headerHeight) { this.headerHeight = headerHeight; }

    public int getFooterHeight() { return this.footerHeight; }
    public void setFooterHeight(int footerHeight) { this.footerHeight = footerHeight; }

    public int getSidebarWidth() { return this.sidebarWidth; }
    public void setSidebarWidth(int sidebarWidth) { this.sidebarWidth = sidebarWidth; }

    public int getContentHeight() { return this.screen.height - this.headerHeight - this.footerHeight; }

    // Convenience bounds for right content area
    public int getRightContentX() { return this.sidebarWidth + OUTER_GUTTER; }
    public int getRightContentWidth() { return Math.max(0, this.screen.width - this.sidebarWidth - OUTER_GUTTER * 2); }
    public int getContentTop() { return this.headerHeight + CONTENT_MARGIN_TOP; }
    public int getFooterTop() { return this.screen.height - this.footerHeight; }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        this.headerFrame.visitChildren(visitor);
        this.leftSidebarFrame.visitChildren(visitor);
        this.rightContentFrame.visitChildren(visitor);
        this.footerFrame.visitChildren(visitor);
    }

    @Override
    public void arrangeElements() {
        final int headerTop = 0;
        final int footerTop = getFooterTop();

        // Header
        this.headerFrame.setMinWidth(this.screen.width);
        this.headerFrame.setMinHeight(this.headerHeight);
        this.headerFrame.setPosition(0, headerTop);
        this.headerFrame.arrangeElements();

        // Footer
        this.footerFrame.setMinWidth(this.screen.width);
        this.footerFrame.setMinHeight(this.footerHeight);
        this.footerFrame.setPosition(0, footerTop);
        this.footerFrame.arrangeElements();

        // Content vertical bounds
        final int contentTop = getContentTop();
        final int contentHeight = Math.max(0, footerTop - contentTop);

        // Sidebar
        this.leftSidebarFrame.setMinWidth(this.sidebarWidth);
        this.leftSidebarFrame.setMinHeight(contentHeight);
        this.leftSidebarFrame.setPosition(0, contentTop);
        this.leftSidebarFrame.arrangeElements();

        // Right content
        final int rightX = getRightContentX();
        final int rightWidth = getRightContentWidth();
        this.rightContentFrame.setMinWidth(rightWidth);
        this.rightContentFrame.setMinHeight(0);
        this.rightContentFrame.setPosition(rightX, contentTop);
        this.rightContentFrame.arrangeElements();
    }

    // Adders
    public <T extends LayoutElement> T addToHeader(T child) { return (T)this.headerFrame.addChild(child); }
    public <T extends LayoutElement> T addToHeader(T child, Consumer<LayoutSettings> settings) { return (T)this.headerFrame.addChild(child, settings); }
    public void addTitleHeader(Component message, Font font) { this.headerFrame.addChild(new StringWidget(message, font)); }
    public <T extends LayoutElement> T addToFooter(T child) { return (T)this.footerFrame.addChild(child); }
    public <T extends LayoutElement> T addToFooter(T child, Consumer<LayoutSettings> settings) { return (T)this.footerFrame.addChild(child, settings); }
    public <T extends LayoutElement> T addToLeftSidebar(T child) { return (T)this.leftSidebarFrame.addChild(child); }
    public <T extends LayoutElement> T addToLeftSidebar(T child, Consumer<LayoutSettings> settings) { return (T)this.leftSidebarFrame.addChild(child, settings); }
    public <T extends LayoutElement> T addToRight(T child) { return (T)this.rightContentFrame.addChild(child); }
    public <T extends LayoutElement> T addToRight(T child, Consumer<LayoutSettings> settings) { return (T)this.rightContentFrame.addChild(child, settings); }
}


