/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.block;

import com.mojang.serialization.MapCodec;
import com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity;
import com.zehro_mc.pokenotifier.component.ModDataComponents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.WorldView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The block that holds and displays a Pokédex trophy. It is visually invisible,
 * with the rendering handled entirely by its associated BlockEntityRenderer.
 */
public class TrophyDisplayBlock extends BlockWithEntity {

    private static final VoxelShape SHAPE = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    public TrophyDisplayBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() { // Required for 1.20.5+
        return createCodec(TrophyDisplayBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyDisplayBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // The block itself is invisible; rendering is handled by the BlockEntityRenderer.
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            if (world.getBlockEntity(pos) instanceof TrophyDisplayBlockEntity be) {
                String ownerUuid = be.getOwnerUuid();
                // Only the owner of the trophy can pick it up.
                if (ownerUuid != null && !ownerUuid.isEmpty() && ownerUuid.equals(player.getUuidAsString())) {
                    String trophyId = be.getTrophyId();
                    Item trophyItem = Registries.ITEM.get(Identifier.of(trophyId));
                    ItemStack trophyStack = new ItemStack(trophyItem);
                    // Restore authenticity data to the item.
                    trophyStack.set(ModDataComponents.OWNER_NAME, player.getName().getString());
                    trophyStack.set(ModDataComponents.OWNER_UUID, player.getUuidAsString());

                    player.getInventory().offerOrDrop(trophyStack);
                    world.removeBlock(pos, false);
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS; // If not the owner, do nothing.
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof TrophyDisplayBlockEntity be) {
            String trophyId = be.getTrophyId();
            if (trophyId != null && !trophyId.isEmpty()) {
                Item trophyItem = Registries.ITEM.get(Identifier.of(trophyId));
                return new ItemStack(trophyItem);
            }
        }
        return super.getPickStack(world, pos, state);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(5) == 0) { // Controls particle frequency.
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
        }
    }
}