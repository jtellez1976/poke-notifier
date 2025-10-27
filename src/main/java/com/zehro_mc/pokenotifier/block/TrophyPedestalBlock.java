/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.List;
import java.util.ArrayList;

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
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.literal("Place trophies to activate blessings").formatted(Formatting.GOLD));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("From: ").formatted(Formatting.GRAY).append(Text.literal("Poke Notifier").formatted(Formatting.AQUA)));
        super.appendTooltip(stack, context, tooltip, options);
    }
    
    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && !player.isCreative()) {
            dropStack(world, pos, new ItemStack(this));
        }
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            ItemStack heldItem = player.getStackInHand(Hand.MAIN_HAND);
            
            // Verificar si el jugador tiene un trofeo
            if (isTrophy(heldItem)) {
                player.sendMessage(Text.literal("Trophy detected! " + heldItem.getName().getString()));
                return ActionResult.SUCCESS;
            } else {
                player.sendMessage(Text.literal("You need a trophy to use this pedestal."));
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