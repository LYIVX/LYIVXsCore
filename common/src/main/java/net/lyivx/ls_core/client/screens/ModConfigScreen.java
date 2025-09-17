package net.lyivx.ls_core.client.screens;

import net.lyivx.ls_core.LYIVXsCore;
import net.lyivx.ls_core.client.screens.widgets.CustomOptionsList;
import net.lyivx.ls_core.common.config.ILyivxConfigProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModConfigScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("config.ls_core.title");
    private List<Button> categoryButtons;
    private CustomOptionsList list;
    private ILyivxConfigProvider currentProvider;
    private final Screen lastScreen;
    // Options are provided via superclass; we do not need a field here

    public ModConfigScreen(Screen lastScreen, Options options, String initialProviderModId) {
        super(lastScreen, options, TITLE);
        this.lastScreen = lastScreen;
        this.categoryButtons = new ArrayList<>();
        this.currentProvider = findProvider(initialProviderModId)
                .orElse(LYIVXsCore.getConfigProviders().isEmpty() ? null : LYIVXsCore.getConfigProviders().get(0));
    }

    private int getCategorySectionWidth() {
        return Math.max(155, this.width / 4);
    }

    private Optional<ILyivxConfigProvider> findProvider(String modId) {
        if (modId == null) return Optional.empty();
        return LYIVXsCore.getConfigProviders().stream()
                .filter(p -> modId.equals(p.getModId()))
                .findFirst();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        ILyivxConfigProvider providerBeforeResize = this.currentProvider;

        super.resize(minecraft, width, height);

        int contentWidth = this.width - getCategorySectionWidth();
        this.list = new CustomOptionsList(this.minecraft, contentWidth, this);
        this.list.setX(getCategorySectionWidth());
        this.addWidget(this.list);

        this.currentProvider = findProvider(providerBeforeResize != null ? providerBeforeResize.getModId() : null)
                .orElse(LYIVXsCore.getConfigProviders().isEmpty() ? null : LYIVXsCore.getConfigProviders().get(0));

        switchCategory(this.currentProvider);
    }

    @Override
    protected void init() {
        super.init();

        int categorySectionWidth = getCategorySectionWidth();

        int contentWidth = this.width - categorySectionWidth;

        this.list = new CustomOptionsList(this.minecraft, contentWidth, this);
        this.list.setX(categorySectionWidth);
        this.addWidget(this.list);

        addOptions();
        switchCategory(this.currentProvider);
        addFooter();
    }

    @Override
    protected void addOptions() {
        createCategoryButtons();
    }

    private void createCategoryButtons() {
        int categorySectionWidth = getCategorySectionWidth();
        int buttonWidth = Math.min(125, categorySectionWidth - 30);
        int buttonHeight = 20;
        int startX = (categorySectionWidth - buttonWidth) / 2;
        int startY = 40;
        int yOffset = 0;

        this.categoryButtons.clear();

        List<ILyivxConfigProvider> providers = LYIVXsCore.getConfigProviders();

        for (ILyivxConfigProvider provider : providers) {
            ILyivxConfigProvider currentLoopProvider = provider;
            Button categoryButton = Button.builder(
                            Component.literal(provider.getConfigCategoryName()),
                            button -> switchCategory(currentLoopProvider))
                    .bounds(startX, startY + yOffset, buttonWidth, buttonHeight)
                    .build();
            this.categoryButtons.add(this.addRenderableWidget(categoryButton));
            yOffset += buttonHeight + 5;
        }

        updateCategoryButtonSelection();
    }

    private void updateCategoryButtonSelection() {
        for (Button button : this.categoryButtons) {
            String buttonText = button.getMessage().getString();
            boolean isActive = this.currentProvider != null && buttonText.equals(this.currentProvider.getConfigCategoryName());
            button.active = !isActive;
        }
    }

    protected void addFooter() {
        LinearLayout linearLayout = (LinearLayout)this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearLayout.addChild(Button.builder(Component.translatable("config.ls_core.reset"), button -> resetConfig()).build());
        linearLayout.addChild(Button.builder(Component.translatable("screen.ls_core.docs.title"), button -> this.minecraft.setScreen(new net.lyivx.ls_core.client.screens.docs.DocsScreen(this, this.minecraft.options))).build());
        linearLayout.addChild(Button.builder(Component.translatable("gui.done"), button -> this.minecraft.setScreen(this.lastScreen)).build());
    }

    private void switchCategory(ILyivxConfigProvider provider) {
        this.currentProvider = provider;
        this.list.children().clear();
        this.list.setScrollAmount(0);

        if (this.currentProvider != null) {
            this.list.addTitle(Component.literal(this.currentProvider.getConfigCategoryName()));
            this.currentProvider.addConfigOptions(this, this.list);
        } else {
            this.list.addTitle(Component.literal("No Configuration Available"));
        }

        updateCategoryButtonSelection();
    }

    private void resetConfig() {
        if (this.currentProvider != null) {
            LYIVXsCore.LOGGER.info("Resetting config for: {}", this.currentProvider.getModId());
            this.currentProvider.resetConfigDefaults();

            this.list.children().clear();
            this.list.setScrollAmount(0);
            this.list.addTitle(Component.literal(this.currentProvider.getConfigCategoryName()));
            this.currentProvider.addConfigOptions(this, this.list);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int categorySectionWidth = getCategorySectionWidth();
        int topPadding = 33; // Standard top padding below title
        int bottomPadding = 33; // Standard bottom padding above footer buttons

        this.list.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw the vertical dividing line, respecting padding
        guiGraphics.fill(
                categorySectionWidth - 1,      // x1
                topPadding,                  // y1 (Start below header area)
                categorySectionWidth,          // x2
                this.height - bottomPadding, // y2 (End above footer area)
                0x75000000                   // Color
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.list != null) {
            for (CustomOptionsList.Entry entry : this.list.children()) {
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.list != null) {
            for (CustomOptionsList.Entry entry : this.list.children()) {
                if (entry.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.list != null) {
            for (CustomOptionsList.Entry entry : this.list.children()) {
                if (entry.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}