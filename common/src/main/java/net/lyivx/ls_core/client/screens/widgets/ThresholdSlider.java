package net.lyivx.ls_core.client.screens.widgets;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ThresholdSlider extends AbstractSliderButton {

    public static final int MAX_THRESHOLD = 200;
    private final IntSupplier getter;
    private final IntConsumer setter;

    public ThresholdSlider(int x, int y, int width, int height, Component message, IntSupplier getter, IntConsumer setter) {
        super(x, y, width, height, message, Mth.clamp((double) getter.getAsInt() / MAX_THRESHOLD, 0.0, 1.0));
        this.getter = getter;
        this.setter = setter;
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal("Threshold: " + getThreshold()));
    }

    @Override
    protected void applyValue() {
        setter.accept(getThreshold());
    }

    public int getThreshold() {
        return (int) (this.value * MAX_THRESHOLD);
    }
}