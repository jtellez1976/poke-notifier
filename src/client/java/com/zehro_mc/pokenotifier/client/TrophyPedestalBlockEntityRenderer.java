/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.block.TrophyPedestalBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class TrophyPedestalBlockEntityRenderer implements BlockEntityRenderer<TrophyPedestalBlockEntity> {
    private final ItemRenderer itemRenderer;

    public TrophyPedestalBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TrophyPedestalBlockEntity blockEntity, float tickDelta, MatrixStack matrices, 
                      VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ItemStack trophy = blockEntity.getTrophy();
        if (trophy.isEmpty()) {
            return;
        }

        matrices.push();
        
        // Position the trophy on top of the pedestal
        matrices.translate(0.5, 1.2, 0.5);
        
        // Rotate the trophy slowly
        long time = blockEntity.getWorld().getTime();
        float rotation = (time + tickDelta) * 2.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        
        // Scale the trophy slightly
        matrices.scale(0.8f, 0.8f, 0.8f);
        
        // Render the trophy
        itemRenderer.renderItem(trophy, ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, blockEntity.getWorld(), 0);
        
        matrices.pop();
    }
}