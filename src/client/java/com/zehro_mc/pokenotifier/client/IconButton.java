/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Button that renders custom icons alongside text
 */
public class IconButton extends ButtonWidget {
    private final Identifier iconId;
    private final String labelText;

    public IconButton(int x, int y, int width, int height, Identifier iconId, String labelText, PressAction onPress) {
        super(x, y, width, height, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.iconId = iconId;
        this.labelText = labelText;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render button background
        super.renderWidget(context, mouseX, mouseY, delta);
        
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        Text labelTextComponent = Text.literal(labelText);
        
        if (iconId != null) {
            // Render icon on the left
            int iconX = this.getX() + 4;
            int iconY = this.getY() + (this.height - 12) / 2;
            GuiIcons.renderIcon(context, iconId, iconX, iconY, 12);
            
            // Render text next to icon
            int textX = this.getX() + 20; // Space for icon + padding
            int textY = this.getY() + (this.height - 8) / 2;
            context.drawText(client.textRenderer, labelTextComponent, textX, textY, 0xFFFFFF, false);
        } else {
            // Render text centered when no icon
            int textWidth = client.textRenderer.getWidth(labelTextComponent);
            int textX = this.getX() + (this.width - textWidth) / 2;
            int textY = this.getY() + (this.height - 8) / 2;
            context.drawText(client.textRenderer, labelTextComponent, textX, textY, 0xFFFFFF, false);
        }
    }
}