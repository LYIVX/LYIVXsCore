package net.lyivx.ls_core.client.screens.docs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.lyivx.ls_core.client.screens.docs.renderer.SectionRenderer;
import net.lyivx.ls_core.client.screens.docs.renderer.SectionLink;
import net.lyivx.ls_core.docs.DocsService;
import net.lyivx.ls_core.docs.model.EntryJson;
import net.lyivx.ls_core.docs.model.CategoryJson;
import net.lyivx.ls_core.client.screens.docs.widgets.IconListButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Entry screen with page navigation and left navigation.
 */
public class EntryScreen extends OptionsSubScreen {
	private final Screen lastScreen;
	private final String namespace;
	private final String entryId;
	private int currentPageIndex = 0;
	private boolean hasPrev;
	private boolean hasNext;
	private Button prevArrow;
	private Button nextArrow;

	// Left navigation
	private final int leftNavWidth = 155;
	private final List<Button> navButtons = new ArrayList<>();
	private String openCategoryId;
	private int navScroll = 0;
	private int navContentHeight = 0;

	// Page rendering state
	private List<SectionLink> currentLinks = new ArrayList<>();

	public EntryScreen(Screen lastScreen, Options options, String namespace, String entryId) {
		super(lastScreen, options, titleFor(namespace, entryId));
		this.lastScreen = lastScreen;
		this.namespace = namespace;
		this.entryId = entryId;
	}

	@Override
	protected void addOptions() {

	}

	@Override
	protected void addContents() {
		super.addContents();

		var entry = DocsService.get().getEntry(namespace, entryId).orElse(null);
		if (this.openCategoryId == null && entry != null) this.openCategoryId = entry.category;
		int pageCount = entry != null && entry.pages != null ? entry.pages.size() : 0;
		if (pageCount <= 0) currentPageIndex = 0; else if (currentPageIndex >= pageCount) currentPageIndex = pageCount - 1; else if (currentPageIndex < 0) currentPageIndex = 0;
		this.hasPrev = pageCount > 0 && currentPageIndex > 0;
		this.hasNext = pageCount > 0 && currentPageIndex < pageCount - 1;
		rebuildLeftNav(entry);
	}

	private static Component titleFor(String namespace, String entryId) {
		var meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
		var entry = DocsService.get().getEntry(namespace, entryId).orElse(null);
		String raw = entry != null && entry.title != null ? entry.title : entryId;
		return (meta != null && meta.useI18n) ? Component.translatable(raw) : Component.literal(raw);
	}

	@Override
	protected void addTitle() {
		super.addTitle();
		this.repositionElements();
	}

	@Override
	protected void init() {
		this.clearWidgets();
		super.init();
		refreshPageArrows();
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
	protected void addFooter() {
		LinearLayout footer = (LinearLayout)this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(Component.translatable("gui.back"), b -> this.minecraft.setScreen(this.lastScreen)).build());
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

		guiGraphics.fill(leftNavWidth - 1, 33, leftNavWidth, this.height - 33, 0x75000000);

		int contentX = leftNavWidth + 10;
		int contentY = this.layout.getHeaderHeight() + 6;
		int contentWidth = this.width - leftNavWidth - 30;
		int colWidth = (contentWidth - 10) / 2;
		int leftColX = contentX;
		int rightColX = contentX + colWidth + 10;
		int leftY = contentY;
		int rightY = contentY;
		this.currentLinks.clear();
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
			for (int i = 0; i < sections.size(); i++) {
				EntryJson.Section s = sections.get(i);
				if (i % 2 == 0) {
					var res = SectionRenderer.renderSection(guiGraphics, leftColX, leftY, colWidth, this.font.lineHeight, s, i18n);
					leftY = res.nextY + 6;
					this.currentLinks.addAll(res.links);
					hoverItems.addAll(res.hoverItems);
				} else {
					var res = SectionRenderer.renderSection(guiGraphics, rightColX, rightY, colWidth, this.font.lineHeight, s, i18n);
					rightY = res.nextY + 6;
					this.currentLinks.addAll(res.links);
					hoverItems.addAll(res.hoverItems);
				}
			}
			int contentBottom = (this.height - this.layout.getFooterHeight()) - 6; // align with left nav bounds
			guiGraphics.fill(contentX + colWidth + 5, contentY, contentX + colWidth + 6, contentBottom, 0x55000000);

			// Render tooltips for hovered items (16x16 slot at each recorded x,y)
			for (SectionRenderer.HoverItem hi : hoverItems) {
				if (mouseX >= hi.x && mouseX <= hi.x + 16 && mouseY >= hi.y && mouseY <= hi.y + 16) {
					guiGraphics.renderTooltip(this.font, hi.stack, mouseX, mouseY);
					break; // show only one tooltip at a time
				}
			}
		}

