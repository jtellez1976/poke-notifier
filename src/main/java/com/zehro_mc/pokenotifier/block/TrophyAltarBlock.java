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
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;

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
    public void onPlaced(World world, BlockPos pos, BlockState state, net.minecraft.entity.LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        
        // Mostrar preview automáticamente al colocar el altar
        if (!world.isClient && world.getBlockEntity(pos) instanceof TrophyAltarBlockEntity blockEntity) {
            // Mostrar inmediatamente sin delay
            blockEntity.showStructurePreview();
            blockEntity.showingPreview = true;
            blockEntity.previewTimer = 0;
            
            if (placer instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                player.sendMessage(net.minecraft.text.Text.literal("🏰 Altar placed! Ghost blocks show required structure").formatted(net.minecraft.util.Formatting.AQUA), false);
                player.sendMessage(net.minecraft.text.Text.literal("🔴 Red particles = Redstone Blocks needed below").formatted(net.minecraft.util.Formatting.RED), false);
                player.sendMessage(net.minecraft.text.Text.literal("🟣 Purple particles = Trophy Pedestals needed").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE), false);
                player.sendMessage(net.minecraft.text.Text.literal("👁 Ghosts will show every 5 seconds until structure is complete").formatted(net.minecraft.util.Formatting.GRAY), false);
            }
        }
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
        
        // Si intenta poner una pokeball en el altar
        if (isPokeball(heldItem) && !blockEntity.hasTrophy()) {
            player.sendMessage(Text.literal("⚠ Pokéballs go on pedestals, not the altar! The altar is only for trophies.").formatted(net.minecraft.util.Formatting.YELLOW), false);
            return ActionResult.FAIL;
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
                // Intentar invocar Pokémon con patrón válido
                String[] pattern = blockEntity.getPokeballPattern();
                if (blockEntity.isValidPattern(pattern)) {
                    player.sendMessage(Text.literal("✓ Summoning ritual ready! Activating...").formatted(net.minecraft.util.Formatting.GREEN), false);
                    blockEntity.attemptPokemonSummon();
                } else {
                    int count = 0;
                    for (String p : pattern) {
                        if (p != null && !p.equals("empty")) count++;
                    }
                    if (count == 0) {
                        player.sendMessage(Text.literal("⚠ No Pokéballs found in pedestals!").formatted(net.minecraft.util.Formatting.YELLOW), false);
                    } else {
                        player.sendMessage(Text.literal("⚠ Invalid pokeball pattern! Found " + count + " pokeballs but no matching Pokemon.").formatted(net.minecraft.util.Formatting.YELLOW), false);
                    }
                    player.sendMessage(Text.literal("ℹ Try different pokeball combinations in specific positions").formatted(net.minecraft.util.Formatting.GRAY), false);
                }
            } else {
                String error = blockEntity.getStructureError();
                player.sendMessage(Text.literal("✗ Multiblock incomplete: " + error).formatted(net.minecraft.util.Formatting.RED), false);
                
                // Mostrar hologramas de bloques faltantes
                blockEntity.showStructurePreview();
                blockEntity.showingPreview = true;
                blockEntity.previewTimer = 0;
                player.sendMessage(Text.literal("👁 Ghost blocks shown - they'll repeat every 5 seconds").formatted(net.minecraft.util.Formatting.GRAY), false);
            }
            return ActionResult.SUCCESS;
        }
        
        // Si no es un trofeo
        player.sendMessage(Text.literal("You can only place Trophies on this altar. Pokéballs go on the pedestals.").formatted(net.minecraft.util.Formatting.YELLOW), false);
        return ActionResult.FAIL;
    }
    
    private boolean isPokeball(ItemStack item) {
        if (item.isEmpty()) return false;
        
        String itemId = net.minecraft.registry.Registries.ITEM.getId(item.getItem()).toString();
        return itemId.startsWith("cobblemon:") && itemId.contains("_ball");
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
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, com.zehro_mc.pokenotifier.block.entity.ModBlockEntities.TROPHY_ALTAR, TrophyAltarBlockEntity::tick);
    }
}