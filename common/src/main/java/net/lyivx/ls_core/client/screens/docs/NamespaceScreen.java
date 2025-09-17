package net.lyivx.ls_core.client.screens.docs;

import net.lyivx.ls_core.client.screens.ListSubScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.lyivx.ls_core.docs.DocsService;
import net.lyivx.ls_core.docs.model.CategoryJson;
import net.lyivx.ls_core.docs.model.DocJson;
import net.lyivx.ls_core.client.screens.docs.widgets.IconListButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Screen listing categories for a specific documentation namespace.
 */
public class NamespaceScreen extends ListSubScreen {
    private final Screen lastScreen;
    private final String namespace;
    private final List<Button> categoryButtons = new ArrayList<>();
    private EditBox searchBox;
    private String lastQuery = null;
    private boolean showSearch = true;
    private int listScroll = 0;
    private int listContentHeight = 0;

    public NamespaceScreen(Screen lastScreen, Options options, String namespace) {
        super(lastScreen, options, titleFor(namespace));
        this.lastScreen = lastScreen;
        this.namespace = namespace;
    }

    private static Component titleFor(String namespace) {
        var meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
        if (meta != null && meta.title != null) {
            return meta.useI18n ? Component.translatable(meta.title) : Component.literal(meta.title);
        }
        return Component.literal(namespace);
    }

    @Override
    protected void addTitle() {
        super.addTitle();
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

    @Override
    protected void addContents() {
        DocJson meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
        this.showSearch = meta == null || meta.searchbar;
        if (this.showSearch) {
            int headerY = this.layout.getHeaderHeight();
            this.searchBox = new EditBox(this.font, this.width / 2 - 100, headerY + 6, 200, 20, Component.translatable("screen.ls_core.search"));
            this.searchBox.setResponder(q -> refreshCategories());
            this.addRenderableWidget(this.searchBox);
        } else {
            this.searchBox = null;
        }

        List<CategoryJson> categories = new ArrayList<>(DocsService.get().getCategories(namespace));
        if (meta != null && meta.categoryOrder != null && !meta.categoryOrder.isEmpty()) {
            categories.sort(Comparator.comparingInt(c -> meta.categoryOrder.indexOf(c.id)));
        } else {
            categories.sort(Comparator.comparing(c -> c.title == null ? "" : c.title));
        }
        refreshCategories();
    }

    protected void addFooter() {
        LinearLayout footer = (LinearLayout)this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(Component.translatable("gui.back"), b -> this.minecraft.setScreen(this.lastScreen)).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int listTop = (this.showSearch ? this.layout.getHeaderHeight() + 6 + 28 : this.layout.getHeaderHeight() + 6);
        int listBottom = this.height - 33;

        // Draw scrollbar if needed
        int viewport = Math.max(0, listBottom - listTop);
        if (listContentHeight > viewport) {
            int trackX1 = this.width / 2 + 102;
            int trackX2 = this.width / 2 + 104;
            guiGraphics.fill(trackX1, listTop, trackX2, listBottom, 0x33000000);
            float ratio = (float)viewport / (float)listContentHeight;
            int thumbH = Math.max(12, (int)(viewport * ratio));
            int maxScroll = Math.max(0, listContentHeight - viewport);
            int thumbY = listTop + (maxScroll == 0 ? 0 : (int)((float)listScroll / (float)maxScroll * (viewport - thumbH)));
            guiGraphics.fill(trackX1, thumbY, trackX2, thumbY + thumbH, 0x99000000);
        }

        // Per-frame visibility/active update
        int vpTop = (this.showSearch ? this.layout.getHeaderHeight() + 6 + 28 : this.layout.getHeaderHeight() + 6);
        int vpBottom = this.height - 33;
        for (Button b : this.categoryButtons) {
            boolean vis = b.getY() + b.getHeight() > vpTop && b.getY() < vpBottom;
            b.visible = vis;
            b.active = vis;
        }
    }

    private void refreshCategories() {
        String query = (this.searchBox == null || this.searchBox.getValue() == null) ? "" : this.searchBox.getValue().toLowerCase();
        this.lastQuery = query;
        for (Button b : this.categoryButtons) this.removeWidget(b);
        this.categoryButtons.clear();

        int listTop = (this.showSearch ? this.layout.getHeaderHeight() + 6 + 28 : this.layout.getHeaderHeight() + 6);
        int listBottom = this.height - 33;
        int y = listTop - listScroll;
        List<CategoryJson> categories = new ArrayList<>(DocsService.get().getCategories(namespace));
        DocJson meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
        if (meta != null && meta.categoryOrder != null && !meta.categoryOrder.isEmpty()) {
            categories.sort(Comparator.comparingInt(c -> meta.categoryOrder.indexOf(c.id)));
        } else {
            categories.sort(Comparator.comparing(c -> c.title == null ? "" : c.title));
        }
        for (CategoryJson category : categories) {
            String titleTextRaw = category.title == null ? category.id : category.title;
            String titleText = (meta != null && meta.useI18n) ? Component.translatable(titleTextRaw).getString() : titleTextRaw;
            if (!query.isEmpty() && !titleText.toLowerCase().contains(query)) continue;
            Button button = new IconListButton(this.width / 2 - 100, y, 200, 20,
                    Component.literal(titleText), category.iconType, category.icon,
                    b -> this.minecraft.setScreen(new CategoryScreen(this, this.minecraft.options, namespace, category.id)));
            if (category.tooltip != null && !category.tooltip.isEmpty()) {
                Component tip = (meta != null && meta.useI18n) ? Component.translatable(category.tooltip) : Component.literal(category.tooltip);
                button.setTooltip(Tooltip.create(tip));
            }
            button.visible = y + 20 > listTop && y < listBottom;
            button.active = button.visible;
            this.categoryButtons.add(button);
            this.addRenderableWidget(button);
            y += 24;
        }
        this.listContentHeight = Math.max(0, y - (listTop - listScroll));
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.lastQuery = null;
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int listTop = (this.showSearch ? this.layout.getHeaderHeight() + 6 + 28 : this.layout.getHeaderHeight() + 6);
        int listBottom = this.height - 33;
        int listLeft = this.width / 2 - 100;
        int listRight = this.width / 2 + 100;
        if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom && listContentHeight > (listBottom - listTop)) {
            int maxScroll = Math.max(0, listContentHeight - (listBottom - listTop));
            listScroll -= (int)(deltaY * 12);
            if (listScroll < 0) listScroll = 0;
            if (listScroll > maxScroll) listScroll = maxScroll;
            this.setFocused(null);
            this.refreshCategories();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
}


