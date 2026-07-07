package ca.favro.vega.common.gui.components;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.text.DecimalFormat;


public class SliderButton extends AbstractSliderButton {
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private final float minValue;
    private final float maxValue;
    private final String name;
    private final boolean integer;
    private float _value;

    public SliderButton(int x, int y, int width, float maxValue, float minValue, String name, float value, boolean integer) {
        super(x, y, width, 20, Component.literal(name), (value - minValue) / (maxValue - minValue));

        this.maxValue = maxValue;
        this.minValue = minValue;
        this._value = value;
        this.name = name;
        this.integer = integer;

        updateMessage();
    }

    @Override
    public void applyValue() {
        this._value = this.integer ? (float) Mth.floor(Mth.clampedLerp(this.value, this.minValue, this.maxValue)) : (float) Mth.clampedLerp(this.value, this.minValue, this.maxValue);
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(String.format("%s: %s", this.name, decimalFormat.format(this._value))));
    }

    public void onClick(double d, double e) {
    }

    public void onRelease(double d, double e) {
    }

    public float getValue() {
        return this._value;
    }
}