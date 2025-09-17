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

import net.minecraft.network.chat.Component;
import net.lyivx.ls_core.docs.DocsService;
import net.lyivx.ls_core.docs.model.DocJson;
import net.lyivx.ls_core.client.screens.docs.widgets.IconListButton;


import java.util.ArrayList;
import java.util.List;

/**
 * Root Docs screen that lists available document namespaces.
 * Styled roughly like a Minecraft list screen for now; can be iterated on.
 */
public class DocsScreen extends ListSubScreen {
    private final Screen lastScreen;
    private final Options options;
    private EditBox searchBox;
    private final List<Button> docButtons = new ArrayList<>();
    private String lastQuery = null;
    private int listScroll = 0;
    private int listContentHeight = 0;

    public DocsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("screen.ls_core.docs.title"));
        this.lastScreen = lastScreen;
        this.options = options;

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
    protected void addTitle() {
        super.addTitle();
    }

    @Override
    protected void addContents() {
        int headerY = this.layout.getHeaderHeight();
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, headerY + 6, 200, 20, Component.translatable("screen.ls_core.search"));
        this.searchBox.setResponder(q -> refreshDocList());
        this.addRenderableWidget(this.searchBox);
        refreshDocList();
    }



    protected void addFooter() {
        LinearLayout footer = (LinearLayout)this.layout.addToFooter(LinearLayout.horizontal().spacing(8));

        footer.addChild(Button.builder(Component.translatable("gui.done"), b -> this.minecraft.setScreen(this.lastScreen)).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int listTop = this.layout.getHeaderHeight() + 6 + 28;
        int listBottom = this.height - 33;
        String query = this.searchBox.getValue() == null ? "" : this.searchBox.getValue().toLowerCase();
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
        // Per-frame visibility/active update to ensure proper clickability after scroll/resize
        int vpTop = this.layout.getHeaderHeight() + 6 + 28;
        int vpBottom = this.height - 33;
        for (Button b : this.docButtons) {
            boolean vis = b.getY() + b.getHeight() > vpTop && b.getY() < vpBottom;
            b.visible = vis;
            b.active = vis;
        }
    }

    private void refreshDocList() {
        String query = this.searchBox.getValue() == null ? "" : this.searchBox.getValue().toLowerCase();
        this.lastQuery = query;
        for (Button b : this.docButtons) this.removeWidget(b);
        this.docButtons.clear();

        int listTop = this.layout.getHeaderHeight() + 6 + 28;
        int listBottom = this.height - 33;
        int y = listTop - listScroll;
        for (String namespace : DocsService.get().getNamespaces()) {
            DocJson doc = DocsService.get().getDocForNamespace(namespace).orElse(null);
            if (doc == null) continue;
            String titleText = doc.useI18n ? Component.translatable(doc.title).getString() : doc.title;
            String descText = doc.useI18n ? (doc.description == null ? "" : Component.translatable(doc.description).getString()) : (doc.description == null ? "" : doc.description);
            if (!query.isEmpty() && !(titleText.toLowerCase().contains(query) || descText.toLowerCase().contains(query))) continue;

            Component label = Component.literal(titleText);
            Button.Builder builder = Button.builder(label, b -> Minecraft.getInstance().setScreen(new NamespaceScreen(this, this.options, namespace)));
            if (doc.tooltip != null && !doc.tooltip.isEmpty()) {
                Component tip = doc.useI18n ? Component.translatable(doc.tooltip) : Component.literal(doc.tooltip);
                builder = builder.tooltip(Tooltip.create(tip));
            }
            Button button = new IconListButton(this.width / 2 - 100, y, 200, 20,
                    Component.literal(titleText), doc.iconType, doc.icon,
                    b -> Minecraft.getInstance().setScreen(new NamespaceScreen(this, this.options, namespace)));
            button.visible = y + 20 > listTop && y < listBottom;
            button.active = button.visible;
            this.docButtons.add(button);
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
        int listTop = this.layout.getHeaderHeight() + 6 + 28;
        int listBottom = this.height - 33;
        int listLeft = this.width / 2 - 100;
        int listRight = this.width / 2 + 100;
        if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom && listContentHeight > (listBottom - listTop)) {
            int maxScroll = Math.max(0, listContentHeight - (listBottom - listTop));
            listScroll -= (int)(deltaY * 12);
            if (listScroll < 0) listScroll = 0;
            if (listScroll > maxScroll) listScroll = maxScroll;
            this.setFocused(null);
            this.refreshDocList();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
}


