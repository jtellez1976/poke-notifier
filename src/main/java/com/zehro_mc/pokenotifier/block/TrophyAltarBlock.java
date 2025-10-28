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
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.registry.Registries;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

public class TrophyAltarBlock extends BlockWithEntity {
    
    public TrophyAltarBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.union(
            VoxelShapes.cuboid(0.0625, 0.0, 0.0625, 0.9375, 0.125, 0.9375), // Base
            VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.875, 0.75, 0.875),    // Pilar central
            VoxelShapes.cuboid(0.0625, 0.75, 0.0625, 0.9375, 1.0, 0.9375)   // Top
        );
    }
    
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        TrophyAltarBlockEntity blockEntity = (TrophyAltarBlockEntity) world.getBlockEntity(pos);
        if (blockEntity == null) {
            return ActionResult.FAIL;
        }

        ItemStack heldItem = player.getStackInHand(Hand.MAIN_HAND);
        
        // Si el jugador tiene las manos vacías, intenta quitar el trofeo
        if (heldItem.isEmpty() && blockEntity.hasTrophy()) {
            ItemStack trophy = blockEntity.removeTrophy();
            player.giveItemStack(trophy);
            player.sendMessage(Text.literal("Trophy removed from altar."), false);
            blockEntity.checkMultiblockStructure();
            return ActionResult.SUCCESS;
        }
        
        // Si tiene un trofeo en la mano y el altar está vacío
        if (isTrophy(heldItem) && !blockEntity.hasTrophy()) {
            ItemStack trophyToPlace = heldItem.copy();
            trophyToPlace.setCount(1);
            blockEntity.setTrophy(trophyToPlace);
            
            if (!player.getAbilities().creativeMode) {
                heldItem.decrement(1);
            }
            
            player.sendMessage(Text.literal("Trophy placed on altar!"), false);
            blockEntity.checkMultiblockStructure();
            return ActionResult.SUCCESS;
        }
        
        // Si el altar ya tiene un trofeo
        if (blockEntity.hasTrophy()) {
            player.sendMessage(Text.literal("This altar already has a trophy."), false);
            return ActionResult.FAIL;
        }
        
        // Si no tiene nada en la mano, verificar estructura manualmente
        if (heldItem.isEmpty()) {
            boolean isValid = blockEntity.validateStructureManually();
            if (isValid) {
                player.sendMessage(Text.literal("✓ Multiblock structure is complete! Trophies: " + blockEntity.getTrophyCount()).formatted(net.minecraft.util.Formatting.GREEN), false);
            } else {
                String error = blockEntity.getStructureError();
                player.sendMessage(Text.literal("✗ Multiblock incomplete: " + error).formatted(net.minecraft.util.Formatting.RED), false);
            }
            return ActionResult.SUCCESS;
        }
        
        // Si no es un trofeo
        player.sendMessage(Text.literal("You can only place trophies on this altar."), false);
        return ActionResult.FAIL;
    }
    
    private boolean isTrophy(ItemStack item) {
        if (item.isEmpty()) return false;
        String itemName = item.getName().getString();
        return itemName.contains("Trophy");
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        TrophyAltarBlockEntity blockEntity = (TrophyAltarBlockEntity) world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.hasTrophy()) {
            dropStack(world, pos, blockEntity.getTrophy());
        }
        if (!world.isClient && !player.isCreative()) {
            dropStack(world, pos, new ItemStack(this));
        }
        return super.onBreak(world, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyAltarBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}