/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Renders the HUD that displays the progress of the "Catch 'em All" mode on the screen.
 */
public class CatchEmAllHUD {

    private static final Identifier BUTTON_TEXTURE = Identifier.of("minecraft", "widget/button");

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        // Do not render anything if the mode is not active or there is no information.
        if (PokeNotifierClient.currentCatchEmAllGeneration == null || "none".equals(PokeNotifierClient.currentCatchEmAllGeneration) || PokeNotifierClient.catchTotalCount == 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int margin = 5;
        int boxPadding = 4;

        // Format the progress text.
        String genName = PokeNotifierClient.currentCatchEmAllGeneration.substring(0, 1).toUpperCase() + PokeNotifierClient.currentCatchEmAllGeneration.substring(1);
        Text progressText = Text.literal(genName + ": ").formatted(Formatting.YELLOW)
                .append(Text.literal(PokeNotifierClient.catchCaughtCount + "/" + PokeNotifierClient.catchTotalCount).formatted(Formatting.GREEN));

        int textWidth = client.textRenderer.getWidth(progressText);
        int boxWidth = textWidth + (boxPadding * 2);
        int boxHeight = client.textRenderer.fontHeight + (boxPadding * 2);

        // Position the HUD in the bottom-left corner, adjusting if the chat is open.
        ChatHud chatHud = client.inGameHud.getChatHud();
        int chatHeight = chatHud.isChatFocused() ? chatHud.getHeight() : 0;
        int boxX = margin;
        int boxY = screenHeight - chatHeight - boxHeight - margin;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw the HUD background using a Minecraft button texture for a native look and feel.
        context.drawGuiTexture(BUTTON_TEXTURE, boxX, boxY, boxWidth, boxHeight);

        // Draw the progress text, scaled down to better fit inside the button.
        float scale = 0.8f;
        int scaledTextWidth = (int) (textWidth * scale);

        // Calculate the new position to center the scaled text.
        int textX = boxX + (boxWidth - scaledTextWidth) / 2;
        int textY = boxY + (boxHeight - (int)(client.textRenderer.fontHeight * scale)) / 2;

        context.getMatrices().push();
        context.getMatrices().translate(textX, textY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        // Draw the text at (0,0) as we have already translated the matrix.
        context.drawTextWithShadow(client.textRenderer, progressText, 0, 0, 0xFFFFFF);

        context.getMatrices().pop();

        RenderSystem.disableBlend();
    }
}