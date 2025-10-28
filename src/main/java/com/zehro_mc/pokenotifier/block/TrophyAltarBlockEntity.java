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
import com.zehro_mc.pokenotifier.block.ModBlocks;
import net.minecraft.world.World;

public class TrophyAltarBlockEntity extends BlockEntity {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private boolean isMultiblockComplete = false;
    private int trophyCount = 0;
    public int previewTimer = 0;
    public boolean showingPreview = false;

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
            // Crear Trophy Display encima solo si es un trofeo
            if (!trophy.isEmpty() && isTrophy(trophy)) {
                BlockPos displayPos = pos.up();
                if (world.getBlockState(displayPos).isAir()) {
                    world.setBlockState(displayPos, ModBlocks.TROPHY_DISPLAY_BLOCK.getDefaultState());
                    if (world.getBlockEntity(displayPos) instanceof com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity displayEntity) {
                        String trophyId = "poke-notifier:" + getTrophyId(trophy);
                        displayEntity.setTrophyData(trophyId, "altar");
                    }
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
        
        // Remover Trophy Display de encima solo si era un trofeo
        if (world != null && !world.isClient && isTrophy(trophy)) {
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
    
    public void showStructurePreview() {
        if (world == null || world.isClient) return;
        
        // Mostrar hologramas de pedestales faltantes
        showPedestalGhosts();
        
        // Mostrar hologramas de redstone faltantes
        showRedstoneGhosts();
    }
    
    private void showPedestalGhosts() {
        int[][] pedestalOffsets = {
            {0, 0, -3},   // Norte
            {3, 0, 0},    // Este
            {0, 0, 3},    // Sur
            {-3, 0, 0},   // Oeste
            {2, 0, -2},   // Noreste
            {2, 0, 2},    // Sureste
            {-2, 0, 2},   // Suroeste
            {-2, 0, -2}   // Noroeste
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != ModBlocks.TROPHY_PEDESTAL) {
                // Crear partículas púrpuras más visibles para pedestal faltante
                spawnPedestalGhostParticles(checkPos);
            }
        }
    }
    
    private void spawnPedestalGhostParticles(BlockPos ghostPos) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Partículas púrpuras para pedestales
        for (int i = 0; i < 45; i++) {
            double x = ghostPos.getX() + Math.random();
            double y = ghostPos.getY() + Math.random();
            double z = ghostPos.getZ() + Math.random();
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.PORTAL, x, y, z, 1, 0, 0.02, 0, 0);
        }
        
        // Partículas END_ROD en el centro para mejor visibilidad
        for (int i = 0; i < 10; i++) {
            double x = ghostPos.getX() + 0.5 + (Math.random() - 0.5) * 0.3;
            double y = ghostPos.getY() + 0.5 + (Math.random() - 0.5) * 0.3;
            double z = ghostPos.getZ() + 0.5 + (Math.random() - 0.5) * 0.3;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, x, y, z, 1, 0, 0.01, 0, 0);
        }
    }
    
    private void showRedstoneGhosts() {
        int[][] redstoneOffsets = {
            {0, -1, 0},   // Debajo del altar
            {0, -1, -3},  // Debajo pedestal Norte
            {3, -1, 0},   // Debajo pedestal Este
            {0, -1, 3},   // Debajo pedestal Sur
            {-3, -1, 0},  // Debajo pedestal Oeste
            {2, -1, -2},  // Debajo pedestal Noreste
            {2, -1, 2},   // Debajo pedestal Sureste
            {-2, -1, 2},  // Debajo pedestal Suroeste
            {-2, -1, -2}  // Debajo pedestal Noroeste
        };
        
        for (int[] offset : redstoneOffsets) {
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != net.minecraft.block.Blocks.REDSTONE_BLOCK) {
                // Crear partículas rojas más visibles para redstone faltante
                spawnRedstoneGhostParticles(checkPos);
            }
        }
    }
    
    private void spawnRedstoneGhostParticles(BlockPos ghostPos) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        net.minecraft.particle.DustParticleEffect redDust = new net.minecraft.particle.DustParticleEffect(net.minecraft.util.math.Vec3d.unpackRgb(0xFF0000).toVector3f(), 1.0f);
        
        // Partículas rojas más intensas para redstone
        for (int i = 0; i < 40; i++) {
            double x = ghostPos.getX() + Math.random();
            double y = ghostPos.getY() + Math.random();
            double z = ghostPos.getZ() + Math.random();
            
            serverWorld.spawnParticles(redDust, x, y, z, 1, 0, 0, 0, 0);
        }
        
        // Partículas centrales
        for (int i = 0; i < 8; i++) {
            serverWorld.spawnParticles(redDust, 
                ghostPos.getX() + 0.5, 
                ghostPos.getY() + 0.5, 
                ghostPos.getZ() + 0.5, 
                1, 0, 0, 0, 0);
        }
    }
    
    private void spawnGhostParticles(BlockPos ghostPos, net.minecraft.particle.ParticleEffect particle, double r, double g, double b) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Crear un cubo de partículas más denso y visible
        for (int i = 0; i < 50; i++) {
            double x = ghostPos.getX() + 0.1 + Math.random() * 0.8;
            double y = ghostPos.getY() + 0.1 + Math.random() * 0.8;
            double z = ghostPos.getZ() + 0.1 + Math.random() * 0.8;
            
            // Crear bordes más definidos del cubo fantasma
            boolean isEdge = false;
            
            // Bordes en X
            if (x < ghostPos.getX() + 0.2 || x > ghostPos.getX() + 0.8) isEdge = true;
            // Bordes en Z
            if (z < ghostPos.getZ() + 0.2 || z > ghostPos.getZ() + 0.8) isEdge = true;
            // Bordes en Y
            if (y < ghostPos.getY() + 0.2 || y > ghostPos.getY() + 0.8) isEdge = true;
            
            if (isEdge || Math.random() < 0.1) {
                serverWorld.spawnParticles(particle, x, y, z, 1, 0, 0.01, 0, 0);
            }
        }
        
        // Partículas centrales más brillantes
        for (int i = 0; i < 5; i++) {
            double x = ghostPos.getX() + 0.5 + (Math.random() - 0.5) * 0.2;
            double y = ghostPos.getY() + 0.5 + (Math.random() - 0.5) * 0.2;
            double z = ghostPos.getZ() + 0.5 + (Math.random() - 0.5) * 0.2;
            
            serverWorld.spawnParticles(particle, x, y, z, 1, 0, 0.02, 0, 0);
        }
    }

    private boolean validateStructure() {
        // Verificar Trophy Pedestals (capa Y+0) - estructura 7x7
        int[][] pedestalOffsets = {
            {0, 0, -3},   // Norte
            {3, 0, 0},    // Este
            {0, 0, 3},    // Sur
            {-3, 0, 0},   // Oeste
            {2, 0, -2},   // Noreste
            {2, 0, 2},    // Sureste
            {-2, 0, 2},   // Suroeste
            {-2, 0, -2}   // Noroeste
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != ModBlocks.TROPHY_PEDESTAL) {
                return false;
            }
        }
        
        // Verificar Redstone Blocks (capa Y-1) - debajo de altar + pedestales
        int[][] redstoneOffsets = {
            {0, -1, 0},   // Debajo del altar
            {0, -1, -3},  // Debajo pedestal Norte
            {3, -1, 0},   // Debajo pedestal Este
            {0, -1, 3},   // Debajo pedestal Sur
            {-3, -1, 0},  // Debajo pedestal Oeste
            {2, -1, -2},  // Debajo pedestal Noreste
            {2, -1, 2},   // Debajo pedestal Sureste
            {-2, -1, 2},  // Debajo pedestal Suroeste
            {-2, -1, -2}  // Debajo pedestal Noroeste
        };
        
        for (int[] offset : redstoneOffsets) {
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != Blocks.REDSTONE_BLOCK) {
                return false;
            }
        }
        
        return true;
    }

    private int countTrophies() {
        if (!isMultiblockComplete) return 0;
        return getUniquePokeballs().size();
    }
    
    public java.util.Set<String> getUniquePokeballs() {
        java.util.Set<String> pokeballs = new java.util.HashSet<>();
        
        // Agregar pokeball del altar si existe
        if (hasTrophy()) {
            String pokeball = getPokeball(getTrophy());
            if (pokeball != null) {
                pokeballs.add(pokeball);
            }
        }
        
        // Agregar pokeballs de pedestales (nueva estructura 7x7)
        int[][] pedestalOffsets = {
            {0, 0, -3},   // Norte
            {3, 0, 0},    // Este
            {0, 0, 3},    // Sur
            {-3, 0, 0},   // Oeste
            {2, 0, -2},   // Noreste
            {2, 0, 2},    // Sureste
            {-2, 0, 2},   // Suroeste
            {-2, 0, -2}   // Noroeste
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity pedestal) {
                if (pedestal.hasTrophy()) {
                    String pokeball = getPokeball(pedestal.getTrophy());
                    if (pokeball != null) {
                        pokeballs.add(pokeball);
                    }
                }
            }
        }
        
        return pokeballs;
    }
    
    private String getPokeball(ItemStack item) {
        if (item.isEmpty()) return null;
        
        String itemId = net.minecraft.registry.Registries.ITEM.getId(item.getItem()).toString();
        
        // Mapear IDs de Pokéballs de Cobblemon
        return switch (itemId) {
            case "cobblemon:poke_ball" -> "poke_ball";
            case "cobblemon:great_ball" -> "great_ball";
            case "cobblemon:ultra_ball" -> "ultra_ball";
            case "cobblemon:master_ball" -> "master_ball";
            case "cobblemon:timer_ball" -> "timer_ball";
            case "cobblemon:dusk_ball" -> "dusk_ball";
            case "cobblemon:quick_ball" -> "quick_ball";
            case "cobblemon:repeat_ball" -> "repeat_ball";
            case "cobblemon:luxury_ball" -> "luxury_ball";
            default -> null;
        };
    }

    private String getTrophyId(ItemStack trophy) {
        return net.minecraft.registry.Registries.ITEM.getId(trophy.getItem()).getPath();
    }
    
    private boolean isTrophy(ItemStack item) {
        if (item.isEmpty()) return false;
        
        String itemName = item.getName().getString();
        return itemName.contains("Trophy");
    }
    
    private boolean isLegendaryOrMythical(String pokemonName) {
        String[] legendaries = {
            "mewtwo", "mew", "lugia", "ho-oh", "celebi", "kyogre", "groudon", "rayquaza", "jirachi", "deoxys",
            "dialga", "palkia", "giratina", "phione", "manaphy", "darkrai", "shaymin", "arceus",
            "victini", "reshiram", "zekrom", "kyurem", "keldeo", "meloetta", "genesect",
            "xerneas", "yveltal", "zygarde", "diancie", "hoopa", "volcanion",
            "cosmog", "cosmoem", "solgaleo", "lunala", "necrozma", "magearna", "marshadow", "zeraora",
            "zacian", "zamazenta", "eternatus", "kubfu", "urshifu", "regieleki", "regidrago", "glastrier", "spectrier", "calyrex"
        };
        
        String lowerName = pokemonName.toLowerCase();
        for (String legendary : legendaries) {
            if (lowerName.contains(legendary)) {
                return true;
            }
        }
        return false;
    }
    
    private void sendGlobalSummonMessage(String playerName, String pokemonName) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.MinecraftServer server = world.getServer();
        if (server != null) {
            net.minecraft.text.Text message = net.minecraft.text.Text.literal("🎆 ").formatted(net.minecraft.util.Formatting.GOLD)
                .append(net.minecraft.text.Text.literal(playerName).formatted(net.minecraft.util.Formatting.YELLOW))
                .append(net.minecraft.text.Text.literal(" has summoned the legendary ").formatted(net.minecraft.util.Formatting.GOLD))
                .append(net.minecraft.text.Text.literal(pokemonName.toUpperCase()).formatted(net.minecraft.util.Formatting.LIGHT_PURPLE))
                .append(net.minecraft.text.Text.literal("! 🎆").formatted(net.minecraft.util.Formatting.GOLD));
            
            server.getPlayerManager().broadcast(message, false);
        }
    }
    
    private void spawnCompletionEffects() {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Explosión masiva de partículas desde el altar
        for (int i = 0; i < 120; i++) {
            double x = pos.getX() + 0.5 + (Math.random() - 0.5) * 4;
            double y = pos.getY() + 1 + Math.random() * 3;
            double z = pos.getZ() + 0.5 + (Math.random() - 0.5) * 4;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANT, 
                x, y, z, 1, 0, 0.15, 0, 0.08);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, 
                x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }
        
        // Ondas de partículas en círculos concéntricos
        for (int ring = 1; ring <= 3; ring++) {
            for (int i = 0; i < 60; i++) {
                double angle = (Math.PI * 2 * i) / 60;
                double radius = ring * 1.2;
                double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
                
                serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, 
                    x, pos.getY() + 1.5, z, 1, 0, 0.1, 0, 0.02);
                serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.WITCH, 
                    x, pos.getY() + 1.2, z, 1, 0, 0.05, 0, 0.01);
            }
        }
        
        // Sonido de completación
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_BEACON_ACTIVATE, 
            net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 
            net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.8f);
    }
    
    private void spawnReadyEffects() {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Partículas flotantes suaves alrededor del altar
        for (int i = 0; i < 10; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 1.0 + Math.random() * 0.5;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.WITCH, 
                x, pos.getY() + 1.2, z, 1, 0, 0.02, 0, 0.01);
        }
        
        // Partículas mágicas en el centro
        serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, 
            pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 
            1, 0, 0.1, 0, 0.02);
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
        
        // Verificar Trophy Pedestals primero (capa Y+0)
        int[][] pedestalOffsets = {
            {0, 0, -3},   // Norte
            {3, 0, 0},    // Este
            {0, 0, 3},    // Sur
            {-3, 0, 0},   // Oeste
            {2, 0, -2},   // Noreste
            {2, 0, 2},    // Sureste
            {-2, 0, 2},   // Suroeste
            {-2, 0, -2}   // Noroeste
        };
        
        String[] pedestalNames = {"North", "East", "South", "West", "Northeast", "Southeast", "Southwest", "Northwest"};
        
        for (int i = 0; i < pedestalOffsets.length; i++) {
            int[] offset = pedestalOffsets[i];
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != ModBlocks.TROPHY_PEDESTAL) {
                return "Missing Trophy Pedestal at " + pedestalNames[i] + " (" + checkPos.getX() + ", " + checkPos.getY() + ", " + checkPos.getZ() + ")";
            }
        }
        
        // Verificar Redstone Blocks (capa Y-1)
        int[][] redstoneOffsets = {
            {0, -1, 0},   // Debajo del altar
            {0, -1, -3},  // Debajo pedestal Norte
            {3, -1, 0},   // Debajo pedestal Este
            {0, -1, 3},   // Debajo pedestal Sur
            {-3, -1, 0},  // Debajo pedestal Oeste
            {2, -1, -2},  // Debajo pedestal Noreste
            {2, -1, 2},   // Debajo pedestal Sureste
            {-2, -1, 2},  // Debajo pedestal Suroeste
            {-2, -1, -2}  // Debajo pedestal Noroeste
        };
        
        String[] redstoneNames = {"Altar Base", "North Base", "East Base", "South Base", "West Base", "Northeast Base", "Southeast Base", "Southwest Base", "Northwest Base"};
        
        for (int i = 0; i < redstoneOffsets.length; i++) {
            int[] offset = redstoneOffsets[i];
            BlockPos checkPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockState(checkPos).getBlock() != Blocks.REDSTONE_BLOCK) {
                return "Missing Redstone Block at " + redstoneNames[i] + " (" + checkPos.getX() + ", " + checkPos.getY() + ", " + checkPos.getZ() + ")";
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
        nbt.putInt("previewTimer", previewTimer);
        nbt.putBoolean("showingPreview", showingPreview);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        isMultiblockComplete = nbt.getBoolean("multiblockComplete");
        trophyCount = nbt.getInt("trophyCount");
        previewTimer = nbt.getInt("previewTimer");
        showingPreview = nbt.getBoolean("showingPreview");
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
    
    public static void tick(World world, BlockPos pos, BlockState state, TrophyAltarBlockEntity blockEntity) {
        if (world.isClient) return;
        
        blockEntity.previewTimer++;
        
        // Verificar estructura cada segundo
        if (blockEntity.previewTimer % 20 == 0) {
            boolean wasComplete = blockEntity.isMultiblockComplete;
            String[] oldPattern = blockEntity.getPokeballPattern();
            int oldPokeballCount = 0;
            for (String p : oldPattern) {
                if (p != null && !p.equals("empty")) oldPokeballCount++;
            }
            
            blockEntity.checkMultiblockStructure();
            
            // Si la estructura se completó, detener hologramas
            if (!wasComplete && blockEntity.isMultiblockComplete) {
                blockEntity.showingPreview = false;
                // Notificar a jugadores cercanos
                java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                    world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                        new net.minecraft.util.math.Box(pos).expand(10), player -> true);
                
                for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                    player.sendMessage(net.minecraft.text.Text.literal("✓ Multiblock structure complete! Ghosts disabled.").formatted(net.minecraft.util.Formatting.GREEN), false);
                }
            }
            
            // Verificar si se completaron las 8 pokéballs
            if (blockEntity.isMultiblockComplete) {
                String[] newPattern = blockEntity.getPokeballPattern();
                int newPokeballCount = 0;
                for (String p : newPattern) {
                    if (p != null && !p.equals("empty")) newPokeballCount++;
                }
                
                // Si se acaban de completar las 8 pokéballs
                if (oldPokeballCount < 8 && newPokeballCount == 8) {
                    blockEntity.spawnCompletionEffects();
                    
                    // Notificar a jugadores cercanos
                    java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                        world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                            new net.minecraft.util.math.Box(pos).expand(15), player -> true);
                    
                    for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                        player.sendMessage(net.minecraft.text.Text.literal("✨ The ritual is ready! Right-click the altar to summon! ✨").formatted(net.minecraft.util.Formatting.GOLD), false);
                    }
                }
            }
        }
        
        // Efectos continuos cuando está listo para invocar
        if (blockEntity.isMultiblockComplete && blockEntity.isValidPattern(blockEntity.getPokeballPattern())) {
            if (blockEntity.previewTimer % 40 == 0) { // Cada 2 segundos
                blockEntity.spawnReadyEffects();
            }
        }
        
        // Si el multibloque no está completo, mostrar preview cada 5 segundos
        if (!blockEntity.isMultiblockComplete && blockEntity.previewTimer % 100 == 0) { // 100 ticks = 5 segundos
            blockEntity.showStructurePreview();
            blockEntity.showingPreview = true;
        }
    }
    
    public void attemptPokemonSummon() {
        if (world == null || world.isClient) return;
        
        // Obtener patrón de pokeballs en los 8 pedestales
        String[] pokeballPattern = getPokeballPattern();
        
        // Verificar si el patrón es válido (8 pokeballs)
        if (!isValidPattern(pokeballPattern)) {
            return;
        }
        
        // Buscar qué Pokémon corresponde a este patrón
        String pokemon = getPokemonFromPattern(pokeballPattern);
        
        if (pokemon != null) {
            // Consumir las pokeballs ANTES de invocar
            consumePokeballs();
            
            // Invocar el Pokémon (incluye efectos pre-invocación)
            summonPokemon(pokemon);
            
            // Efectos visuales finales de invocación
            spawnSummonEffects();
            
            // Quemar los recursos - convertir redstone en campfires
            burnRedstoneBlocks();
        }
    }
    

    
    public String[] getPokeballPattern() {
        String[] pattern = new String[8];
        
        // Posiciones de los 8 pedestales en orden
        int[][] pedestalOffsets = {
            {0, 0, -3},   // 0: Norte
            {3, 0, 0},    // 1: Este
            {0, 0, 3},    // 2: Sur
            {-3, 0, 0},   // 3: Oeste
            {2, 0, -2},   // 4: Noreste
            {2, 0, 2},    // 5: Sureste
            {-2, 0, 2},   // 6: Suroeste
            {-2, 0, -2}   // 7: Noroeste
        };
        
        for (int i = 0; i < 8; i++) {
            int[] offset = pedestalOffsets[i];
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            
            if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity pedestal) {
                if (pedestal.hasTrophy()) {
                    pattern[i] = getPokeball(pedestal.getTrophy());
                } else {
                    pattern[i] = "empty";
                }
            } else {
                pattern[i] = "empty";
            }
        }
        
        return pattern;
    }
    
    public boolean isValidPattern(String[] pattern) {
        // Verificar que hay exactamente 8 pokeballs (no empty)
        int count = 0;
        for (String pokeball : pattern) {
            if (pokeball != null && !pokeball.equals("empty")) {
                count++;
            }
        }
        return count == 8;
    }
    
    private String getPokemonFromPattern(String[] pattern) {
        // Convertir patrón a string para comparación
        String patternString = String.join(",", pattern);
        
        // Ejemplos de combinaciones (expandir según necesidad)
        return switch (patternString) {
            case "poke_ball,poke_ball,poke_ball,poke_ball,poke_ball,poke_ball,poke_ball,poke_ball" -> "pikachu";
            case "great_ball,great_ball,great_ball,great_ball,great_ball,great_ball,great_ball,great_ball" -> "charizard";
            case "ultra_ball,ultra_ball,ultra_ball,ultra_ball,ultra_ball,ultra_ball,ultra_ball,ultra_ball" -> "blastoise";
            case "master_ball,master_ball,master_ball,master_ball,master_ball,master_ball,master_ball,master_ball" -> "mewtwo";
            case "poke_ball,great_ball,poke_ball,great_ball,poke_ball,great_ball,poke_ball,great_ball" -> "eevee";
            case "timer_ball,dusk_ball,quick_ball,repeat_ball,luxury_ball,timer_ball,dusk_ball,quick_ball" -> "lucario";
            // Más combinaciones...
            default -> null;
        };
    }
    
    private void summonPokemon(String pokemonName) {
        if (world == null || world.isClient) return;
        
        try {
            // Efectos pre-invocación
            spawnPreSummonEffects();
            
            // Usar Cobblemon API para crear el Pokémon
            com.cobblemon.mod.common.api.pokemon.PokemonProperties props = 
                com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse(pokemonName);
            
            com.cobblemon.mod.common.entity.pokemon.PokemonEntity pokemonEntity = 
                props.createEntity((net.minecraft.server.world.ServerWorld) world);
            
            // Posicionar en el suelo cerca del altar
            pokemonEntity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
            
            world.spawnEntity(pokemonEntity);
            
            // Verificar si es legendario/mítico para mensaje global
            boolean isLegendaryOrMythical = isLegendaryOrMythical(pokemonName);
            
            // Notificar a jugadores cercanos
            java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                    new net.minecraft.util.math.Box(pos).expand(50), player -> true);
            
            net.minecraft.server.network.ServerPlayerEntity summoner = null;
            for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                if (summoner == null) summoner = player; // Primer jugador encontrado como invocador
                
                if (isLegendaryOrMythical) {
                    player.sendMessage(net.minecraft.text.Text.literal("🎆 LEGENDARY SUMMONING! 🎆").formatted(net.minecraft.util.Formatting.GOLD), false);
                }
                player.sendMessage(net.minecraft.text.Text.literal("✨ " + pokemonName.toUpperCase() + " has been summoned! ✨").formatted(net.minecraft.util.Formatting.GOLD), false);
            }
            
            // Mensaje global para legendarios/míticos
            if (isLegendaryOrMythical && summoner != null) {
                sendGlobalSummonMessage(summoner.getName().getString(), pokemonName);
            }
            
        } catch (Exception e) {
            // Error en la invocación
            java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                    new net.minecraft.util.math.Box(pos).expand(10), player -> true);
            
            for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                player.sendMessage(net.minecraft.text.Text.literal("⚠ Summoning failed: Unknown Pokémon pattern").formatted(net.minecraft.util.Formatting.RED), false);
            }
        }
    }
    
    private void consumePokeballs() {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Consumir todas las pokeballs de los pedestales
        int[][] pedestalOffsets = {
            {0, 0, -3}, {3, 0, 0}, {0, 0, 3}, {-3, 0, 0},
            {2, 0, -2}, {2, 0, 2}, {-2, 0, 2}, {-2, 0, -2}
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity pedestal) {
                if (pedestal.hasTrophy()) {
                    // Limpiar el inventario directamente
                    pedestal.inventory.set(0, ItemStack.EMPTY);
                    pedestal.markDirty();
                    
                    // Romper y recolocar el bloque para forzar actualización del render
                    BlockState pedestalState = world.getBlockState(pedestalPos);
                    world.removeBlock(pedestalPos, false);
                    world.setBlockState(pedestalPos, pedestalState, 3);
                    
                    // Recrear el BlockEntity vacío
                    if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity newPedestal) {
                        newPedestal.setTrophy(ItemStack.EMPTY);
                        newPedestal.markDirty();
                    }
                }
            }
        }
    }
    
    private void spawnPreSummonEffects() {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Rayos cósmicos cayendo del cielo
        for (int i = 0; i < 8; i++) {
            double x = pos.getX() + 0.5 + (Math.random() - 0.5) * 3;
            double z = pos.getZ() + 0.5 + (Math.random() - 0.5) * 3;
            
            // Crear rayo de partículas desde arriba
            for (int y = 25; y >= 0; y--) {
                serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, 
                    x, pos.getY() + y, z, 1, 0, 0, 0, 0);
                if (y % 3 == 0) {
                    serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, 
                        x, pos.getY() + y, z, 1, 0.1, 0, 0.1, 0.01);
                }
            }
        }
        
        // Espiral mágica masiva alrededor del altar
        for (int i = 0; i < 100; i++) {
            double angle = (Math.PI * 2 * i) / 100;
            double radius = 2.5 + Math.sin(angle * 3) * 0.5;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = pos.getY() + 1 + Math.sin(angle * 5) * 0.3;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANT, 
                x, y, z, 1, 0, 0.1, 0, 0.02);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.DRAGON_BREATH, 
                x, y, z, 1, 0, 0.05, 0, 0.01);
        }
        
        // Sonidos dramáticos
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.ITEM_TOTEM_USE, 
            net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.5f);
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
            net.minecraft.sound.SoundCategory.BLOCKS, 0.5f, 1.5f);
    }
    
    private void spawnSummonEffects() {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // GRAN FINALE - Explosión masiva de partículas doradas
        for (int i = 0; i < 200; i++) {
            double x = pos.getX() + 0.5 + (Math.random() - 0.5) * 6;
            double y = pos.getY() + 1 + Math.random() * 4;
            double z = pos.getZ() + 0.5 + (Math.random() - 0.5) * 6;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING, 
                x, y, z, 1, 0, 0.3, 0, 0.15);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, 
                x, y, z, 1, 0.1, 0.1, 0.1, 0.08);
        }
        
        // Fuegos artificiales espectaculares
        for (int i = 0; i < 50; i++) {
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.FIREWORK, 
                pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5, 
                1, (Math.random() - 0.5) * 2, Math.random() * 2, (Math.random() - 0.5) * 2, 0.2);
        }
        
        // Espiral gigante de partículas mágicas
        for (int i = 0; i < 150; i++) {
            double angle = (Math.PI * 2 * i) / 150;
            double radius = 4.0 + Math.sin(angle * 4) * 1.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = pos.getY() + 1 + Math.sin(angle * 8) * 1.5;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.DRAGON_BREATH, 
                x, y, z, 1, 0, 0.2, 0, 0.05);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, 
                x, y, z, 1, 0, 0.1, 0, 0.03);
        }
        
        // Columna de luz hacia el cielo
        for (int y = 0; y < 30; y++) {
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, 
                pos.getX() + 0.5, pos.getY() + 1 + y, pos.getZ() + 0.5, 
                3, 0.2, 0, 0.2, 0.02);
        }
        
        // Sonido final épico
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 
            net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.8f);
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 
            net.minecraft.sound.SoundCategory.BLOCKS, 0.8f, 1.2f);
    }
    
    private void burnRedstoneBlocks() {
        if (world == null || world.isClient) return;
        
        // Posiciones de todos los bloques de redstone
        int[][] redstoneOffsets = {
            {0, -1, 0},   // Debajo del altar
            {0, -1, -3},  // Debajo pedestal Norte
            {3, -1, 0},   // Debajo pedestal Este
            {0, -1, 3},   // Debajo pedestal Sur
            {-3, -1, 0},  // Debajo pedestal Oeste
            {2, -1, -2},  // Debajo pedestal Noreste
            {2, -1, 2},   // Debajo pedestal Sureste
            {-2, -1, 2},  // Debajo pedestal Suroeste
            {-2, -1, -2}  // Debajo pedestal Noroeste
        };
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        for (int[] offset : redstoneOffsets) {
            BlockPos redstonePos = pos.add(offset[0], offset[1], offset[2]);
            
            // Verificar que sea un bloque de redstone
            if (world.getBlockState(redstonePos).getBlock() == net.minecraft.block.Blocks.REDSTONE_BLOCK) {
                // Efectos de quemado antes de reemplazar
                spawnBurnEffects(serverWorld, redstonePos);
                
                // Reemplazar con campfire encendido
                world.setBlockState(redstonePos, net.minecraft.block.Blocks.CAMPFIRE.getDefaultState()
                    .with(net.minecraft.block.CampfireBlock.LIT, true), 3);
            }
        }
        
        // Sonido de quemado
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.ITEM_FIRECHARGE_USE, 
            net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.8f);
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_FIRE_AMBIENT, 
            net.minecraft.sound.SoundCategory.BLOCKS, 0.8f, 1.0f);
    }
    
    private void spawnBurnEffects(net.minecraft.server.world.ServerWorld serverWorld, BlockPos burnPos) {
        // Partículas de fuego y humo
        for (int i = 0; i < 20; i++) {
            double x = burnPos.getX() + Math.random();
            double y = burnPos.getY() + Math.random();
            double z = burnPos.getZ() + Math.random();
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.FLAME, 
                x, y, z, 1, 0, 0.1, 0, 0.02);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.SMOKE, 
                x, y + 0.5, z, 1, 0, 0.1, 0, 0.05);
        }
        
        // Partículas de lava para efecto más dramático
        for (int i = 0; i < 10; i++) {
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.LAVA, 
                burnPos.getX() + 0.5, burnPos.getY() + 1, burnPos.getZ() + 0.5, 
                1, 0.2, 0, 0.2, 0.1);
        }
    }
}