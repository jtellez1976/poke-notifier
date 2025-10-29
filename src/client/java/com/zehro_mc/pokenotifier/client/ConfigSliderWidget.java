/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ConfigSliderWidget extends SliderWidget {
    private final String label;
    private final int minValue;
    private final int maxValue;
    private final String suffix;
    private final java.util.function.Consumer<Integer> onValueChanged;

    public ConfigSliderWidget(int x, int y, int width, int height, String label, int currentValue, int minValue, int maxValue, String suffix, java.util.function.Consumer<Integer> onValueChanged) {
        super(x, y, width, height, Text.empty(), (double)(currentValue - minValue) / (maxValue - minValue));
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.suffix = suffix;
        this.onValueChanged = onValueChanged;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        int currentValue = (int)(this.value * (maxValue - minValue)) + minValue;
        setMessage(Text.literal(label + ": " + currentValue + suffix));
    }

    @Override
    protected void applyValue() {
        int currentValue = (int)(this.value * (maxValue - minValue)) + minValue;
        onValueChanged.accept(currentValue);
    }

    public int getCurrentValue() {
        return (int)(this.value * (maxValue - minValue)) + minValue;
    }

    public void setValue(int newValue) {
        this.value = (double)(newValue - minValue) / (maxValue - minValue);
        updateMessage();
    }
}