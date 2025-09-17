package net.lyivx.ls_core.client.screens.docs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.lyivx.ls_core.client.screens.SidebarSplitSubScreen;
import net.lyivx.ls_core.client.screens.docs.widgets.IconListButton;
import net.lyivx.ls_core.client.screens.docs.renderer.SectionRenderer;
import net.lyivx.ls_core.client.screens.docs.renderer.SectionLink;
import net.lyivx.ls_core.client.screens.docs.CategoryScreen;
import net.lyivx.ls_core.docs.DocsService;
import net.lyivx.ls_core.docs.model.CategoryJson;
import net.lyivx.ls_core.docs.model.EntryJson;
import net.lyivx.ls_core.docs.model.EntryJson.Section;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Split-screen version of EntryScreen using SidebarSplitLayout.
 * Left sidebar shows scrollable categories/entries, right side has two columns for content.
 */
public class EntrySplitScreen extends SidebarSplitSubScreen {
    private final String namespace;
    private final String entryId;
    private int currentPageIndex = 0;
    private boolean hasPrev;
    private boolean hasNext;
    private Button prevArrow;
    private Button nextArrow;
    
    // Left navigation
    private final List<Button> navButtons = new ArrayList<>();
    private String openCategoryId;
    
    // Page rendering state
    private List<SectionLink> currentLinks = new ArrayList<>();
    private List<SectionRenderer.TextScrollState> textScrollStates = new ArrayList<>();
    private SectionRenderer.TextScrollState hoveredTextScroll = null;
    


    public EntrySplitScreen(Screen lastScreen, Options options, String namespace, String entryId) {
        super(lastScreen, options, titleFor(namespace, entryId));
        this.namespace = namespace;
        this.entryId = entryId;
    }
    
    private static Component titleFor(String namespace, String entryId) {
        var meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
        var entry = DocsService.get().getEntry(namespace, entryId).orElse(null);
        String raw = entry != null && entry.title != null ? entry.title : entryId;
        return (meta != null && meta.useI18n) ? Component.translatable(raw) : Component.literal(raw);
    }

    @Override
    protected void init() {
        this.addTitle();
        this.addContents();
        this.addFooter();
        this.layout.visitWidgets((guiEventListener) -> {
            AbstractWidget var10000 = (AbstractWidget) this.addRenderableWidget(guiEventListener);
        });
        this.repositionElements();
    }
    
    private void refreshPageArrows() {
        // Remove existing arrows
        if (this.prevArrow != null) this.removeWidget(this.prevArrow);
        if (this.nextArrow != null) this.removeWidget(this.nextArrow);
        this.prevArrow = null;
        this.nextArrow = null;
        
        // Recompute nav state based on current entry
        var entry = DocsService.get().getEntry(namespace, entryId).orElse(null);
        int pageCount = entry != null && entry.pages != null ? entry.pages.size() : 0;
        if (pageCount <= 0) currentPageIndex = 0; else if (currentPageIndex >= pageCount) currentPageIndex = pageCount - 1; else if (currentPageIndex < 0) currentPageIndex = 0;
        this.hasPrev = pageCount > 0 && currentPageIndex > 0;
        this.hasNext = pageCount > 0 && currentPageIndex < pageCount - 1;
        
        int y = this.height - 24;
        if (hasPrev) {
            this.prevArrow = Button.builder(Component.literal("<"), b -> {
                currentPageIndex--;
                this.setFocused(null);
                refreshPageArrows();
            }).bounds(6, y, 16, 16).build();
            this.addRenderableWidget(this.prevArrow);
        }
        if (hasNext) {
            this.nextArrow = Button.builder(Component.literal(">"), b -> {
                currentPageIndex++;
                this.setFocused(null);
                refreshPageArrows();
            }).bounds(this.width - 22, y, 16, 16).build();
            this.addRenderableWidget(this.nextArrow);
        }
    }

    @Override
    protected void addContents() {
        var entry = DocsService.get().getEntry(namespace, entryId).orElse(null);
        if (this.openCategoryId == null && entry != null) this.openCategoryId = entry.category;
        int pageCount = entry != null && entry.pages != null ? entry.pages.size() : 0;
        if (pageCount <= 0) currentPageIndex = 0; else if (currentPageIndex >= pageCount) currentPageIndex = pageCount - 1; else if (currentPageIndex < 0) currentPageIndex = 0;
        this.hasPrev = pageCount > 0 && currentPageIndex > 0;
        this.hasNext = pageCount > 0 && currentPageIndex < pageCount - 1;
        rebuildLeftNav(entry);
    }



