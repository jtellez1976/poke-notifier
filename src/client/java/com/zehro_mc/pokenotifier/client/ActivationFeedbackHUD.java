/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.client.compat.AdvancementPlaquesCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Manages and renders a temporary feedback HUD on the screen for actions like enabling/disabling modes.
 * It uses AdvancementPlaques for a better visual integration if available; otherwise, it falls back
 * to its own rendering system.
 */
public class ActivationFeedbackHUD {

    private static Text message;
    private static Formatting color;
    private static long displayUntil = -1L;

    /** A flag to check only once if the compatibility mod is present. */
    private static final boolean plaquesModLoaded = FabricLoader.getInstance().isModLoaded("advancementplaques");

    public static void show(Text message, boolean isActivation) {
        if (plaquesModLoaded) {
            AdvancementPlaquesCompat.showPlaque(message, isActivation);
        } else {
            // Fallback to our custom HUD system if AdvancementPlaques is not installed.
            ActivationFeedbackHUD.message = message;
            ActivationFeedbackHUD.color = isActivation ? Formatting.GREEN : Formatting.RED;
            ActivationFeedbackHUD.displayUntil = System.currentTimeMillis() + 3000L; // Display message for 3 seconds.
        }
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (plaquesModLoaded || System.currentTimeMillis() > displayUntil || message == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Animation logic for the fade-out effect.
        long timeRemaining = displayUntil - System.currentTimeMillis();
        float alpha = 1.0f;
        if (timeRemaining < 500) { // Fade out in the last half-second.
            alpha = timeRemaining / 500.0f;
        }
        int alphaInt = (int) (alpha * 255);

        int textWidth = client.textRenderer.getWidth(message);
        float scale = 1.5f;
        float scaledTextWidth = textWidth * scale;

        // Center the scaled text on the screen.
        float x = (screenWidth - scaledTextWidth) / 2;
        int y = screenHeight / 4;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        // Draw the text at (0,0) as we have already translated the matrix.
        context.drawTextWithShadow(client.textRenderer, message, 0, 0, color.getColorValue() | (alphaInt << 24));

        context.getMatrices().pop();

        RenderSystem.disableBlend();
    }
}