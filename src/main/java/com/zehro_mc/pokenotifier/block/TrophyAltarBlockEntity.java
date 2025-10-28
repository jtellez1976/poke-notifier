/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class TrophyAltarBlockEntity extends BlockEntity {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private boolean isMultiblockComplete = false;
    private int trophyCount = 0;

    public TrophyAltarBlockEntity(BlockPos pos, BlockState state) {
        super(com.zehro_mc.pokenotifier.block.entity.ModBlockEntities.TROPHY_ALTAR, pos, state);
    }

    public ItemStack getTrophy() {
        return inventory.get(0);
    }

    public void setTrophy(ItemStack trophy) {
        inventory.set(0, trophy);
        markDirty();
        if (world != null && !world.isClient) {
            // Crear Trophy Display encima si hay trofeo
            BlockPos displayPos = pos.up();
            if (!trophy.isEmpty() && world.getBlockState(displayPos).isAir()) {
                world.setBlockState(displayPos, ModBlocks.TROPHY_DISPLAY_BLOCK.getDefaultState());
                if (world.getBlockEntity(displayPos) instanceof com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity displayEntity) {
                    String trophyId = "poke-notifier:" + getTrophyId(trophy);
                    displayEntity.setTrophyData(trophyId, "altar");
                }
            }
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public boolean hasTrophy() {
        return !getTrophy().isEmpty();
    }

    public ItemStack removeTrophy() {
        ItemStack trophy = getTrophy();
        setTrophy(ItemStack.EMPTY);
        
        // Remover Trophy Display de encima
        if (world != null && !world.isClient) {
            BlockPos displayPos = pos.up();
            if (world.getBlockState(displayPos).getBlock() == ModBlocks.TROPHY_DISPLAY_BLOCK) {
                world.removeBlock(displayPos, false);
            }
        }
        
        return trophy;
    }

    public void checkMultiblockStructure() {
        if (world == null || world.isClient) return;
        
        boolean wasComplete = isMultiblockComplete;
        int oldTrophyCount = trophyCount;
        
        isMultiblockComplete = validateStructure();
        trophyCount = isMultiblockComplete ? countTrophies() : 0;
        
        if (wasComplete != isMultiblockComplete || oldTrophyCount != trophyCount) {
            markDirty();
            // Solo actualizar estado, sin mensajes automáticos
        }
    }

    private boolean validateStructure() {
        // Verificar Sea Lanterns (capa Y-1) - usar offset en lugar de add
        int[][] seaLanternOffsets = {
            {0, -1, 0},   // Centro
            {1, -1, 0},   // Este
            {-1, -1, 0},  // Oeste
            {0, -1, 1},   // Sur
            {0, -1, -1}   // Norte
        };
        
        for (int[] offset : seaLanternOffsets) {
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != Blocks.SEA_LANTERN) {
                return false;
            }
        }
        
        // Verificar Trophy Pedestals (capa Y-1)
        int[][] pedestalOffsets = {
            {2, -1, 0},   // Este lejano
            {-2, -1, 0},  // Oeste lejano
            {0, -1, 2},   // Sur lejano
            {0, -1, -2},  // Norte lejano
            {1, -1, 1},   // Sureste
            {-1, -1, 1},  // Suroeste
            {1, -1, -1},  // Noreste
            {-1, -1, -1}  // Noroeste
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != ModBlocks.TROPHY_PEDESTAL) {
                return false;
            }
        }
        
        return true;
    }

    private int countTrophies() {
        if (!isMultiblockComplete) return 0;
        
        int count = hasTrophy() ? 1 : 0; // Trofeo del altar
        
        // Contar trofeos en pedestales
        int[][] pedestalOffsets = {
            {2, -1, 0}, {-2, -1, 0}, {0, -1, 2}, {0, -1, -2},
            {1, -1, 1}, {-1, -1, 1}, {1, -1, -1}, {-1, -1, -1}
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity pedestal) {
                if (pedestal.hasTrophy()) {
                    count++;
                }
            }
        }
        
        return count;
    }

    private String getTrophyId(ItemStack trophy) {
        return net.minecraft.registry.Registries.ITEM.getId(trophy.getItem()).getPath();
    }

    public boolean isMultiblockComplete() {
        return isMultiblockComplete;
    }

    public int getTrophyCount() {
        return trophyCount;
    }
    
    public boolean validateStructureManually() {
        if (world == null || world.isClient) return false;
        
        boolean wasComplete = isMultiblockComplete;
        isMultiblockComplete = validateStructure();
        trophyCount = isMultiblockComplete ? countTrophies() : 0;
        
        if (wasComplete != isMultiblockComplete) {
            markDirty();
        }
        
        return isMultiblockComplete;
    }
    
    public String getStructureError() {
        if (world == null || world.isClient) return "Unknown error";
        
        // Verificar Sea Lanterns primero
        int[][] seaLanternOffsets = {
            {0, -1, 0},   // Centro
            {1, -1, 0},   // Este
            {-1, -1, 0},  // Oeste
            {0, -1, 1},   // Sur
            {0, -1, -1}   // Norte
        };
        
        String[] seaLanternNames = {"Center", "East", "West", "South", "North"};
        
        for (int i = 0; i < seaLanternOffsets.length; i++) {
            int[] offset = seaLanternOffsets[i];
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != Blocks.SEA_LANTERN) {
                return "Missing Sea Lantern at " + seaLanternNames[i] + " (" + checkPos.getX() + ", " + checkPos.getY() + ", " + checkPos.getZ() + ")";
            }
        }
        
        // Verificar Trophy Pedestals
        int[][] pedestalOffsets = {
            {2, -1, 0},   // Este lejano
            {-2, -1, 0},  // Oeste lejano
            {0, -1, 2},   // Sur lejano
            {0, -1, -2},  // Norte lejano
            {1, -1, 1},   // Sureste
            {-1, -1, 1},  // Suroeste
            {1, -1, -1},  // Noreste
            {-1, -1, -1}  // Noroeste
        };
        
        String[] pedestalNames = {"Far East", "Far West", "Far South", "Far North", "Southeast", "Southwest", "Northeast", "Northwest"};
        
        for (int i = 0; i < pedestalOffsets.length; i++) {
            int[] offset = pedestalOffsets[i];
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != ModBlocks.TROPHY_PEDESTAL) {
                return "Missing Trophy Pedestal at " + pedestalNames[i] + " (" + checkPos.getX() + ", " + checkPos.getY() + ", " + checkPos.getZ() + ")";
            }
        }
        
        return "Structure should be complete";
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putBoolean("multiblockComplete", isMultiblockComplete);
        nbt.putInt("trophyCount", trophyCount);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        isMultiblockComplete = nbt.getBoolean("multiblockComplete");
        trophyCount = nbt.getInt("trophyCount");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}