    private void rebuildLeftNav(EntryJson current) {
        for (Button b : this.navButtons) this.removeWidget(b);
        this.navButtons.clear();

        int navTop = this.layout.getHeaderHeight() + 6;
        int navBottom = (this.height - this.layout.getFooterHeight()) - 6;

        // Ensure scroll position is valid
        if (getNavContentHeight() > 0) {
            int viewportHeight = navBottom - navTop;
            if (getNavContentHeight() > viewportHeight) {
                int maxScroll = Math.max(0, getNavContentHeight() - viewportHeight);
                setNavScroll(Math.max(0, Math.min(getNavScroll(), maxScroll)));
            } else {
                setNavScroll(0);
            }
        }

        int y = navTop - getNavScroll();
        var meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
        List<CategoryJson> categories = new ArrayList<>(DocsService.get().getCategories(namespace));
        categories.sort(Comparator.comparing(c -> c.title == null ? c.id : c.title));

        for (CategoryJson cat : categories) {
            boolean wouldBeVisible = y + 20 > navTop && y < navBottom;
            boolean isOpen = cat.id != null && cat.id.equals(this.openCategoryId);

            if (wouldBeVisible) {
                String catTitleRaw = cat.title == null ? cat.id : cat.title;
                String catTitle = (meta != null && meta.useI18n) ? Component.translatable(catTitleRaw).getString() : catTitleRaw;
                String arrow = isOpen ? "v " : "> ";

                Button catButton = new IconListButton(10, y, this.layout.getSidebarWidth() - 20, 20,
                        Component.literal(arrow + catTitle), cat.iconType, cat.icon,
                        b -> {
                            this.openCategoryId = isOpen ? null : cat.id;
                            this.setFocused(null);
                            rebuildLeftNav(DocsService.get().getEntry(namespace, entryId).orElse(null));
                        });
                catButton.visible = true;
                catButton.active = true;
                this.navButtons.add(catButton);
                this.addRenderableWidget(catButton);
            }
            y += 25;

            if (isOpen) {
                List<EntryJson> entries = new ArrayList<>();
                for (EntryJson e : DocsService.get().getEntries(namespace)) {
                    if (e.category != null && e.category.equals(cat.id)) entries.add(e);
                }
                entries.sort((a, b) -> {
                    String at = a.title == null ? a.id : a.title;
                    String bt = b.title == null ? b.id : b.title;
                    return at.compareToIgnoreCase(bt);
                });

                for (EntryJson e : entries) {
                    boolean entryVisible = y + 20 > navTop && y < navBottom;
                    if (entryVisible) {
                        String raw = e.title == null ? e.id : e.title;
                        String title = (meta != null && meta.useI18n) ? Component.translatable(raw).getString() : raw;

                        boolean isCurrent = current != null && current.id != null && current.id.equals(e.id);
                        Button entryButton = new IconListButton(20, y - 5, this.layout.getSidebarWidth() - 30, 20,
                                Component.literal("  " + title), e.iconType, e.icon,
                                btn -> this.minecraft.setScreen(new EntrySplitScreen(this, this.minecraft.options, namespace, e.id)));
                        entryButton.active = !isCurrent;
                        entryButton.visible = true;
                        entryButton.active = entryButton.active && true;
                        this.navButtons.add(entryButton);
                        this.addRenderableWidget(entryButton);
                    }
                    y += 20;
                }
            }
        }
        setNavContentHeight(Math.max(0, y - (navTop - getNavScroll())));

        // Ensure scroll position is valid after rebuilding
        if (getNavContentHeight() > 0) {
            int viewportHeight = navBottom - navTop;
            if (getNavContentHeight() > viewportHeight) {
                int maxScroll = Math.max(0, getNavContentHeight() - viewportHeight);
                setNavScroll(Math.max(0, Math.min(getNavScroll(), maxScroll)));
            } else {
                setNavScroll(0);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Update button visibility based on viewport bounds
        int navTop = this.layout.getHeaderHeight() + 6;
        int navBottom = (this.height - this.layout.getFooterHeight()) - 6;

        // Update button visibility and active state based on viewport
        for (Button b : this.navButtons) {
            // More strict visibility check - button must be fully within the viewport
            boolean vis = b.getY() >= navTop && (b.getY() + b.getHeight()) <= navBottom;
            b.visible = vis;
            if (!(b.getMessage().getString().startsWith("  "))) { // category buttons keep active unless hidden
                b.active = vis;
            } else {
                b.active = b.active && vis; // entry buttons may already be disabled for current entry
            }
        }

        // Render content using the layout's content areas
        int contentX = this.layout.getSidebarWidth() + 10;
        int contentY = this.layout.getHeaderHeight() + 30;
        int contentWidth = this.width - this.layout.getSidebarWidth();
        int colWidth = ((contentWidth) / 2) - 10;
        int leftColX = contentX;
        int rightColX = contentX + colWidth + 10;
        int leftY = contentY;
        int rightY = contentY;
        this.currentLinks.clear();
        this.textScrollStates.clear();
        java.util.List<SectionRenderer.HoverItem> hoverItems = new java.util.ArrayList<>();

        var entry = DocsService.get().getEntry(namespace, entryId).orElse(null);
        if (entry != null && entry.pages != null && !entry.pages.isEmpty()) {
            var p = entry.pages.get(Math.max(0, Math.min(currentPageIndex, entry.pages.size() - 1)));
            List<EntryJson.Section> sections = new ArrayList<>();
            if (p.sections != null && !p.sections.isEmpty()) sections.addAll(p.sections);
            else {
                EntryJson.Section s = new EntryJson.Section();
                s.type = p.type; s.text = p.text; s.image = p.image; s.recipeId = p.recipeId; s.recipeType = p.recipeType; s.items = p.items; s.linkType = p.linkType; s.linkTarget = p.linkTarget; s.linkLabel = p.linkLabel;
                sections.add(s);
            }
            boolean i18n = DocsService.get().getDocForNamespace(namespace).map(d -> d.useI18n).orElse(false);
            
            // Calculate equal row heights for the 2x2 grid
            int availableHeight = this.height - this.layout.getHeaderHeight() - this.layout.getFooterHeight() - 60;
            int rowHeight = availableHeight / 2;
            
            // Define the 4 content areas with fixed positions
            int leftTopY = contentY;
            int leftBottomY = contentY + rowHeight + 10;
            int rightTopY = contentY;
            int rightBottomY = contentY + rowHeight + 10;
            
            // Only render up to 4 sections (one per area)
            int maxSections = Math.min(sections.size(), 4);
            for (int i = 0; i < maxSections; i++) {
                EntryJson.Section s = sections.get(i);
                
                // Fixed distribution: 0=left-top, 1=left-bottom, 2=right-top, 3=right-bottom
                if (i == 0) { // Left top
                    var res = SectionRenderer.renderSection(guiGraphics, leftColX, leftTopY, colWidth, this.font.lineHeight, s, i18n);
                    this.currentLinks.addAll(res.links);
                    hoverItems.addAll(res.hoverItems);
                    if (res.textScrollState != null) {
                        this.textScrollStates.add(res.textScrollState);
                        // Render scrollbar if needed
                        renderTextScrollbar(guiGraphics, leftColX, leftTopY, colWidth, rowHeight, res.textScrollState);
                    }
                } else if (i == 1) { // Left bottom
                    var res = SectionRenderer.renderSection(guiGraphics, leftColX, leftBottomY, colWidth, this.font.lineHeight, s, i18n);
                    this.currentLinks.addAll(res.links);
                    hoverItems.addAll(res.hoverItems);
                    if (res.textScrollState != null) {
                        this.textScrollStates.add(res.textScrollState);
                        // Render scrollbar if needed
                        renderTextScrollbar(guiGraphics, leftColX, leftBottomY, colWidth, rowHeight, res.textScrollState);
                    }
                } else if (i == 2) { // Right top
                    var res = SectionRenderer.renderSection(guiGraphics, rightColX, rightTopY, colWidth, this.font.lineHeight, s, i18n);
                    this.currentLinks.addAll(res.links);
                    hoverItems.addAll(res.hoverItems);
                    if (res.textScrollState != null) {
                        this.textScrollStates.add(res.textScrollState);
                        // Render scrollbar if needed
                        renderTextScrollbar(guiGraphics, rightColX, rightTopY, colWidth, rowHeight, res.textScrollState);
                    }
                } else if (i == 3) { // Right bottom
                    var res = SectionRenderer.renderSection(guiGraphics, rightColX, rightBottomY, colWidth, this.font.lineHeight, s, i18n);
                    this.currentLinks.addAll(res.links);
                    hoverItems.addAll(res.hoverItems);
                    if (res.textScrollState != null) {
                        this.textScrollStates.add(res.textScrollState);
                        // Render scrollbar if needed
                        renderTextScrollbar(guiGraphics, rightColX, rightBottomY, colWidth, rowHeight, res.textScrollState);
                    }
                }
            }

            // Render tooltips for hovered items (using custom width/height)
            for (SectionRenderer.HoverItem hi : hoverItems) {
                if (mouseX >= hi.x && mouseX <= hi.x + hi.width && mouseY >= hi.y && mouseY <= hi.y + hi.height) {
                    guiGraphics.renderTooltip(this.font, hi.stack, mouseX, mouseY);
                    break; // show only one tooltip at a time
                }
            }
        }
    }

    private void renderTextScrollbar(GuiGraphics guiGraphics, int x, int y, int width, int height, SectionRenderer.TextScrollState scrollState) {
        if (!scrollState.needsScrollbar) return;
        
        // Draw scrollbar on the right side of the text area
        int scrollbarX = x + width - 6;
        int scrollbarY = y;
        int scrollbarWidth = 4;
        int scrollbarHeight = height;
        
        // Draw scrollbar track
        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0x33000000);
        
        // Calculate thumb size and position
        float ratio = (float) height / (float) scrollState.contentHeight;
        int thumbHeight = Math.max(8, (int) (height * ratio));
        int maxScroll = Math.max(0, scrollState.contentHeight - height);
        int thumbY = scrollbarY + (maxScroll == 0 ? 0 : (int) ((float) scrollState.scrollY / (float) maxScroll * (height - thumbHeight)));
        
        // Draw scrollbar thumb
        guiGraphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x99000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (SectionLink link : this.currentLinks) {
            if (mouseX >= link.x && mouseX <= link.x + link.width && mouseY >= link.y && mouseY <= link.y + link.height) {
                if ("entry".equalsIgnoreCase(link.linkType)) {
                    this.minecraft.setScreen(new EntrySplitScreen(this, this.minecraft.options, this.namespace, link.linkTarget));
                    return true;
                } else if ("category".equalsIgnoreCase(link.linkType)) {
                    this.minecraft.setScreen(new CategoryScreen(this, this.minecraft.options, this.namespace, link.linkTarget));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        // Check if we're hovering over a text section that needs scrolling
        for (SectionRenderer.TextScrollState scrollState : this.textScrollStates) {
            if (scrollState.needsScrollbar) {
                // Check if mouse is over the content area (simplified bounds check)
                int contentX = this.layout.getSidebarWidth() + 10;
                int contentY = this.layout.getHeaderHeight() + 30;
                int contentWidth = this.width - this.layout.getSidebarWidth();
                int colWidth = ((contentWidth) / 2) - 10;
                
                // Check if mouse is over any content area
                if (mouseX >= contentX && mouseX <= contentX + colWidth * 2 + 10 && 
                    mouseY >= contentY && mouseY <= contentY + (this.height - this.layout.getHeaderHeight() - this.layout.getFooterHeight() - 60)) {
                    // Apply scroll to all text sections that need it
                    scrollState.scroll((int)(deltaY * 12));
                    return true;
                }
            }
        }
        
        // Handle sidebar navigation scrolling
        int navTop = this.layout.getHeaderHeight() + 6;
        int navBottom = (this.height - this.layout.getFooterHeight()) - 6;
        int viewportHeight = navBottom - navTop;
        
        // Only allow scrolling if there's actually content to scroll
        if (mouseX >= 0 && mouseX <= this.layout.getSidebarWidth() && mouseY >= navTop && mouseY <= navBottom && getNavContentHeight() > viewportHeight) {
            int maxScroll = Math.max(0, getNavContentHeight() - viewportHeight);
            int oldScroll = getNavScroll();
            
            // Apply scroll delta
            setNavScroll(getNavScroll() - (int)(deltaY * 12));
            
            // Clamp scroll position - this is crucial for preventing over-scrolling
            setNavScroll(Math.max(0, Math.min(getNavScroll(), maxScroll)));
            
            // Only rebuild if scroll actually changed
            if (oldScroll != getNavScroll()) {
                this.setFocused(null);
                rebuildLeftNav(DocsService.get().getEntry(namespace, entryId).orElse(null));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        setNavScroll(0);
        super.resize(minecraft, width, height);
    }
}
