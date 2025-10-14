/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.renderer;

import com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders the 3D model of the Pokédex trophy item inside the TrophyDisplayBlock.
 */
public class TrophyDisplayBlockEntityRenderer implements BlockEntityRenderer<TrophyDisplayBlockEntity> {

    public TrophyDisplayBlockEntityRenderer(BlockEntityRendererFactory.Context context) {}

    @Override
    public void render(TrophyDisplayBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        String trophyId = entity.getTrophyId();

        if (trophyId == null || trophyId.isEmpty()) {
            return;
        }

        ItemStack stack = new ItemStack(Registries.ITEM.get(Identifier.of(trophyId)));
        if (stack.isEmpty()) {
            return;
        }

        matrices.push();
        // Center the item in the block and raise it slightly.
        matrices.translate(0.5, 0.25, 0.5);
        // Make the item rotate slowly on its Y-axis.
        float angle = (entity.getWorld().getTime() + tickDelta) * 1.5f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));

        itemRenderer.renderItem(stack, ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, entity.getWorld(), 0);

        matrices.pop();
    }
}