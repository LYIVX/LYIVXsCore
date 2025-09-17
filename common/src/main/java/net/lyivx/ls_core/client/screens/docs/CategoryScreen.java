package net.lyivx.ls_core.client.screens.docs;

import net.lyivx.ls_core.client.screens.ListSubScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.lyivx.ls_core.docs.DocsService;
import net.lyivx.ls_core.docs.model.EntryJson;
import net.lyivx.ls_core.client.screens.docs.widgets.IconListButton;
import net.lyivx.ls_core.client.screens.docs.EntrySplitScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Screen listing entries for a specific category within a namespace.
 */
public class CategoryScreen extends ListSubScreen {
	private final Screen lastScreen;
	private final String namespace;
	private final String categoryId;
	private final List<Button> entryButtons = new ArrayList<>();
	private EditBox searchBox;
	private String lastQuery = null;
	private boolean showSearch = true;
	private int listScroll = 0;
	private int listContentHeight = 0;

	public CategoryScreen(Screen lastScreen, Options options, String namespace, String categoryId) {
		super(lastScreen, options, titleFor(namespace, categoryId));
		this.lastScreen = lastScreen;
		this.namespace = namespace;
		this.categoryId = categoryId;
	}

	private static Component titleFor(String namespace, String categoryId) {
		var meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
		var cat = DocsService.get().getCategory(namespace, categoryId).orElse(null);
		String raw = cat != null && cat.title != null ? cat.title : categoryId;
		return (meta != null && meta.useI18n) ? Component.translatable(raw) : Component.literal(raw);
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
		// Determine search visibility: category override else doc-level
		var meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
		var catObj = DocsService.get().getCategory(namespace, categoryId).orElse(null);
		this.showSearch = catObj != null && catObj.searchbar != null ? catObj.searchbar : (meta == null || meta.searchbar);
		if (this.showSearch) {
			int headerY = this.layout.getHeaderHeight();
			this.searchBox = new EditBox(this.font, this.width / 2 - 100, headerY + 6, 200, 20, Component.translatable("screen.ls_core.search"));
			this.searchBox.setResponder(q -> refreshEntries());
			this.addRenderableWidget(this.searchBox);
		} else {
			this.searchBox = null;
		}

		List<EntryJson> entries = new ArrayList<>();
		for (EntryJson e : DocsService.get().getEntries(namespace)) {
			if (categoryId.equals(e.category)) entries.add(e);
		}
		entries.sort(Comparator.comparing(e -> e.title == null ? "" : e.title));
		refreshEntries();
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
		for (Button b : this.entryButtons) {
			boolean vis = b.getY() + b.getHeight() > vpTop && b.getY() < vpBottom;
			b.visible = vis;
			b.active = vis;
		}
	}

	private void refreshEntries() {
		String query = (this.searchBox == null || this.searchBox.getValue() == null) ? "" : this.searchBox.getValue().toLowerCase();
		this.lastQuery = query;
		for (Button b : this.entryButtons) this.removeWidget(b);
		this.entryButtons.clear();

		int listTop = (this.showSearch ? this.layout.getHeaderHeight() + 6 + 28 : this.layout.getHeaderHeight() + 6);
		int listBottom = this.height - 33;
		int y = listTop - listScroll;
		List<EntryJson> entries = new ArrayList<>();
		for (EntryJson e : DocsService.get().getEntries(namespace)) {
			if (categoryId.equals(e.category)) entries.add(e);
		}
		entries.sort(Comparator.comparing(e -> e.title == null ? "" : e.title));

		for (EntryJson e : entries) {
			String rawTitle = e.title == null ? e.id : e.title;
			var meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
			String titleText = (meta != null && meta.useI18n) ? Component.translatable(rawTitle).getString() : rawTitle;
			if (!query.isEmpty() && !titleText.toLowerCase().contains(query)) continue;
			Button button = new IconListButton(this.width / 2 - 100, y, 200, 20,
					Component.literal(titleText), e.iconType, e.icon,
					b -> this.minecraft.setScreen(new EntrySplitScreen(this, this.minecraft.options, namespace, e.id)));
			if (e.tooltip != null && !e.tooltip.isEmpty()) {
				var tip = (meta != null && meta.useI18n) ? Component.translatable(e.tooltip) : Component.literal(e.tooltip);
				button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tip));
			}
			button.visible = y + 20 > listTop && y < listBottom;
			button.active = button.visible;
			this.entryButtons.add(button);
			this.addRenderableWidget(button);
			y += 24;
		}
		this.listContentHeight = Math.max(0, y - (listTop - listScroll));
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
			this.refreshEntries();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		this.lastQuery = null;
		super.resize(minecraft, width, height);
	}
}


