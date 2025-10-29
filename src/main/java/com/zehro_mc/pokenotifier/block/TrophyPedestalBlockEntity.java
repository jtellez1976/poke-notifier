/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.block;

import net.minecraft.block.BlockState;
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

public class TrophyPedestalBlockEntity extends BlockEntity {
    public final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);

    public TrophyPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(com.zehro_mc.pokenotifier.block.entity.ModBlockEntities.TROPHY_PEDESTAL, pos, state);
    }

    public ItemStack getTrophy() {
        return inventory.get(0);
    }

    public void setTrophy(ItemStack trophy) {
        ItemStack oldTrophy = inventory.get(0);
        inventory.set(0, trophy);
        markDirty();
        
        // SIEMPRE usar display block para renderizado
        if (world != null && !world.isClient) {
            BlockPos displayPos = pos.up();
            
            // Remover display anterior si existe
            if (world.getBlockState(displayPos).getBlock() instanceof com.zehro_mc.pokenotifier.block.TrophyDisplayBlock) {
                world.removeBlock(displayPos, false);
            }
            
            // Crear display para cualquier item (trophy o pokeball)
            if (!trophy.isEmpty()) {
                world.setBlockState(displayPos, com.zehro_mc.pokenotifier.block.ModBlocks.TROPHY_DISPLAY_BLOCK.getDefaultState());
                if (world.getBlockEntity(displayPos) instanceof com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity displayEntity) {
                    String itemId = net.minecraft.registry.Registries.ITEM.getId(trophy.getItem()).toString();
                    displayEntity.setTrophyData(itemId, "pedestal");
                }
            }
        }
        
        syncToClients();
    }
    
    public boolean isTrophy(ItemStack item) {
        if (item.isEmpty()) return false;
        String itemName = item.getName().getString();
        return itemName.contains("Trophy");
    }
    
    public void forceSync() {
        markDirty();
        syncToClients();
    }
    
    private void syncToClients() {
        if (world != null && !world.isClient) {
            // Sincronización completa cliente-servidor
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, 3);
            
            // Forzar actualización del chunk
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                serverWorld.getChunkManager().markForUpdate(pos);
                
                // Enviar paquete de actualización a todos los jugadores cercanos
                java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                    world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                        new net.minecraft.util.math.Box(pos).expand(32), player -> true);
                
                for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                    player.networkHandler.sendPacket(toUpdatePacket());
                }
            }
        }
    }
    
    private String getPositionName() {
        if (world == null) return "Unknown";
        
        // Find nearby altar within 5 blocks
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos altarPos = pos.add(x, 0, z);
                if (world.getBlockEntity(altarPos) instanceof TrophyAltarBlockEntity altar) {
                    return altar.getPositionName(pos);
                }
            }
        }
        
        return "Unknown";
    }

    public boolean hasTrophy() {
        return !getTrophy().isEmpty();
    }

    public ItemStack removeTrophy() {
        ItemStack trophy = getTrophy();
        setTrophy(ItemStack.EMPTY); // Usar método público para consistencia
        return trophy;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
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