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
import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.registry.Registries;
import com.mojang.serialization.MapCodec;

import java.util.List;
import java.util.ArrayList;

public class TrophyPedestalBlock extends BlockWithEntity {
    
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
        TrophyPedestalBlockEntity blockEntity = (TrophyPedestalBlockEntity) world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.hasTrophy()) {
            ItemStack trophy = blockEntity.getTrophy();
            dropStack(world, pos, trophy);
            
            // Remover Trophy Display Block de encima solo si es un trofeo
            if (isTrophy(trophy)) {
                BlockPos displayPos = pos.up();
                if (world.getBlockState(displayPos).getBlock() == ModBlocks.TROPHY_DISPLAY_BLOCK) {
                    world.removeBlock(displayPos, false);
                }
            }
        }
        if (!world.isClient && !player.isCreative()) {
            dropStack(world, pos, new ItemStack(this));
        }
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        TrophyPedestalBlockEntity blockEntity = (TrophyPedestalBlockEntity) world.getBlockEntity(pos);
        if (blockEntity == null) {
            return ActionResult.FAIL;
        }

        ItemStack heldItem = player.getStackInHand(Hand.MAIN_HAND);
        
        // Si el jugador tiene las manos vacías, intenta quitar el item
        if (heldItem.isEmpty() && blockEntity.hasTrophy()) {
            ItemStack trophy = blockEntity.removeTrophy();
            
            // Remover Trophy Display Block de encima solo si era un trofeo
            if (isTrophy(trophy)) {
                BlockPos displayPos = pos.up();
                if (world.getBlockState(displayPos).getBlock() == ModBlocks.TROPHY_DISPLAY_BLOCK) {
                    world.removeBlock(displayPos, false);
                }
            }
            
            player.giveItemStack(trophy);
            String itemType = isTrophy(trophy) ? "Trophy" : "Pokéball";
            String positionName = getPositionName(world, pos);
            player.sendMessage(Text.literal(itemType + " removed from pedestal " + positionName + "."), false);
            return ActionResult.SUCCESS;
        }
        
        // Si tiene un trofeo o pokéball en la mano y el pedestal está vacío
        if ((isTrophy(heldItem) || isPokeball(heldItem)) && !blockEntity.hasTrophy()) {
            ItemStack trophyToPlace = heldItem.copy();
            trophyToPlace.setCount(1);
            blockEntity.setTrophy(trophyToPlace);
            
            // Crear Trophy Display Block encima solo para trofeos
            if (isTrophy(trophyToPlace)) {
                BlockPos displayPos = pos.up();
                if (world.getBlockState(displayPos).isAir()) {
                    world.setBlockState(displayPos, ModBlocks.TROPHY_DISPLAY_BLOCK.getDefaultState());
                    if (world.getBlockEntity(displayPos) instanceof com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity displayEntity) {
                        // Configurar el Trophy Display con los datos del trofeo
                        String trophyId = "poke-notifier:" + getTrophyId(trophyToPlace);
                        displayEntity.setTrophyData(trophyId, player.getUuidAsString());
                    }
                }
            }
            
            if (!player.getAbilities().creativeMode) {
                heldItem.decrement(1);
            }
            
            String itemType = isTrophy(trophyToPlace) ? "Trophy" : "Pokéball";
            String positionName = getPositionName(world, pos);
            player.sendMessage(Text.literal(itemType + " placed on pedestal " + positionName + "!"), false);
            return ActionResult.SUCCESS;
        }
        
        // Si el pedestal ya tiene un item
        if (blockEntity.hasTrophy()) {
            String itemType = isTrophy(blockEntity.getTrophy()) ? "trophy" : "Pokéball";
            player.sendMessage(Text.literal("This pedestal already has a " + itemType + "."), false);
            return ActionResult.FAIL;
        }
        
        // Si no es un trofeo o pokéball
        player.sendMessage(Text.literal("You can only place trophies or Pokéballs on this pedestal."), false);
        return ActionResult.FAIL;
    }
    
    private boolean isTrophy(ItemStack item) {
        if (item.isEmpty()) return false;
        
        String itemName = item.getName().getString();
        return itemName.contains("Trophy");
    }
    
    private String getTrophyId(ItemStack trophy) {
        String itemId = Registries.ITEM.getId(trophy.getItem()).getPath();
        return itemId; // Returns something like "kanto_trophy"
    }
    
    private boolean isPokeball(ItemStack item) {
        if (item.isEmpty()) return false;
        
        String itemId = Registries.ITEM.getId(item.getItem()).toString();
        return itemId.startsWith("cobblemon:") && itemId.contains("_ball");
    }
    
    private String getPositionName(World world, BlockPos pedestalPos) {
        // Find nearby altar within 5 blocks
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos altarPos = pedestalPos.add(x, 0, z);
                if (world.getBlockEntity(altarPos) instanceof TrophyAltarBlockEntity altar) {
                    return altar.getPositionName(pedestalPos);
                }
            }
        }
        return "";
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyPedestalBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }
}