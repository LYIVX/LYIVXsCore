package net.lyivx.ls_core.client.screens.docs.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.lyivx.ls_core.docs.model.EntryJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SectionRenderer {
    private SectionRenderer() {}

    public static class HoverItem {
        public final int x, y, width, height; 
        public final ItemStack stack;
        public HoverItem(int x, int y, ItemStack stack) { this.x = x; this.y = y; this.width = 16; this.height = 16; this.stack = stack; }
        public HoverItem(int x, int y, ItemStack stack, int width, int height) { this.x = x; this.y = y; this.width = width; this.height = height; this.stack = stack; }
    }

    public static class TextScrollState {
        public int scrollY = 0;
        public int contentHeight = 0;
        public int viewportHeight = 0;
        public boolean needsScrollbar = false;
        
        public TextScrollState(int contentHeight, int viewportHeight) {
            this.contentHeight = contentHeight;
            this.viewportHeight = viewportHeight;
            this.needsScrollbar = contentHeight > viewportHeight;
        }
        
        public void scroll(int delta) {
            if (needsScrollbar) {
                int oldScroll = scrollY;
                scrollY -= delta; // Negative delta (scroll down) decreases scrollY, positive delta (scroll up) increases scrollY
                scrollY = Math.max(0, Math.min(scrollY, contentHeight - viewportHeight));
                
                // Debug logging
                System.out.println("Scrolling text: delta=" + delta + ", oldScroll=" + oldScroll + 
                                 ", newScroll=" + scrollY + ", contentHeight=" + contentHeight + 
                                 ", viewportHeight=" + viewportHeight + ", maxScroll=" + (contentHeight - viewportHeight));
            }
        }
    }

    public static class Result {
        public int nextY;
        public List<SectionLink> links = new ArrayList<>();
        public List<HoverItem> hoverItems = new ArrayList<>();
        public TextScrollState textScrollState = null;
    }

    public static Result renderSection(GuiGraphics gg, int x, int y, int width, int lineHeight, EntryJson.Section s, boolean i18n) {
        Result out = new Result();
        int cursorY = y;
        
        // Set scissor test to clip content to this section's bounds
        int sectionHeight = 120; // Fixed height for text sections
        int titleHeight = 0;
        
        // Auto-generate titles for recipes and showcases if not provided
        String sectionTitle = s.title;
        if ((sectionTitle == null || sectionTitle.isEmpty()) && "recipe".equals(s.type)) {
            // For recipes, use the output item name as title
            if (s.recipeId != null && !s.recipeId.isEmpty()) {
                try {
                    RecipeManager rm = getClientRecipeManager();
                    if (rm != null) {
                        Optional<? extends RecipeHolder<?>> opt = findRecipeById(rm, s.recipeId);
                        if (opt.isPresent()) {
                            Recipe<?> recipe = opt.get().value();
                            ItemStack result = getRecipeResultItem(recipe);
                            if (!result.isEmpty()) {
                                sectionTitle = result.getDisplayName().getString();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            // Fallback to items list if recipe ID not found
            if (sectionTitle == null && s.items != null && !s.items.isEmpty()) {
                ItemStack firstItem = itemStackById(s.items.get(s.items.size() - 1)); // Last item is usually output
                if (!firstItem.isEmpty()) {
                    sectionTitle = firstItem.getDisplayName().getString();
                }
            }
        } else if ((sectionTitle == null || sectionTitle.isEmpty()) && "showcase".equals(s.type)) {
            // For showcases, use the first item/block name as title
            if (s.items != null && !s.items.isEmpty()) {
                ItemStack firstItem = itemStackById(s.items.get(0));
                if (!firstItem.isEmpty()) {
                    sectionTitle = firstItem.getDisplayName().getString();
                }
            }
        }
        
        if (sectionTitle != null && !sectionTitle.isEmpty()) {
            // Clean up the title to remove brackets and other unwanted characters
            String cleanTitle = cleanItemName(sectionTitle);
            Component titleComponent = i18n ? Component.translatable(cleanTitle) : Component.literal(cleanTitle);
            int titleWidth = Minecraft.getInstance().font.width(titleComponent);
            int titleX = x + (width - titleWidth) / 2;
            gg.drawString(Minecraft.getInstance().font, titleComponent, titleX, cursorY, 0xFFFFFF);
            
            // Draw underline for title
            gg.fill(titleX, cursorY + lineHeight + 1, titleX + titleWidth, cursorY + lineHeight + 2, 0xFFFFFF);
            
            cursorY += lineHeight + 10; // Increased gap between title and content
            titleHeight = lineHeight + 10; // Title height + gap
        }
        
        // Adjust scissor to account for title
        gg.enableScissor(x, y, x + width, y + sectionHeight - titleHeight);
        
        String type = s.type == null ? "text" : s.type;
        if ("text".equals(type)) {
            String txt = s.text == null ? "" : s.text;
            // Process inline links in text
            InlineLinkResult linkResult = processInlineLinks(txt, i18n);
            
            // Calculate content height to determine if scrolling is needed
            // Use a reasonable height for text sections (about 100-150 pixels)
            int availableHeight = 120; // Fixed height for text sections
            if (sectionTitle != null && !sectionTitle.isEmpty()) {
                availableHeight -= titleHeight; // Subtract title height and gap
            }
            int contentHeight = calculateTextHeight(linkResult.text, width, lineHeight);
            TextScrollState scrollState = new TextScrollState(contentHeight, availableHeight);
            out.textScrollState = scrollState;
            
            // Adjust text width if scrolling is needed to make room for scrollbar
            int textWidth = width;
            if (scrollState.needsScrollbar) {
                textWidth -= 16; // Reserve space for scrollbar
            }
            
            // Render text with scrolling if needed
            cursorY = drawWrappedCenteredWithLinks(gg, linkResult.text, linkResult.links, x, cursorY, textWidth, lineHeight, scrollState);
            out.links.addAll(linkResult.links);
        } else if ("image".equals(type)) {
            if (s.image != null) {
                try {
                    ResourceLocation rl = ResourceLocation.parse(s.image);
                    int imageSize = 64;
                    int drawX = x + (width - imageSize) / 2;
                    int drawY = cursorY;
                    
                    // Draw fancy slot outline for image (can be made toggleable by adding showSlot field to EntryJson.Section)
                    drawFancySlot(gg, drawX - 3, drawY - 3, imageSize + 6, imageSize + 6);
                    
                    gg.blit(net.minecraft.client.renderer.RenderType::guiTextured, rl, drawX, drawY, 0, 0, imageSize, imageSize, imageSize, imageSize);
                    cursorY += imageSize + 4;
                } catch (Exception ignored) {}
            }
        } else if ("showcase".equals(type)) {
            if (s.items != null && !s.items.isEmpty()) {
                try {
                    // Only the first item/block should be showcased, and same size as images.
                    String firstId = s.items.get(0);
                    Optional<Item> itemOpt = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(firstId));
                    if (itemOpt.isPresent()) {
                        ItemStack stack = new ItemStack(itemOpt.get());
                        // Render at 70px (same as images) without scaling
                        int targetSize = 64; // pixels
                        int drawX = x + (width - targetSize) / 2;
                        int drawY = cursorY;

                        // Add hover item for tooltip (cover the entire 70x70 area)
                        out.hoverItems.add(new HoverItem(drawX, drawY, stack, targetSize, targetSize));
                        
                        // Draw fancy slot outline for showcase
                        drawFancySlot(gg, drawX - 2, drawY - 5, targetSize + 6, targetSize + 6);
                        
                        gg.pose().pushPose();
                        gg.pose().scale(4f, 4f, 4f);
                        // Adjust position for 2x scaling - divide by 2 and add 1 pixel offset to center properly
                        gg.renderItem(stack, ((drawX / 4) + 1), (int) ((drawY / 4)));
                        gg.pose().popPose();
                        

                        
                        cursorY += targetSize + 6;
                    }
                } catch (Exception ignored) {}
            }
        } else if ("link".equals(type)) {
            // Link sections are deprecated - use inline links in text instead
            String label = s.linkLabel != null ? s.linkLabel : (s.linkTarget == null ? "" : s.linkTarget);
            Component c = i18n ? Component.translatable(label) : Component.literal(label);
            int linkWidth = Minecraft.getInstance().font.width(c);
            int linkX = x + (width - linkWidth) / 2;
            gg.drawString(Minecraft.getInstance().font, c, linkX, cursorY, 0x66CCFF);
            gg.fill(linkX, cursorY + lineHeight - 2, linkX + linkWidth, cursorY + lineHeight - 1, 0x66CCFF);
            out.links.add(new SectionLink(s.linkType, s.linkTarget, label, linkX, cursorY, linkWidth, lineHeight));
            cursorY += lineHeight + 2;
        } else if ("recipe".equals(type)) {
            String rtype = s.recipeType == null ? "minecraft:crafting" : s.recipeType.toLowerCase();
            boolean rendered = false;
            if (s.recipeId != null && !s.recipeId.isEmpty()) {
                try {
                    RecipeManager rm = getClientRecipeManager();
                    if (rm != null) {
                        Optional<? extends RecipeHolder<?>> opt = findRecipeById(rm, s.recipeId);
                        if (opt.isPresent()) {
                            Recipe<?> recipe = opt.get().value();
                            if (recipe instanceof ShapedRecipe shaped) {
                                int w = shaped.getWidth();
                                int h = shaped.getHeight();
                                java.util.List<?> ings = getRecipeIngredients(recipe);
                                int recipeWidth = 3 * 18 + 22 + 18; // 3x3 grid + arrow + output
                                int gridX = x + (width - recipeWidth) / 2;
                                int gridY = cursorY;
                                for (int row = 0; row < 3; row++) {
                                    for (int col = 0; col < 3; col++) {
                                        int gx = gridX + col * 18;
                                        int gy = gridY + row * 18;
                                        drawSlot(gg, gx, gy);
                                        int ridx = row < h && col < w ? row * w + col : -1;
                                        if (ridx >= 0 && ridx < ings.size()) {
                                            Object ingObj = ings.get(ridx);
                                            ItemStack st = firstItemFromIngredient(ingObj);
                                            if (!st.isEmpty()) { gg.renderItem(st, gx, gy); out.hoverItems.add(new HoverItem(gx, gy, st)); }
                                        }
                                    }
                                }
                                ItemStack outStack = getRecipeResultItem(recipe);
                                int outX = gridX + 3 * 18 + 22;
                                int outY = gridY + 18;
                                drawSlot(gg, outX, outY);
                                if (!outStack.isEmpty()) { gg.renderItem(outStack, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, outStack)); }
                                cursorY += 3 * 18 + 6;
                                rendered = true;
                            } else if (recipe instanceof ShapelessRecipe) {
                                java.util.List<?> ings = getRecipeIngredients(recipe);
                                int recipeWidth = 3 * 18 + 22 + 18; // 3x3 grid + arrow + output
                                int gridX = x + (width - recipeWidth) / 2;
                                int gridY = cursorY;
                                for (int i = 0; i < 9; i++) {
                                    int gx = gridX + (i % 3) * 18;
                                    int gy = gridY + (i / 3) * 18;
                                    drawSlot(gg, gx, gy);
                                    if (i < ings.size()) {
                                        Object ingObj = ings.get(i);
                                        ItemStack st = firstItemFromIngredient(ingObj);
                                        if (!st.isEmpty()) { gg.renderItem(st, gx, gy); out.hoverItems.add(new HoverItem(gx, gy, st)); }
                                    }
                                }
                                ItemStack outStack = getRecipeResultItem(recipe);
                                int outX = gridX + 3 * 18 + 22;
                                int outY = gridY + 18;
                                drawSlot(gg, outX, outY);
                                if (!outStack.isEmpty()) { gg.renderItem(outStack, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, outStack)); }
                                cursorY += 3 * 18 + 6;
                                rendered = true;
                            } else if (isCookingRecipe(recipe)) {
                                // Generic cooking family (furnace/smoker/blast)
                                java.util.List<?> ings = getRecipeIngredients(recipe);
                                int recipeWidth = 76 + 18; // input + arrow + output
                                int recipeX = x + (width - recipeWidth) / 2;
                                int rowY = cursorY;
                                // Input (top-left), Fuel (below), Output (right)
                                drawSlot(gg, recipeX, rowY);
                                if (!ings.isEmpty()) { ItemStack st = firstItemFromIngredient(ings.get(0)); if (!st.isEmpty()) { gg.renderItem(st, recipeX, rowY); out.hoverItems.add(new HoverItem(recipeX, rowY, st)); } }
                                ItemStack outStack = getRecipeResultItem(recipe);
                                int fuelX = recipeX;
                                int fuelY = rowY + 20;
                                drawSlot(gg, fuelX, fuelY); // fuel slot (may be empty if unknown)
                                int outX = recipeX + 76;
                                int outY = rowY;
                                drawSlot(gg, outX, outY);
                                if (!outStack.isEmpty()) { gg.renderItem(outStack, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, outStack)); }
                                cursorY += 18 + 6;
                                rendered = true;
                            } else if (isStonecuttingRecipe(recipe)) {
                                java.util.List<?> ings = getRecipeIngredients(recipe);
                                int recipeWidth = 62 + 18; // input + arrow + output
                                int recipeX = x + (width - recipeWidth) / 2;
                                drawSlot(gg, recipeX, cursorY);
                                if (!ings.isEmpty()) { ItemStack st = firstItemFromIngredient(ings.get(0)); if (!st.isEmpty()) { gg.renderItem(st, recipeX, cursorY); out.hoverItems.add(new HoverItem(recipeX, cursorY, st)); } }
                                ItemStack outStack = getRecipeResultItem(recipe);
                                int outX = recipeX + 62;
                                int outY = cursorY;
                                drawSlot(gg, outX, outY);
                                if (!outStack.isEmpty()) { gg.renderItem(outStack, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, outStack)); }
                                cursorY += 18 + 6;
                                rendered = true;
                            } else if (isSmithingRecipe(recipe)) {
                                // Try to reflect template/base/addition
                                int recipeWidth = 100 + 18; // template + base + addition + arrow + output
                                int recipeX = x + (width - recipeWidth) / 2;
                                int cx = recipeX;
                                try {
                                    drawSlot(gg, cx, cursorY);
                                    ItemStack t = smithingIngredientIfPresent(recipe, "getTemplate"); if (!t.isEmpty()) { gg.renderItem(t, cx, cursorY); out.hoverItems.add(new HoverItem(cx, cursorY, t)); } cx += 20;
                                    drawSlot(gg, cx, cursorY);
                                    ItemStack b = smithingIngredientIfPresent(recipe, "getBase"); if (!b.isEmpty()) { gg.renderItem(b, cx, cursorY); out.hoverItems.add(new HoverItem(cx, cursorY, b)); } cx += 20;
                                    drawSlot(gg, cx, cursorY);
                                    ItemStack a = smithingIngredientIfPresent(recipe, "getAddition"); if (!a.isEmpty()) { gg.renderItem(a, cx, cursorY); out.hoverItems.add(new HoverItem(cx, cursorY, a)); }
                                } catch (Throwable ignored) {}
                                ItemStack outStack = getRecipeResultItem(recipe);
                                int outX = recipeX + 100;
                                int outY = cursorY;
                                drawSlot(gg, outX, outY);
                                if (!outStack.isEmpty()) { gg.renderItem(outStack, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, outStack)); }
                                cursorY += 18 + 6;
                                rendered = true;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!rendered) {
                // fallback to item list patterns by type (use official recipe type ids)
                if ("minecraft:crafting".equals(rtype) || "crafting".equals(rtype) || "crafting_shaped".equals(rtype) || "crafting_shapeless".equals(rtype)) {
                    int recipeWidth = 3 * 18 + 22 + 18; // 3x3 grid + arrow + output
                    int gridX = x + (width - recipeWidth) / 2;
                    int gridY = cursorY;
                    for (int i = 0; i < 9; i++) {
                        int gx = gridX + (i % 3) * 18;
                        int gy = gridY + (i / 3) * 18;
                        drawSlot(gg, gx, gy);
                        if (s.items != null && i < s.items.size()) {
                            ItemStack st = itemStackById(s.items.get(i));
                            if (!st.isEmpty()) { gg.renderItem(st, gx, gy); out.hoverItems.add(new HoverItem(gx, gy, st)); }
                        }
                    }
                    int outX = gridX + 3 * 18 + 22;
                    int outY = gridY + 18;
                    drawSlot(gg, outX, outY);
                    if (s.items != null && s.items.size() > 9) { 
                        ItemStack st = itemStackById(s.items.get(9)); 
                        System.out.println("Fallback crafting output: " + st.getDisplayName().getString() + " (empty: " + st.isEmpty() + ")");
                        if (!st.isEmpty()) { 
                            gg.renderItem(st, outX, outY); 
                            out.hoverItems.add(new HoverItem(outX, outY, st)); 
                        } else {
                            System.out.println("Fallback output is empty!");
                        }
                    } else {
                        System.out.println("No fallback output item found (items size: " + (s.items != null ? s.items.size() : "null") + ")");
                    }
                    cursorY += 3 * 18 + 6;
                } else if ("minecraft:smelting".equals(rtype) || "minecraft:smoking".equals(rtype) || "minecraft:blasting".equals(rtype) || "smelting".equals(rtype) || "smoking".equals(rtype) || "blasting".equals(rtype)) {
                    int recipeWidth = 76 + 18; // input + arrow + output
                    int recipeX = x + (width - recipeWidth) / 2;
                    int rowY = cursorY;
                    drawSlot(gg, recipeX, rowY);
                    if (s.items != null && s.items.size() > 0) { ItemStack st = itemStackById(s.items.get(0)); if (!st.isEmpty()) { gg.renderItem(st, recipeX, rowY); out.hoverItems.add(new HoverItem(recipeX, rowY, st)); } }
                    // fuel (optional) below input
                    int fuelX = recipeX;
                    int fuelY = rowY + 20;
                    drawSlot(gg, fuelX, fuelY);
                    if (s.items != null && s.items.size() > 1) { ItemStack st = itemStackById(s.items.get(1)); if (!st.isEmpty()) { gg.renderItem(st, fuelX, fuelY); out.hoverItems.add(new HoverItem(fuelX, fuelY, st)); } }
                    int outX = recipeX + 76;
                    int outY = rowY;
                    drawSlot(gg, outX, outY);
                    if (s.items != null && s.items.size() > 2) { ItemStack st = itemStackById(s.items.get(2)); if (!st.isEmpty()) { gg.renderItem(st, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, st)); } }
                    cursorY += 18 + 6;
                } else if ("minecraft:smithing".equals(rtype) || "smithing".equals(rtype)) {
                    int recipeWidth = 100 + 18; // template + base + addition + arrow + output
                    int recipeX = x + (width - recipeWidth) / 2;
                    int cx = recipeX;
                    drawSlot(gg, cx, cursorY); if (s.items != null && s.items.size() > 0) { ItemStack st = itemStackById(s.items.get(0)); if (!st.isEmpty()) { gg.renderItem(st, cx, cursorY); out.hoverItems.add(new HoverItem(cx, cursorY, st)); } } cx += 20;
                    drawSlot(gg, cx, cursorY); if (s.items != null && s.items.size() > 1) { ItemStack st = itemStackById(s.items.get(1)); if (!st.isEmpty()) { gg.renderItem(st, cx, cursorY); out.hoverItems.add(new HoverItem(cx, cursorY, st)); } } cx += 20;
                    drawSlot(gg, cx, cursorY); if (s.items != null && s.items.size() > 2) { ItemStack st = itemStackById(s.items.get(2)); if (!st.isEmpty()) { gg.renderItem(st, cx, cursorY); out.hoverItems.add(new HoverItem(cx, cursorY, st)); } }
                    int outX = recipeX + 100; int outY = cursorY; drawSlot(gg, outX, outY);
                    if (s.items != null && s.items.size() > 3) { ItemStack st = itemStackById(s.items.get(3)); if (!st.isEmpty()) { gg.renderItem(st, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, st)); } }
                    cursorY += 18 + 6;
                } else if ("minecraft:brewing".equals(rtype) || "brewing".equals(rtype)) {
                    // Simplified layout: ingredient (left), base (center), output (right)
                    int recipeWidth = 76 + 18; // ingredient + base + output
                    int recipeX = x + (width - recipeWidth) / 2;
                    int ingX = recipeX; int ingY = cursorY;
                    int baseX = recipeX + 30; int baseY = cursorY + 12;
                    int outX = recipeX + 76; int outY = cursorY + 6;
                    drawSlot(gg, ingX, ingY);
                    drawSlot(gg, baseX, baseY);
                    drawSlot(gg, outX, outY);
                    if (s.items != null) {
                        if (s.items.size() > 0) { ItemStack st = itemStackById(s.items.get(0)); if (!st.isEmpty()) { gg.renderItem(st, ingX, ingY); out.hoverItems.add(new HoverItem(ingX, ingY, st)); } }
                        if (s.items.size() > 1) { ItemStack st = itemStackById(s.items.get(1)); if (!st.isEmpty()) { gg.renderItem(st, baseX, baseY); out.hoverItems.add(new HoverItem(baseX, baseY, st)); } }
                        if (s.items.size() > 2) { ItemStack st = itemStackById(s.items.get(2)); if (!st.isEmpty()) { gg.renderItem(st, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, st)); } }
                    }
                    cursorY += 18 + 10;
                } else if ("minecraft:stonecutting".equals(rtype) || "stonecutting".equals(rtype)) {
                    int recipeWidth = 62 + 18; // input + arrow + output
                    int recipeX = x + (width - recipeWidth) / 2;
                    int inX = recipeX; int inY = cursorY;
                    int outX = recipeX + 62; int outY = cursorY;
                    drawSlot(gg, inX, inY);
                    drawSlot(gg, outX, outY);
                    if (s.items != null) {
                        if (s.items.size() > 0) { ItemStack st = itemStackById(s.items.get(0)); if (!st.isEmpty()) { gg.renderItem(st, inX, inY); out.hoverItems.add(new HoverItem(inX, inY, st)); } }
                        if (s.items.size() > 1) { ItemStack st = itemStackById(s.items.get(1)); if (!st.isEmpty()) { gg.renderItem(st, outX, outY); out.hoverItems.add(new HoverItem(outX, outY, st)); } }
                    }
                    cursorY += 18 + 6;
                } else {
                    gg.drawString(Minecraft.getInstance().font, Component.literal("[" + rtype + ": " + (s.recipeId == null ? "?" : s.recipeId) + "]"), x, cursorY, 0xAAAAAA);
                    cursorY += lineHeight + 2;
                }
            }
        }
        
        // Disable scissor test
        gg.disableScissor();
        
        out.nextY = cursorY;
        return out;
    }

    private static boolean isCookingRecipe(Recipe<?> recipe) {
        try {
            Class<?> cls = recipe.getClass();
            while (cls != null) {
                if ("net.minecraft.world.item.crafting.AbstractCookingRecipe".equals(cls.getName())) return true;
                cls = cls.getSuperclass();
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isStonecuttingRecipe(Recipe<?> recipe) {
        try {
            return recipe.getClass().getName().endsWith("StonecutterRecipe") || recipe.getClass().getSimpleName().contains("Stonecut");
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isSmithingRecipe(Recipe<?> recipe) {
        try {
            String n = recipe.getClass().getName();
            return n.contains("Smithing");
        } catch (Throwable ignored) {}
        return false;
    }

    private static ItemStack smithingIngredientIfPresent(Recipe<?> recipe, String methodName) {
        try {
            var m = recipe.getClass().getMethod(methodName);
            Object o = m.invoke(recipe);
            return firstItemFromIngredient(o);
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    private static RecipeManager getClientRecipeManager() {
        try {
            var mc = Minecraft.getInstance();
            if (mc == null) return null;
            // 1) Try client level (some mappings expose getRecipeManager on ClientLevel)
            try {
                var lvl = mc.level;
                if (lvl != null) {
                    var m = lvl.getClass().getMethod("getRecipeManager");
                    Object rm = m.invoke(lvl);
                    if (rm instanceof RecipeManager r) return r;
                }
            } catch (Throwable ignored) {}
            // 2) Try network connection's recipe manager (ClientPacketListener)
            try {
                var conn = mc.getConnection();
                if (conn != null) {
                    var m2 = conn.getClass().getMethod("getRecipeManager");
                    Object rm2 = m2.invoke(conn);
                    if (rm2 instanceof RecipeManager r2) return r2;
                }
            } catch (Throwable ignored) {}
            // 3) Fallback: singleplayer server
            if (mc.getSingleplayerServer() != null) return mc.getSingleplayerServer().getRecipeManager();
        } catch (Throwable ignored) {}
        return null;
    }

    // Try multiple strategies to resolve a recipe by id across different mappings
    private static Optional<? extends RecipeHolder<?>> findRecipeById(RecipeManager rm, String id) {
        if (rm == null || id == null || id.isEmpty()) return Optional.empty();
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            // Strategy 1: byKey(ResourceKey<Recipe<?>>)
            try {
                Optional<? extends RecipeHolder<?>> o = rm.byKey(ResourceKey.create(Registries.RECIPE, rl));
                if (o != null && o.isPresent()) return o;
            } catch (Throwable ignored) {}
            // Strategy 2: byKey(ResourceLocation) in some mappings
            try {
                var m = rm.getClass().getMethod("byKey", ResourceLocation.class);
                Object res = m.invoke(rm, rl);
                if (res instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof RecipeHolder<?> rh) {
                    return Optional.of(rh);
                }
            } catch (Throwable ignored) {}
            // Strategy 3: iterate all known recipes and match id
            try {
                // Prefer getAllRecipes() when present
                try {
                    var mAll = rm.getClass().getMethod("getAllRecipes");
                    Object list = mAll.invoke(rm);
                    if (list instanceof Iterable<?> it) {
                        for (Object o : it) {
                            if (o instanceof RecipeHolder<?> rh) {
                                ResourceLocation hid = extractHolderId(rh);
                                if (hid != null && hid.equals(rl)) return Optional.of(rh);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                // Fallback: check fields or maps (older mappings)
                try {
                    var mByType = rm.getClass().getMethod("getRecipes");
                    Object map = mByType.invoke(rm);
                    if (map instanceof java.util.Map<?, ?> outer) {
                        for (Object v : outer.values()) {
                            if (v instanceof java.util.Map<?, ?> inner) {
                                for (Object o : inner.values()) {
                                    if (o instanceof RecipeHolder<?> rh) {
                                        ResourceLocation hid = extractHolderId(rh);
                                        if (hid != null && hid.equals(rl)) return Optional.of(rh);
                                    } else if (o instanceof Recipe<?> r) {
                                        // Some older holders store Recipe directly
                                        ResourceLocation rid = extractRecipeId(r);
                                        if (rid != null && rid.equals(rl)) {
                                            // Wrap minimally into a holder-like object is overkill; just return empty and let fallback render kick in
                                            // Instead, continue searching
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    private static ResourceLocation extractHolderId(RecipeHolder<?> rh) {
        if (rh == null) return null;
        try {
            try {
                var m = rh.getClass().getMethod("id");
                Object out = m.invoke(rh);
                if (out instanceof ResourceLocation rl) return rl;
            } catch (Throwable ignored) {}
            try {
                var m2 = rh.getClass().getMethod("key");
                Object out2 = m2.invoke(rh);
                if (out2 instanceof ResourceKey<?> key) {
                    // ResourceKey has location()
                    var mLoc = key.getClass().getMethod("location");
                    Object loc = mLoc.invoke(key);
                    if (loc instanceof ResourceLocation rl2) return rl2;
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return null;
    }

    private static ResourceLocation extractRecipeId(Recipe<?> r) {
        if (r == null) return null;
        try {
            try {
                var m = r.getClass().getMethod("getId");
                Object out = m.invoke(r);
                if (out instanceof ResourceLocation rl) return rl;
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return null;
    }

    private static java.util.List<?> getRecipeIngredients(Recipe<?> recipe) {
        try {
            // Most recipes expose getIngredients()
            var m = recipe.getClass().getMethod("getIngredients");
            Object res = m.invoke(recipe);
            if (res instanceof java.util.List<?> list) return list;
        } catch (Throwable ignored) {}
        return java.util.List.of();
    }

    private static ItemStack getRecipeResultItem(Recipe<?> recipe) {
        try {
            // Newer API: recipe.getResultItem(RegistryAccess)
            var level = Minecraft.getInstance().level;
            if (level != null) {
                var m = recipe.getClass().getMethod("getResultItem", net.minecraft.core.RegistryAccess.class);
                Object out = m.invoke(recipe, level.registryAccess());
                if (out instanceof ItemStack stack) return stack;
            }
        } catch (Throwable ignored) {}
        // Fallback: try older no-arg getResultItem()
        try {
            var m2 = recipe.getClass().getMethod("getResultItem");
            Object out2 = m2.invoke(recipe);
            if (out2 instanceof ItemStack stack2) return stack2;
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    private static ItemStack firstItemFromIngredient(Object ingObj) {
        if (ingObj == null) return ItemStack.EMPTY;
        try {
            if (ingObj instanceof Ingredient ing) {
                // Use deprecated stream API for compatibility with current mappings
                final ItemStack[] res = new ItemStack[] { ItemStack.EMPTY };
                ing.items().findFirst().ifPresent((holder) -> {
                    Item item = holder.value();
                    if (item != null) res[0] = new ItemStack(item);
                });
                return res[0];
            }
            // Some mappings may wrap in Optional
            if (ingObj instanceof java.util.Optional<?> opt) {
                Object[] box = new Object[1];
                opt.ifPresent(val -> box[0] = val);
                if (box[0] instanceof Ingredient ing2) {
                    final ItemStack[] res = new ItemStack[] { ItemStack.EMPTY };
                    ing2.items().findFirst().ifPresent((holder) -> {
                        Item item2 = holder.value();
                        if (item2 != null) res[0] = new ItemStack(item2);
                    });
                    return res[0];
                }
            }
            // Fallback: try reflective access to getItems()
            var m = ingObj.getClass().getMethod("getItems");
            Object arr = m.invoke(ingObj);
            if (arr instanceof ItemStack[] stacks3 && stacks3.length > 0) {
                return stacks3[0];
            }
        } catch (Throwable ignored) {}
        return ItemStack.EMPTY;
    }

    private static int drawWrapped(GuiGraphics gg, Component text, int x, int y, int width, int lineHeight) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(text, width);
        for (FormattedCharSequence seq : lines) {
            gg.drawString(font, seq, x, y, 0xFFFFFF);
            y += lineHeight;
        }
        return y + 2;
    }
    
    private static int drawWrappedCentered(GuiGraphics gg, Component text, int x, int y, int width, int lineHeight) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(text, width);
        for (FormattedCharSequence seq : lines) {
            int lineWidth = font.width(seq);
            int lineX = x + (width - lineWidth) / 2;
            gg.drawString(font, seq, lineX, y, 0xFFFFFF);
            y += lineHeight;
        }
        return y + 2;
    }

    private static ItemStack itemStackById(String id) {
        try {
            Optional<Item> itemOpt = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
            if (itemOpt.isPresent()) return new ItemStack(itemOpt.get());
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }
    
    private static String cleanItemName(String itemName) {
        if (itemName == null) return "";
        // Remove common unwanted characters and formatting
        return itemName
            .replaceAll("\\[", "")  // Remove opening brackets
            .replaceAll("\\]", "")  // Remove closing brackets
            .replaceAll("\\{", "")  // Remove opening braces
            .replaceAll("\\}", "")  // Remove closing braces
            .replaceAll("minecraft:", "")  // Remove minecraft: prefix
            .replaceAll("_", " ")  // Replace underscores with spaces
            .trim();  // Remove leading/trailing whitespace
    }

    private static void drawSlot(GuiGraphics gg, int x, int y) {
        int x1 = x - 1, y1 = y - 1, x2 = x + 17, y2 = y + 17;
        // Background
        gg.fill(x1, y1, x2, y2, 0x44000000);
        // Outline (lighter)
        gg.fill(x1, y1, x2, y1 + 1, 0x99FFFFFF); // top
        gg.fill(x1, y2 - 1, x2, y2, 0x99000000); // bottom (shadow)
        gg.fill(x1, y1, x1 + 1, y2, 0x99FFFFFF); // left
        gg.fill(x2 - 1, y1, x2, y2, 0x99000000); // right (shadow)
    }
    
    private static void drawFancySlot(GuiGraphics gg, int x, int y, int width, int height) {
        // Background with more opacity
        gg.fill(x, y, x + width, y + height, 0x66000000);
        
        // Outer border (darker)
        gg.fill(x, y, x + width, y + 1, 0xCC333333); // top
        gg.fill(x, y + height - 1, x + width, y + height, 0xCC000000); // bottom
        gg.fill(x, y, x + 1, y + height, 0xCC333333); // left
        gg.fill(x + width - 1, y, x + width, y + height, 0xCC000000); // right
        
        // Inner highlight (lighter)
        gg.fill(x + 1, y + 1, x + width - 1, y + 2, 0x99FFFFFF); // top inner
        gg.fill(x + 1, y + 1, x + 2, y + height - 1, 0x99FFFFFF); // left inner
        gg.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0x99FFFFFF); // bottom inner
        gg.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0x99FFFFFF); // right inner

        // Corner accents
        gg.fill(x + 1, y + 1, x + 3, y + 3, 0xBBFFFFFF); // top-left corner
        gg.fill(x + width - 3, y + 1, x + width - 1, y + 3, 0xBBFFFFFF); // top-right corner
        gg.fill(x + 1, y + height - 3, x + 3, y + height - 1, 0xBBFFFFFF); // bottom-left corner
        gg.fill(x + width - 3, y + height - 3, x + width - 1, y + height - 1, 0xBBFFFFFF); // bottom-right corner
    }
    
    // Inline link processing classes and methods
    public static class InlineLinkResult {
        public final Component text;
        public final List<SectionLink> links;
        
        public InlineLinkResult(Component text, List<SectionLink> links) {
            this.text = text;
            this.links = links;
        }
    }
    
    private static InlineLinkResult processInlineLinks(String text, boolean i18n) {
        List<SectionLink> links = new ArrayList<>();
        
        // Pattern: [[entry:entry_id:label]] or [[category:category_id:label]] or [[entry:entry_id]]
        String pattern = "\\[\\[(entry|category):([^:]+)(?::([^\\]]+))?\\]\\]";
        java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = linkPattern.matcher(text);
        
        // Store link information with their positions in the original text
        while (matcher.find()) {
            String linkType = matcher.group(1); // "entry" or "category"
            String linkTarget = matcher.group(2); // the ID
            String linkLabel = matcher.group(3); // the display text (may be null)
            
            if (linkLabel == null) {
                linkLabel = linkTarget; // Use ID as display text if no label provided
            }
            
            // Store link info with position in original text
            links.add(new SectionLink(linkType, linkTarget, linkLabel, matcher.start(), 0, matcher.end() - matcher.start(), 0));
        }
        
        // Return the original text and links - we'll handle rendering directly
        Component originalText = i18n ? Component.translatable(text) : Component.literal(text);
        return new InlineLinkResult(originalText, links);
    }
    
    private static int drawWrappedCenteredWithLinks(GuiGraphics gg, Component text, List<SectionLink> links, int x, int y, int width, int lineHeight, TextScrollState scrollState) {
        Font font = Minecraft.getInstance().font;
        String textStr = text.getString();
        int currentY = y;
        
        // Apply scroll offset if scrolling is needed
        int scrollOffset = scrollState != null ? scrollState.scrollY : 0;
        int maxHeight = scrollState != null ? scrollState.viewportHeight : Integer.MAX_VALUE;
        
        // Debug scroll offset
        if (scrollState != null && scrollState.scrollY > 0) {
            System.out.println("Applying scroll offset: " + scrollOffset + " to text starting at Y=" + y);
            System.out.println("Text will be rendered at Y=" + (y - scrollOffset) + " instead of Y=" + y);
        }
        
        // If no links, just render normally with scrolling
        if (links.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(text, width);
            for (FormattedCharSequence seq : lines) {
                // Check if this line should be visible (within scroll bounds)
                int lineY = currentY - scrollOffset;
                if (lineY + lineHeight >= y && lineY <= y + maxHeight) {
                    int lineWidth = font.width(seq);
                    int lineX = x + (width - lineWidth) / 2;
                    gg.drawString(font, seq, lineX, lineY, 0xFFFFFF);
                }
                currentY += lineHeight;
            }
            return y + Math.min(maxHeight, currentY - y) + 2;
        }
        
        // Sort links by their position in the text
        List<SectionLink> sortedLinks = new ArrayList<>(links);
        sortedLinks.sort((a, b) -> Integer.compare(a.x, b.x));
        
        // Render text with inline links, handling line wrapping
        int currentX = x;
        int lastLinkEnd = 0;
        int linkIndex = 0;
        
        for (SectionLink link : sortedLinks) {
            // Render text before this link
            if (link.x > lastLinkEnd) {
                String textBefore = textStr.substring(lastLinkEnd, link.x);
                if (!textBefore.isEmpty()) {
                    // Handle text wrapping for the text before the link
                    List<FormattedCharSequence> beforeLines = font.split(Component.literal(textBefore), width - (currentX - x));
                    for (FormattedCharSequence seq : beforeLines) {
                        int lineWidth = font.width(seq);
                        if (currentX + lineWidth > x + width) {
                            // Move to next line
                            currentY += lineHeight;
                            currentX = x;
                        }
                        // Check if this line should be visible (within scroll bounds)
                        int lineY = currentY - scrollOffset;
                        if (lineY + lineHeight >= y && lineY <= y + maxHeight) {
                            gg.drawString(font, seq, currentX, lineY, 0xFFFFFF);
                        }
                        currentX += lineWidth;
                    }
                }
            }
            
            // Check if we need to wrap to a new line for the link
            int linkWidth = font.width(link.label);
            if (currentX + linkWidth > x + width) {
                currentY += lineHeight;
                currentX = x;
            }
            
            // Render the link if it's visible
            int linkY = currentY - scrollOffset;
            if (linkY + lineHeight >= y && linkY <= y + maxHeight) {
                int linkX = currentX;
                
                // Update link position for clickability
                SectionLink positionedLink = new SectionLink(link.linkType, link.linkTarget, link.label, linkX, linkY, linkWidth, lineHeight);
                links.set(linkIndex, positionedLink);
                
                // Render link text in blue with underline
                gg.drawString(font, Component.literal(link.label), linkX, linkY, 0x66CCFF);
                // Draw underline below the link text
                gg.fill(linkX, linkY + lineHeight - 1, linkX + linkWidth, linkY + lineHeight, 0x66CCFF);
            }
            
            currentX += linkWidth;
            lastLinkEnd = link.x + (link.width); // link.width contains the length of the original [[...]] text
            linkIndex++;
        }
        
        // Render remaining text after the last link
        if (lastLinkEnd < textStr.length()) {
            String remainingText = textStr.substring(lastLinkEnd);
            if (!remainingText.isEmpty()) {
                // Handle text wrapping for remaining text
                List<FormattedCharSequence> remainingLines = font.split(Component.literal(remainingText), width - (currentX - x));
                for (FormattedCharSequence seq : remainingLines) {
                    int lineWidth = font.width(seq);
                    if (currentX + lineWidth > x + width) {
                        // Move to next line
                        currentY += lineHeight;
                        currentX = x;
                    }
                    // Check if this line should be visible (within scroll bounds)
                    int lineY = currentY - scrollOffset;
                    if (lineY + lineHeight >= y && lineY <= y + maxHeight) {
                        gg.drawString(font, seq, currentX, lineY, 0xFFFFFF);
                    }
                    currentY += lineHeight;
                }
            }
        }
        
        return y + Math.min(maxHeight, currentY - y) + 2;
    }

    private static int calculateTextHeight(Component text, int width, int lineHeight) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(text, width);
        return lines.size() * lineHeight;
    }
}


