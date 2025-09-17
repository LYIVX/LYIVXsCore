package net.lyivx.ls_core.client.screens.docs.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.lyivx.ls_core.client.screens.docs.DocIconRenderer;
import net.lyivx.ls_core.docs.DocIconType;

/**
 * A modern list button with a 16x16 icon slot on the left and label text.
 * Draws a simple translucent background with a subtle outline (no vanilla texture),
 * so it always appears above list backgrounds and remains clickable.
 */
public class IconListButton extends Button {
    private final DocIconType iconType;
    private final String iconId;

    public IconListButton(int x, int y, int width, int height,
                          Component label,
                          DocIconType iconType,
                          String iconId,
                          OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.iconType = iconType;
        this.iconId = iconId;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // Background
        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = x1 + this.getWidth();
        int y2 = y1 + this.getHeight();
        int bg = this.isActive() ? 0x33000000 : 0x22000000; // slightly lighter when active
        // Hover highlight
        if (this.isHoveredOrFocused()) {
            bg = 0x44000000;
        }
        gg.fill(x1, y1, x2, y2, bg);
        // Outline
        gg.fill(x1, y1, x2, y1 + 1, 0x66FFFFFF); // top
        gg.fill(x1, y2 - 1, x2, y2, 0x66000000); // bottom
        gg.fill(x1, y1, x1 + 1, y2, 0x66FFFFFF); // left
        gg.fill(x2 - 1, y1, x2, y2, 0x66000000); // right

        // Icon slot (16x16) at left with its own subtle outline
        int slotX = x1 + 2;
        int slotY = y1 + Math.max(2, (this.getHeight() - 16) / 2);
        drawSlot(gg, slotX, slotY);
        if (iconType != null && iconId != null && !iconId.isEmpty()) {
            DocIconRenderer.renderIcon(gg, slotX, slotY, iconType, iconId);
        }

        // Label text
        int textX = slotX + 18 + 4; // slot + padding
        
        var font = Minecraft.getInstance().font;
        int textY = y1 + (this.getHeight() - font.lineHeight) / 2;
        
        // Calculate available width for text (button width minus left margin and right padding)
        int availableWidth = this.getWidth() - (textX - x1) - 4; // 4px right padding
        
        // Truncate text if it's too long to fit within the button
        if (font.width(this.getMessage().getString()) > availableWidth) {
            String displayText = font.plainSubstrByWidth(this.getMessage().getString(), availableWidth - 6) + "...";
            gg.drawString(font, displayText, textX, textY + 2, 0xFFFFFF);
        }
        else {
            gg.drawString(font, this.getMessage().getString(), textX, textY + 2, 0xFFFFFF);
        }
    }

    private void drawSlot(GuiGraphics gg, int x, int y) {
        int x1 = x - 1, y1 = y - 1, x2 = x + 17, y2 = y + 17;
        gg.fill(x1, y1, x2, y2, 0x22000000);
        gg.fill(x1, y1, x2, y1 + 1, 0x55FFFFFF);
        gg.fill(x1, y2 - 1, x2, y2, 0x55000000);
        gg.fill(x1, y1, x1 + 1, y2, 0x55FFFFFF);
        gg.fill(x2 - 1, y1, x2, y2, 0x55000000);
    }
}
