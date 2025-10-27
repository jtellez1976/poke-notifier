/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class TrophyPedestalBlock extends Block {
    
    public TrophyPedestalBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Define la forma del bloque para colisiones y renderizado
        return VoxelShapes.union(
            VoxelShapes.cuboid(0.125, 0.0, 0.125, 0.875, 0.125, 0.875), // Base
            VoxelShapes.cuboid(0.3125, 0.1875, 0.3125, 0.6875, 0.9375, 0.6875), // Pilar
            VoxelShapes.cuboid(0.125, 1.0, 0.125, 0.875, 1.125, 0.875) // Top
        );
    }
    
    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            ItemStack heldItem = player.getStackInHand(Hand.MAIN_HAND);
            
            // Verificar si el jugador tiene un trofeo
            if (isTrophy(heldItem)) {
                player.sendMessage(Text.literal("¡Trofeo detectado! " + heldItem.getName().getString()));
                return ActionResult.SUCCESS;
            } else {
                player.sendMessage(Text.literal("Necesitas un trofeo para usar este pedestal."));
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.SUCCESS;
    }
    
    private boolean isTrophy(ItemStack item) {
        if (item.isEmpty()) return false;
        
        String itemName = item.getName().getString();
        return itemName.contains("Trophy") && itemName.contains("Generation");
    }
}