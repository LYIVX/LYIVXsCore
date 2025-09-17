package net.lyivx.ls_core.client.gui.layouts;

import java.util.function.Consumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;

/**
 * Extends SidebarLayout to provide two independent right-side columns.
 */
@Environment(EnvType.CLIENT)
public class SidebarSplitLayout extends SidebarLayout {
    private static final int INNER_GUTTER = 10;       // gap between the two right columns
    private final FrameLayout rightLeftColumnFrame;
    private final FrameLayout rightRightColumnFrame;

    public SidebarSplitLayout(Screen screen) {
        super(screen);
        this.rightLeftColumnFrame = new FrameLayout();
        this.rightRightColumnFrame = new FrameLayout();
    }

    public int getRightColumnWidth() { return Math.max(0, (getRightContentWidth() - INNER_GUTTER) / 2); }
    public int getRightColumnSeparatorX() { return getRightContentX() + getRightColumnWidth() + (INNER_GUTTER / 2); }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        super.visitChildren(visitor);
        this.rightLeftColumnFrame.visitChildren(visitor);
        this.rightRightColumnFrame.visitChildren(visitor);
    }

    @Override
    public void arrangeElements() {
        super.arrangeElements();

        // Right columns
        final int rightX = getRightContentX();
        final int columnWidth = getRightColumnWidth();

        // Left column of the right content
        this.rightLeftColumnFrame.setMinWidth(columnWidth);
        this.rightLeftColumnFrame.setMinHeight(0);
        this.rightLeftColumnFrame.setPosition(rightX, getContentTop());
        this.rightLeftColumnFrame.arrangeElements();

        // Right column of the right content
        int rightColumnX = rightX + columnWidth + INNER_GUTTER;
        this.rightRightColumnFrame.setMinWidth(columnWidth);
        this.rightRightColumnFrame.setMinHeight(0);
        this.rightRightColumnFrame.setPosition(rightColumnX, getContentTop());
        this.rightRightColumnFrame.arrangeElements();
    }

    // Inherit header/footer/sidebar/right single adders from SidebarLayout

    public <T extends LayoutElement> T addToRightLeft(T child) {
        return (T)this.rightLeftColumnFrame.addChild(child);
    }

    public <T extends LayoutElement> T addToRightLeft(T child, Consumer<LayoutSettings> settings) {
        return (T)this.rightLeftColumnFrame.addChild(child, settings);
    }

    public <T extends LayoutElement> T addToRightRight(T child) {
        return (T)this.rightRightColumnFrame.addChild(child);
    }

    public <T extends LayoutElement> T addToRightRight(T child, Consumer<LayoutSettings> settings) {
        return (T)this.rightRightColumnFrame.addChild(child, settings);
    }
}


