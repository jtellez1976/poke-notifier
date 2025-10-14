/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.util.RankInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * A utility class for rendering rank icons and text in the game world.
 */
public class RankRenderer {

    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 1;
    private static final int TEXT_SPACING = 2;

    /**
     * Draws the rank icon and text for a 2D GUI context.
     * @return The total width drawn.
     */
    public static int drawRank(DrawContext context, int x, int y, RankInfo rankInfo) {
        if (rankInfo == null) return 0;
        // This method is for 2D GUIs, which is not the current focus.
        return 0;
    }

    public static int getRankWidth(RankInfo rankInfo) {
        if (rankInfo == null) return 0;
        int iconsWidth = rankInfo.iconCount() * (ICON_SIZE + ICON_SPACING);
        return iconsWidth + TEXT_SPACING + MinecraftClient.getInstance().textRenderer.getWidth(rankInfo.rank().displayText);
    }

    /**
     * Draws the rank icon and text for a 3D world nametag.
     * @return The total width drawn, used to offset the player's name.
     */
    public static int drawNametagRank(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, RankInfo rankInfo) {
        if (rankInfo == null) return 0;
        
        int currentX = 0;
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        // 1. Draw the rank icons.
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(rankInfo.rank().icon));
        for (int i = 0; i < rankInfo.iconCount(); i++) {
            float x1 = (float)currentX;
            float y1 = -4f; // Vertical adjustment to align with text.
            float x2 = x1 + ICON_SIZE;
            float y2 = y1 + ICON_SIZE;
            float z = 0.0f;

            // Manually draw the quad for the texture.
            vertexConsumer.vertex(positionMatrix, x1, y1, z).color(255, 255, 255, 255).texture(0.0f, 0.0f).light(light);
            vertexConsumer.vertex(positionMatrix, x1, y2, z).color(255, 255, 255, 255).texture(0.0f, 1.0f).light(light);
            vertexConsumer.vertex(positionMatrix, x2, y2, z).color(255, 255, 255, 255).texture(1.0f, 1.0f).light(light);
            vertexConsumer.vertex(positionMatrix, x2, y1, z).color(255, 255, 255, 255).texture(1.0f, 0.0f).light(light);

            currentX += ICON_SIZE + ICON_SPACING;
        }

        // 2. Draw the rank text.
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        textRenderer.draw(rankInfo.rank().displayText, currentX, 0, 0xFFFFFF, false, positionMatrix, vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, light);

        return currentX + textRenderer.getWidth(rankInfo.rank().displayText);
    }
}