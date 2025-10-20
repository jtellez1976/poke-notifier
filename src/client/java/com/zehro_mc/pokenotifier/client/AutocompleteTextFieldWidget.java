/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Supplier;

/**
 * A TextFieldWidget that displays autocomplete suggestions.
 */
public class AutocompleteTextFieldWidget extends TextFieldWidget {

    private final Supplier<List<String>> suggestionProvider;
    private List<String> currentSuggestions;

    public AutocompleteTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text, Supplier<List<String>> suggestionProvider) {
        super(textRenderer, x, y, width, height, text);
        this.suggestionProvider = suggestionProvider;
        // Set up the listener for text changes.
        this.setChangedListener(this::onTextUpdate);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused && this.getText().isEmpty()) {
            // FIX: Show all suggestions when the empty field is focused.
            this.currentSuggestions = suggestionProvider.get().stream().limit(3).toList();
        } else if (!focused) {
            this.currentSuggestions = null;
        }
    }

    private void onTextUpdate(String text) {
        // This method is now called automatically whenever the text changes.
        if (text == null || text.isEmpty()) {
            // If focused, show all suggestions. Otherwise, clear them.
            currentSuggestions = isFocused() ? suggestionProvider.get().stream().limit(3).toList() : null;
        } else {
            currentSuggestions = suggestionProvider.get().stream()
                    .filter(s -> s.toLowerCase().startsWith(text.toLowerCase()))
                    .limit(3)
                    .toList();
        }
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // The widget itself only renders the text field.
        // The suggestion box is rendered by the parent screen.
        super.renderWidget(context, mouseX, mouseY, delta);
    }

    /**
     * Renders the suggestion box. This should be called from the parent screen's render method
     * to ensure it's drawn on top of other widgets.
     */
    public void renderSuggestions(DrawContext context, int mouseX, int mouseY) {
        if (isFocused() && this.visible && currentSuggestions != null && !currentSuggestions.isEmpty()) {
            int boxX = getX(); // Position below the text field
            int boxY = getY() + getHeight() + 2;
            int boxWidth = getWidth();
            int maxSuggestions = Math.min(3, currentSuggestions.size());
            int boxHeight = maxSuggestions * (MinecraftClient.getInstance().textRenderer.fontHeight + 1) + 3;

            context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000); // Darker background

            for (int i = 0; i < maxSuggestions; i++) {
                String suggestion = currentSuggestions.get(i);
                int suggestionY = boxY + 1 + i * (MinecraftClient.getInstance().textRenderer.fontHeight + 1);
                boolean isHovered = mouseX >= boxX && mouseX < boxX + boxWidth && mouseY >= suggestionY && mouseY < suggestionY + MinecraftClient.getInstance().textRenderer.fontHeight + 1;

                if (isHovered) context.fill(boxX, suggestionY, boxX + boxWidth, suggestionY + MinecraftClient.getInstance().textRenderer.fontHeight, 0x55FFFFFF);

                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, suggestion, boxX + 3, suggestionY + 1, 0xFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isFocused() && currentSuggestions != null && !currentSuggestions.isEmpty()) {
            int boxX = getX();
            int boxY = getY() + getHeight() + 2;
            int maxSuggestions = Math.min(3, currentSuggestions.size());
            for (int i = 0; i < maxSuggestions; i++) {
                int suggestionY = boxY + 1 + i * (MinecraftClient.getInstance().textRenderer.fontHeight + 1);
                if (mouseX >= boxX && mouseX < boxX + getWidth() && mouseY >= suggestionY && mouseY < suggestionY + MinecraftClient.getInstance().textRenderer.fontHeight + 1) {
                    setText(currentSuggestions.get(i));
                    currentSuggestions = null;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}