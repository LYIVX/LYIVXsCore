package net.lyivx.ls_core.client.screens.docs;

import net.lyivx.ls_core.docs.DocIconType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class DocIconRenderer {
    private DocIconRenderer() {}

    public static void renderIcon(GuiGraphics guiGraphics, int x, int y, DocIconType type, String icon) {
        if (icon == null || icon.isEmpty()) return;
        try {
            switch (type) {
                case ITEM: {
                    ItemStack stack = itemFromString(icon);
                    if (!stack.isEmpty()) {
                        guiGraphics.renderItem(stack, x, y);
                    }
                    break;
                }
                case BLOCK: {
                    ItemStack stack = blockFromString(icon);
                    if (!stack.isEmpty()) {
                        guiGraphics.renderItem(stack, x, y);
                    }
                    break;
                }
                case TEXTURE: {
                    ResourceLocation texture = ResourceLocation.parse(icon);
                    // Draw a 16x16 icon region at given coords
                    guiGraphics.blit(RenderType::guiTextured, texture, x, y, 0, 0, 16, 16, 16, 16);
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static ItemStack itemFromString(String id) {
        try {
            ResourceLocation loc = ResourceLocation.parse(id);
            Item item = BuiltInRegistries.ITEM.getOptional(loc).orElse(null);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        } catch (Exception ex) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack blockFromString(String id) {
        try {
            ResourceLocation loc = ResourceLocation.parse(id);
            Block block = BuiltInRegistries.BLOCK.getOptional(loc).orElse(null);
            if (block != null) {
                Item item = block.asItem();
                if (item != null) return new ItemStack(item);
            }
            return ItemStack.EMPTY;
        } catch (Exception ex) {
            return ItemStack.EMPTY;
        }
    }
}


