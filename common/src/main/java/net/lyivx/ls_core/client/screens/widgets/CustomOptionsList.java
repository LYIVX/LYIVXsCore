package net.lyivx.ls_core.client.screens.widgets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class CustomOptionsList extends ContainerObjectSelectionList<CustomOptionsList.Entry> {
    private static final int DEFAULT_ITEM_HEIGHT = 25;
    private final OptionsSubScreen screen;

    public CustomOptionsList(Minecraft minecraft, int width, OptionsSubScreen screen) {
        super(minecraft, width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), DEFAULT_ITEM_HEIGHT);
        this.centerListVertically = false;
        this.screen = screen;
    }

    public void addBig(OptionInstance<?> option) {
        this.addEntry(OptionEntry.big(this.minecraft.options, option, this.screen));
    }

    public void addSmall(OptionInstance<?>... options) {
        for (int i = 0; i < options.length; i += 2) {
            OptionInstance<?> optionInstance = i < options.length - 1 ? options[i + 1] : null;
            this.addEntry(OptionEntry.small(this.minecraft.options, options[i], optionInstance, this.screen));
        }
    }

    public void addSmall(List<AbstractWidget> options) {
        for (int i = 0; i < options.size(); i += 2) {
            this.addSmall(options.get(i), i < options.size() - 1 ? options.get(i + 1) : null);
        }
    }

    public void addSmall(AbstractWidget leftOption, @Nullable AbstractWidget rightOption) {
        this.addEntry(Entry.small(leftOption, rightOption, this.screen));
    }

    public void addTitle(Component title) {
        this.addEntry(TitleEntry.create(title, this.screen));
    }

    public int getRowWidth() {
        // Calculate dynamically based on available width
        return (int)(this.width * 0.9); // 90% of available width
    }

    @Nullable
    public AbstractWidget findOption(OptionInstance<?> option) {
        for (Entry entry : this.children()) {
            if (entry instanceof OptionEntry optionEntry) {
                AbstractWidget abstractWidget = optionEntry.options.get(option);
                if (abstractWidget != null) {
                    return abstractWidget;
                }
            }
        }
        return null;
    }

    public Optional<GuiEventListener> getMouseOver(double mouseX, double mouseY) {
        for (Entry entry : this.children()) {
            for (GuiEventListener guiEventListener : entry.children()) {
                if (guiEventListener.isMouseOver(mouseX, mouseY)) {
                    return Optional.of(guiEventListener);
                }
            }
        }
        return Optional.empty();
    }

    @Environment(EnvType.CLIENT)
    protected static class OptionEntry extends Entry {
        final Map<OptionInstance<?>, AbstractWidget> options;

        private OptionEntry(Map<OptionInstance<?>, AbstractWidget> options, OptionsSubScreen screen) {
            super(ImmutableList.copyOf(options.values()), screen);
            this.options = options;
        }

        public static OptionEntry big(Options options, OptionInstance<?> option, OptionsSubScreen screen) {
            int width = screen.width - 155 - 20;  // Account for category width and padding
            return new OptionEntry(ImmutableMap.of(option, option.createButton(options, 0, 0, width)), screen);
        }

        public static OptionEntry small(Options options, OptionInstance leftOption, @Nullable OptionInstance rightOption, OptionsSubScreen screen) {
            AbstractWidget abstractWidget = leftOption.createButton(options, 0, 0, 150);
            return rightOption == null ?
                    new OptionEntry(ImmutableMap.of(leftOption, abstractWidget), screen) :
                    new OptionEntry(ImmutableMap.of(leftOption, abstractWidget, rightOption, rightOption.createButton(options, 0, 0, 150)), screen);
        }
    }

    @Environment(EnvType.CLIENT)
    protected static class TitleEntry extends Entry {
        private final Component title;

        private TitleEntry(Component title, Screen screen) {
            super(ImmutableList.of(), screen);
            this.title = title;
        }

        public static TitleEntry create(Component title, Screen screen) {
            return new TitleEntry(title, screen);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int textWidth = Minecraft.getInstance().font.width(this.title);
            int x = left + (width - textWidth) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, this.title, x, top + 5, 0xFFFFFF);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private final List<AbstractWidget> children;
        private final Screen screen;
        protected final Minecraft minecraft = Minecraft.getInstance();

        Entry(List<AbstractWidget> children, Screen screen) {
            this.children = ImmutableList.copyOf(children);
            this.screen = screen;
        }

        public static Entry big(List<AbstractWidget> options, Screen screen) {
            return new Entry(options, screen);
        }

        public static Entry small(AbstractWidget leftOption, @Nullable AbstractWidget rightOption, Screen screen) {
            return rightOption == null ?
                    new Entry(ImmutableList.of(leftOption), screen) :
                    new Entry(ImmutableList.of(leftOption, rightOption), screen);
        }

        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int totalWidth = width - 20; // 10px padding on each side
            int widgetWidth = this.children.size() > 1 ? totalWidth / 2 - 10 : totalWidth;

            int x = left + 10; // Start with padding

            for (AbstractWidget widget : this.children) {
                widget.setWidth(widgetWidth);
                widget.setPosition(x, top + 3);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
                x += widgetWidth + 20; // Add spacing between widgets
            }
        }

        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }
    }

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
    }
}