		// Draw left nav scrollbar if needed
		int navViewport = Math.max(0, navBottom - navTop);
		if (navContentHeight > navViewport) {
			int trackX1 = leftNavWidth - 4;
			int trackX2 = leftNavWidth - 2;
			guiGraphics.fill(trackX1, navTop, trackX2, navBottom, 0x33000000);
			float ratio = (float)navViewport / (float)navContentHeight;
			int thumbH = Math.max(12, (int)(navViewport * ratio));
			int maxScroll = Math.max(0, navContentHeight - navViewport);
			int thumbY = navTop + (maxScroll == 0 ? 0 : (int)((float)navScroll / (float)maxScroll * (navViewport - thumbH)));
			guiGraphics.fill(trackX1, thumbY, trackX2, thumbY + thumbH, 0x99000000);
		}
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

	private void rebuildLeftNav(EntryJson current) {
		for (Button b : this.navButtons) this.removeWidget(b);
		this.navButtons.clear();
		int navTop = this.layout.getHeaderHeight() + 6;
		int navBottom = (this.height - this.layout.getFooterHeight()) - 6;
		// Ensure scroll position is valid before positioning buttons
		if (navContentHeight > 0) {
			int viewportHeight = navBottom - navTop;
			if (navContentHeight > viewportHeight) {
				int maxScroll = Math.max(0, navContentHeight - viewportHeight);
				navScroll = Math.max(0, Math.min(navScroll, maxScroll));
			} else {
				navScroll = 0;
			}
		}
		
		int y = navTop - navScroll;
		var meta = DocsService.get().getDocForNamespace(namespace).orElse(null);
		List<CategoryJson> categories = new ArrayList<>(DocsService.get().getCategories(namespace));
		categories.sort(Comparator.comparing(c -> c.title == null ? c.id : c.title));
		for (CategoryJson cat : categories) {
			// Check visibility BEFORE creating the button
			boolean wouldBeVisible = y + 20 > navTop && y < navBottom;
			boolean isOpen = cat.id != null && cat.id.equals(this.openCategoryId);
			
			if (wouldBeVisible) {
				String catTitleRaw = cat.title == null ? cat.id : cat.title;
				String catTitle = (meta != null && meta.useI18n) ? Component.translatable(catTitleRaw).getString() : catTitleRaw;
				String arrow = isOpen ? "v " : "> ";
				Button catButton = new IconListButton(10, y, leftNavWidth - 20, 20,
						Component.literal(arrow + catTitle), cat.iconType, cat.icon,
						b -> {
							this.openCategoryId = isOpen ? null : cat.id;
							this.setFocused(null);
							rebuildLeftNav(current);
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
					// Check visibility BEFORE creating the button
					boolean entryVisible = y + 20 > navTop && y < navBottom;
					if (entryVisible) {
						String raw = e.title == null ? e.id : e.title;
						String title = (meta != null && meta.useI18n) ? Component.translatable(raw).getString() : raw;
						boolean isCurrent = current != null && current.id != null && current.id.equals(e.id);
						Button entryButton = new IconListButton(20, y - 5, leftNavWidth - 30, 20,
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
		this.navContentHeight = Math.max(0, y - (navTop - navScroll));
		
		// Ensure scroll position is valid after rebuilding
		if (navContentHeight > 0) {
			int viewportHeight = navBottom - navTop;
			if (navContentHeight > viewportHeight) {
				int maxScroll = Math.max(0, navContentHeight - viewportHeight);
				navScroll = Math.max(0, Math.min(navScroll, maxScroll));
			} else {
				navScroll = 0;
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		int navTop = this.layout.getHeaderHeight() + 6;
		int navBottom = (this.height - this.layout.getFooterHeight()) - 6;
		int viewportHeight = navBottom - navTop;
		
		// Only allow scrolling if there's actually content to scroll
		if (mouseX >= 0 && mouseX <= leftNavWidth && mouseY >= navTop && mouseY <= navBottom && navContentHeight > viewportHeight) {
			int maxScroll = Math.max(0, navContentHeight - viewportHeight);
			int oldScroll = navScroll;
			
			// Apply scroll delta
			navScroll -= (int)(deltaY * 12);
			
			// Clamp scroll position - this is crucial for preventing over-scrolling
			navScroll = Math.max(0, Math.min(navScroll, maxScroll));
			
			// Only rebuild if scroll actually changed
			if (oldScroll != navScroll) {
				this.setFocused(null);
				rebuildLeftNav(DocsService.get().getEntry(namespace, entryId).orElse(null));
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		// Reset scroll position when resizing to prevent layout issues
		navScroll = 0;
		super.resize(minecraft, width, height);
	}
}
