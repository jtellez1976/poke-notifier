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
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Renders a large, centered HUD notification on the screen for rare Pokémon spawns.
 * It displays the Pokémon's sprite if available, otherwise it shows a fallback icon.
 */
public class NotificationHUD {

    private static Text title;
    private static Text description;
    private static Identifier icon;
    private static long displayUntil = -1L;

    /** Fallback icon (Poké Ball) if the Pokémon's sprite is not found. */
    private static final Identifier FALLBACK_ICON = Identifier.of("poke-notifier", "textures/gui/pokeball-icon.png");

    public static void show(Text title, Text description, Identifier icon) {
        NotificationHUD.title = title;
        NotificationHUD.description = description;
        NotificationHUD.icon = icon;
        NotificationHUD.displayUntil = System.currentTimeMillis() + 5000L; // Display for 5 seconds.
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (System.currentTimeMillis() > displayUntil || title == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int y = 10; // Posición vertical fija cerca de la parte superior

        // Enable blending for PNG transparency.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Check if the custom Pokémon sprite exists.
        boolean useCustomSprite = client.getResourceManager().getResource(icon).isPresent();

        if (useCustomSprite) {
            // Case 1: The Pokémon sprite exists.
            int iconSize = 64;
            int padding = 0;
            int textWidth = client.textRenderer.getWidth(title);
            int totalWidth = iconSize + padding + textWidth;
            int x = (screenWidth - totalWidth) / 2;

            // Draw the Pokémon sprite.
            context.drawTexture(icon, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

            // Draw the text, vertically centered with the large icon.
            int textY = y + (iconSize / 2) - (client.textRenderer.fontHeight / 2);
            context.drawTextWithShadow(client.textRenderer, title, x + iconSize + padding, textY, 0xFFFFFF);

        } else {
            // Case 2: The Pokémon sprite does not exist, use the fallback.
            int iconSize = 32; // Use a smaller, more appropriate size for the Poké Ball.
            int padding = 4;
            int textWidth = client.textRenderer.getWidth(title);
            int totalWidth = iconSize + padding + textWidth;
            int x = (screenWidth - totalWidth) / 2;

            // Draw the fallback Poké Ball.
            context.drawTexture(FALLBACK_ICON, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

            // Draw the text, vertically centered with the small icon.
            int textY = y + (iconSize / 2) - (client.textRenderer.fontHeight / 2);
            context.drawTextWithShadow(client.textRenderer, title, x + iconSize + padding, textY, 0xFFFFFF);
        }

        RenderSystem.disableBlend();
    }
}