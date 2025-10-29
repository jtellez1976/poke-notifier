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
import java.io.InputStream;
import java.io.InputStreamReader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.*;
import com.zehro_mc.pokenotifier.PokeNotifier;

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
        
        // Mapear TODAS las Pokéballs de Cobblemon
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
            case "cobblemon:net_ball" -> "net_ball";
            case "cobblemon:nest_ball" -> "nest_ball";
            case "cobblemon:dive_ball" -> "dive_ball";
            case "cobblemon:heal_ball" -> "heal_ball";
            case "cobblemon:premier_ball" -> "premier_ball";
            case "cobblemon:safari_ball" -> "safari_ball";
            case "cobblemon:sport_ball" -> "sport_ball";
            case "cobblemon:park_ball" -> "park_ball";
            case "cobblemon:cherish_ball" -> "cherish_ball";
            case "cobblemon:gs_ball" -> "gs_ball";
            case "cobblemon:beast_ball" -> "beast_ball";
            case "cobblemon:dream_ball" -> "dream_ball";
            case "cobblemon:moon_ball" -> "moon_ball";
            case "cobblemon:love_ball" -> "love_ball";
            case "cobblemon:friend_ball" -> "friend_ball";
            case "cobblemon:lure_ball" -> "lure_ball";
            case "cobblemon:heavy_ball" -> "heavy_ball";
            case "cobblemon:level_ball" -> "level_ball";
            case "cobblemon:fast_ball" -> "fast_ball";
            case "cobblemon:ancient_poke_ball" -> "ancient_poke_ball";
            case "cobblemon:ancient_great_ball" -> "ancient_great_ball";
            case "cobblemon:ancient_ultra_ball" -> "ancient_ultra_ball";
            case "cobblemon:ancient_heavy_ball" -> "ancient_heavy_ball";
            case "cobblemon:feather_ball" -> "feather_ball";
            case "cobblemon:wing_ball" -> "wing_ball";
            case "cobblemon:jet_ball" -> "jet_ball";
            case "cobblemon:leaden_ball" -> "leaden_ball";
            case "cobblemon:gigaton_ball" -> "gigaton_ball";
            case "cobblemon:origin_ball" -> "origin_ball";
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
                .append(net.minecraft.text.Text.literal(" has summoned the legendary ").formatted(net.minecraft.util.Formatting.WHITE))
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
        nbt.putBoolean("isSummoning", isSummoning);
        nbt.putInt("summoningTimer", summoningTimer);
        nbt.putBoolean("isAnimatingPokeballs", isAnimatingPokeballs);
        nbt.putInt("animationPhase", animationPhase);
        if (pendingPokemon != null) {
            nbt.putString("pendingPokemon", pendingPokemon);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
        isMultiblockComplete = nbt.getBoolean("multiblockComplete");
        trophyCount = nbt.getInt("trophyCount");
        previewTimer = nbt.getInt("previewTimer");
        showingPreview = nbt.getBoolean("showingPreview");
        isSummoning = nbt.getBoolean("isSummoning");
        summoningTimer = nbt.getInt("summoningTimer");
        isAnimatingPokeballs = nbt.getBoolean("isAnimatingPokeballs");
        animationPhase = nbt.getInt("animationPhase");
        if (nbt.contains("pendingPokemon")) {
            pendingPokemon = nbt.getString("pendingPokemon");
        }
        
        // Limpiar lista de pokeballs flotantes al cargar (no se pueden serializar entidades)
        if (isAnimatingPokeballs) {
            floatingPokeballs.clear();
        }
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
        
        // Manejar secuencia de invocación
        if (blockEntity.isSummoning) {
            blockEntity.summoningTimer++;
            
            // FASE 0: Animación de pokeballs (0-60 ticks)
            if (blockEntity.summoningTimer <= 60) {
                blockEntity.updatePokeballAnimation();
            }
            
            // FASE 1: Efectos pre-invocación después de animación (60 ticks)
            if (blockEntity.summoningTimer == 60) {
                blockEntity.spawnPreSummonEffects();
            }
            
            // FASE 2: Dragon Death Flash después de 100 ticks (5 segundos)
            if (blockEntity.summoningTimer == 100) {
                blockEntity.spawnDragonDeathFlash();
                blockEntity.consumePokeballs();
                blockEntity.burnRedstoneBlocks();
            }
            
            // FASE 3: Invocar Pokémon después de 120 ticks (6 segundos total)
            if (blockEntity.summoningTimer >= 120) {
                if (blockEntity.pendingPokemon != null) {
                    blockEntity.summonPokemon(blockEntity.pendingPokemon);
                }
                
                // Resetear estado
                blockEntity.isSummoning = false;
                blockEntity.pendingPokemon = null;
                blockEntity.summoningTimer = 0;
                blockEntity.cleanupPokeballAnimation();
                blockEntity.markDirty();
            }
        }
        
        // Verificar estructura cada segundo
        if (blockEntity.previewTimer % 20 == 0) {
            boolean wasComplete = blockEntity.isMultiblockComplete;
            String[] oldPattern = blockEntity.getPokeballPattern();
            boolean wasValidPattern = blockEntity.isValidPattern(oldPattern);
            
            blockEntity.checkMultiblockStructure();
            
            // Si la estructura se completó, detener hologramas
            if (!wasComplete && blockEntity.isMultiblockComplete) {
                blockEntity.showingPreview = false;
                // Notificar a jugadores cercanos
                java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                    world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                        new net.minecraft.util.math.Box(pos).expand(10), player -> true);
                
                for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                    player.sendMessage(net.minecraft.text.Text.literal("✓ ").formatted(net.minecraft.util.Formatting.GREEN)
                        .append(net.minecraft.text.Text.literal("Multiblock structure complete! ").formatted(net.minecraft.util.Formatting.WHITE))
                        .append(net.minecraft.text.Text.literal("Ghosts disabled.").formatted(net.minecraft.util.Formatting.GRAY)), false);
                    
                    // Entregar libro guía si no lo tiene
                    blockEntity.giveGuideBookIfNeeded(player);
                }
            }
            
            // Verificar si hay un patrón válido
            if (blockEntity.isMultiblockComplete) {
                String[] newPattern = blockEntity.getPokeballPattern();
                boolean isValidPattern = blockEntity.isValidPattern(newPattern);
                
                // Si se completó un patrón válido
                if (!wasValidPattern && isValidPattern) {
                    blockEntity.spawnCompletionEffects();
                    
                    // Notificar a jugadores cercanos
                    java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                        world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                            new net.minecraft.util.math.Box(pos).expand(15), player -> true);
                    
                    for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                        player.sendMessage(net.minecraft.text.Text.literal("✨ ").formatted(net.minecraft.util.Formatting.GOLD)
                            .append(net.minecraft.text.Text.literal("The ritual is ready! ").formatted(net.minecraft.util.Formatting.GOLD))
                            .append(net.minecraft.text.Text.literal("Right-click the altar to summon! ").formatted(net.minecraft.util.Formatting.WHITE))
                            .append(net.minecraft.text.Text.literal("✨").formatted(net.minecraft.util.Formatting.GOLD)), false);
                    }
                }
            }
        }
        
        // Efectos continuos SOLO cuando hay un patrón válido
        if (blockEntity.isMultiblockComplete && !blockEntity.isSummoning) {
            String[] pattern = blockEntity.getPokeballPattern();
            if (blockEntity.isValidPattern(pattern)) {
                if (blockEntity.previewTimer % 40 == 0) { // Cada 2 segundos
                    blockEntity.spawnReadyEffects();
                }
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
        
        if (!isValidPattern(pokeballPattern)) {
            Map<String, String> currentPattern = getCurrentPositionalPattern();
            int pokeballCount = currentPattern.size();
            
            java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                    new net.minecraft.util.math.Box(pos).expand(10), player -> true);
            
            for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                if (pokeballCount == 0) {
                    player.sendMessage(net.minecraft.text.Text.literal("⚠ ").formatted(net.minecraft.util.Formatting.RED)
                        .append(net.minecraft.text.Text.literal("No pokeballs found in pedestals!").formatted(net.minecraft.util.Formatting.GOLD)), false);
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("⚠ ").formatted(net.minecraft.util.Formatting.RED)
                        .append(net.minecraft.text.Text.literal("Invalid pokeball pattern! Found ").formatted(net.minecraft.util.Formatting.GOLD))
                        .append(net.minecraft.text.Text.literal(String.valueOf(pokeballCount)).formatted(net.minecraft.util.Formatting.WHITE))
                        .append(net.minecraft.text.Text.literal(" pokeballs but no matching Pokemon.").formatted(net.minecraft.util.Formatting.GOLD)), false);
                }
            }
            return;
        }
        
        // Buscar qué Pokémon corresponde a este patrón
        String pokemon = getPokemonFromPattern(pokeballPattern);
        
        if (pokemon != null) {
            // Notificar inicio del ritual
            java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                    new net.minecraft.util.math.Box(pos).expand(15), player -> true);
            
            for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                player.sendMessage(net.minecraft.text.Text.literal("✨ ").formatted(net.minecraft.util.Formatting.GOLD)
                    .append(net.minecraft.text.Text.literal("Summoning ritual ready! Activating...").formatted(net.minecraft.util.Formatting.GREEN)), false);
            }
            
            // Iniciar secuencia de invocación con efectos dramáticos
            startSummoningSequence(pokemon);
        } else {
            // Patrón no reconocido
            java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                    new net.minecraft.util.math.Box(pos).expand(10), player -> true);
            
            for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                player.sendMessage(net.minecraft.text.Text.literal("⚠ ").formatted(net.minecraft.util.Formatting.RED)
                    .append(net.minecraft.text.Text.literal("Unknown pokeball pattern! No Pokemon matches this combination.").formatted(net.minecraft.util.Formatting.GOLD)), false);
            }
        }
    }
    

    
    public String[] getPokeballPattern() {
        String[] pattern = new String[8];
        String[] positions = {"North", "East", "South", "West", "Northeast", "Southeast", "Southwest", "Northwest"};
        
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
                    String pokeball = getPokeball(pedestal.getTrophy());
                    pattern[i] = pokeball != null ? pokeball : "empty";
                } else {
                    pattern[i] = "empty";
                }
            } else {
                pattern[i] = "empty";
            }
        }
        
        return pattern;
    }
    
    public String getPositionName(BlockPos pedestalPos) {
        int relX = pedestalPos.getX() - pos.getX();
        int relZ = pedestalPos.getZ() - pos.getZ();
        
        if (relX == 0 && relZ == -3) return "North";
        if (relX == 3 && relZ == 0) return "East";
        if (relX == 0 && relZ == 3) return "South";
        if (relX == -3 && relZ == 0) return "West";
        if (relX == 2 && relZ == -2) return "Northeast";
        if (relX == 2 && relZ == 2) return "Southeast";
        if (relX == -2 && relZ == 2) return "Southwest";
        if (relX == -2 && relZ == -2) return "Northwest";
        
        return "Unknown";
    }
    
    public boolean isValidPattern(String[] pattern) {
        Map<String, String> currentPattern = getCurrentPositionalPattern();
        
        for (Map.Entry<String, Map<String, String>> entry : POKEMON_PATTERNS.entrySet()) {
            Map<String, String> requiredPattern = entry.getValue();
            
            if (matchesPattern(currentPattern, requiredPattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    private String getPokemonFromPattern(String[] pattern) {
        return checkFlexiblePatterns(pattern);
    }
    
    private String checkFlexiblePatterns(String[] pattern) {
        return findPokemonByPositionalPattern(pattern);
    }
    
    private String findPokemonByPositionalPattern(String[] pattern) {
        // Get current pedestal pattern with positions
        Map<String, String> currentPattern = getCurrentPositionalPattern();
        
        // Check each Pokemon pattern
        for (Map.Entry<String, Map<String, String>> entry : POKEMON_PATTERNS.entrySet()) {
            Map<String, String> requiredPattern = entry.getValue();
            
            if (matchesPattern(currentPattern, requiredPattern)) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    private Map<String, String> getCurrentPositionalPattern() {
        Map<String, String> pattern = new HashMap<>();
        String[] positions = {"north", "east", "south", "west", "northeast", "southeast", "southwest", "northwest"};
        
        int[][] pedestalOffsets = {
            {0, 0, -3},   // north
            {3, 0, 0},    // east
            {0, 0, 3},    // south
            {-3, 0, 0},   // west
            {2, 0, -2},   // northeast
            {2, 0, 2},    // southeast
            {-2, 0, 2},   // southwest
            {-2, 0, -2}   // northwest
        };
        
        for (int i = 0; i < 8; i++) {
            int[] offset = pedestalOffsets[i];
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            
            if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity pedestal) {
                if (pedestal.hasTrophy()) {
                    String pokeball = getPokeball(pedestal.getTrophy());
                    if (pokeball != null) {
                        pattern.put(positions[i], pokeball);
                    }
                }
            }
        }
        
        return pattern;
    }
    
    private boolean matchesPattern(Map<String, String> current, Map<String, String> required) {
        // For partial patterns (RARE), check if all required positions match
        if (required.size() < 8) {
            for (Map.Entry<String, String> entry : required.entrySet()) {
                String position = entry.getKey();
                String requiredBall = entry.getValue();
                String currentBall = current.get(position);
                
                if (!requiredBall.equals(currentBall)) {
                    return false;
                }
            }
            return true;
        }
        
        // For full patterns (LEGENDARIES), check exact match
        for (Map.Entry<String, String> entry : required.entrySet()) {
            String position = entry.getKey();
            String requiredBall = entry.getValue();
            String currentBall = current.get(position);
            
            if (!requiredBall.equals(currentBall)) {
                return false;
            }
        }
        
        return true;
    }
    
    private static final Map<String, List<String>> POKEMON_COMBINATIONS = new HashMap<>();
    private static final Map<String, Map<String, String>> POKEMON_PATTERNS = new HashMap<>();

    static {
        loadPokemonCombinations();
    }

    private static void loadPokemonCombinations() {
        try {
            InputStream inputStream = TrophyAltarBlockEntity.class.getResourceAsStream("/data/poke-notifier/pokemon_combinations.json");
            if (inputStream == null) {
                PokeNotifier.LOGGER.error("Could not find pokemon_combinations.json file");
                return;
            }
            
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(new InputStreamReader(inputStream), JsonObject.class);
            
            // Load all categories
            for (String category : Arrays.asList("LEGENDARIES", "MYTHICALS", "ULTRA_BEASTS", "PARADOX", "ULTRA_RARE", "RARE")) {
                if (jsonObject.has(category)) {
                    JsonObject categoryObject = jsonObject.getAsJsonObject(category);
                    for (Map.Entry<String, JsonElement> entry : categoryObject.entrySet()) {
                        String pokemonName = entry.getKey();
                        JsonObject patternObject = entry.getValue().getAsJsonObject();
                        
                        Map<String, String> pattern = new HashMap<>();
                        for (Map.Entry<String, JsonElement> patternEntry : patternObject.entrySet()) {
                            pattern.put(patternEntry.getKey(), patternEntry.getValue().getAsString());
                        }
                        
                        POKEMON_PATTERNS.put(pokemonName.toLowerCase(), pattern);
                    }
                }
            }
            
            PokeNotifier.LOGGER.info("Loaded {} Pokemon patterns from JSON", POKEMON_PATTERNS.size());
            
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to load Pokemon combinations: {}", e.getMessage(), e);
        }
    }
    

    

    
    // Variables para la secuencia de invocación
    private String pendingPokemon = null;
    private int summoningTimer = 0;
    private boolean isSummoning = false;
    
    // Variables para animación de pokeballs flotantes
    private java.util.List<net.minecraft.entity.ItemEntity> floatingPokeballs = new java.util.ArrayList<>();
    private boolean isAnimatingPokeballs = false;
    private int animationPhase = 0; // 0=elevación, 1=convergencia, 2=fusión
    
    private void startSummoningSequence(String pokemonName) {
        if (world == null || world.isClient) return;
        
        // Iniciar la secuencia
        pendingPokemon = pokemonName;
        summoningTimer = 0;
        isSummoning = true;
        
        // NUEVA FASE 0: Animación de pokeballs flotantes
        startPokeballAnimation();
        
        markDirty(); // Guardar el estado
    }
    
    private void spawnDragonDeathFlash() {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Encontrar jugadores cercanos para aplicar el efecto
        java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
            world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                new net.minecraft.util.math.Box(pos).expand(50), player -> true);
        
        for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
            // Crear el efecto de flash blanco usando el mismo efecto que la muerte del dragón
            // Esto crea una pantalla blanca temporal
            net.minecraft.network.packet.s2c.play.WorldEventS2CPacket flashPacket = 
                new net.minecraft.network.packet.s2c.play.WorldEventS2CPacket(1028, pos, 0, false);
            player.networkHandler.sendPacket(flashPacket);
            
            // Sonido dramático del dragón
            world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 
                net.minecraft.sound.SoundCategory.BLOCKS, 0.8f, 1.5f);
        }
        
        // Efectos de partículas masivas durante el flash
        for (int i = 0; i < 300; i++) {
            double x = pos.getX() + 0.5 + (Math.random() - 0.5) * 8;
            double y = pos.getY() + 1 + Math.random() * 6;
            double z = pos.getZ() + 0.5 + (Math.random() - 0.5) * 8;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.EXPLOSION_EMITTER, 
                x, y, z, 1, 0, 0, 0, 0);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, 
                x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
        }
        
        // Sonidos adicionales para el efecto dramático
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
            net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.5f);
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 
            net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.8f);
    }
    
    private void summonPokemon(String pokemonName) {
        if (world == null || world.isClient) return;
        
        try {
            net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
            
            // Usar Cobblemon API para crear el Pokémon
            com.cobblemon.mod.common.api.pokemon.PokemonProperties props = 
                com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse(pokemonName);
            
            com.cobblemon.mod.common.entity.pokemon.PokemonEntity pokemonEntity = 
                props.createEntity(serverWorld);
            
            // Posicionar en el suelo cerca del altar
            pokemonEntity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
            
            // Spawnear la entidad
            boolean spawned = world.spawnEntity(pokemonEntity);
            
            if (spawned) {
                // Efectos visuales finales de invocación
                spawnSummonEffects();
                
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
                        player.sendMessage(net.minecraft.text.Text.literal("🎆 ").formatted(net.minecraft.util.Formatting.GOLD)
                            .append(net.minecraft.text.Text.literal("LEGENDARY SUMMONING!").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE))
                            .append(net.minecraft.text.Text.literal(" 🎆").formatted(net.minecraft.util.Formatting.GOLD)), false);
                    }
                    player.sendMessage(net.minecraft.text.Text.literal("✨ ").formatted(net.minecraft.util.Formatting.GOLD)
                        .append(net.minecraft.text.Text.literal(pokemonName.toUpperCase()).formatted(net.minecraft.util.Formatting.AQUA))
                        .append(net.minecraft.text.Text.literal(" has been summoned! ").formatted(net.minecraft.util.Formatting.WHITE))
                        .append(net.minecraft.text.Text.literal("✨").formatted(net.minecraft.util.Formatting.GOLD)), false);
                }
                
                // Mensaje global para legendarios/míticos
                if (isLegendaryOrMythical && summoner != null) {
                    sendGlobalSummonMessage(summoner.getName().getString(), pokemonName);
                }
            } else {
                throw new RuntimeException("Failed to spawn Pokemon entity");
            }
            
        } catch (Exception e) {
            // Error en la invocación
            java.util.List<net.minecraft.server.network.ServerPlayerEntity> nearbyPlayers = 
                world.getEntitiesByClass(net.minecraft.server.network.ServerPlayerEntity.class, 
                    new net.minecraft.util.math.Box(pos).expand(10), player -> true);
            
            for (net.minecraft.server.network.ServerPlayerEntity player : nearbyPlayers) {
                player.sendMessage(net.minecraft.text.Text.literal("⚠ ").formatted(net.minecraft.util.Formatting.RED)
                    .append(net.minecraft.text.Text.literal("Summoning failed: ").formatted(net.minecraft.util.Formatting.GOLD))
                    .append(net.minecraft.text.Text.literal(e.getMessage()).formatted(net.minecraft.util.Formatting.WHITE)), false);
                com.zehro_mc.pokenotifier.PokeNotifier.LOGGER.error("Pokemon summoning error: ", e);
            }
        }
    }
    
    private void consumePokeballs() {
        if (world == null || world.isClient) return;
        
        // Consumir todas las pokeballs de los pedestales
        int[][] pedestalOffsets = {
            {0, 0, -3}, {3, 0, 0}, {0, 0, 3}, {-3, 0, 0},
            {2, 0, -2}, {2, 0, 2}, {-2, 0, 2}, {-2, 0, -2}
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity pedestal) {
                if (pedestal.hasTrophy()) {
                    // Usar método público para consistencia
                    pedestal.setTrophy(ItemStack.EMPTY);
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
    
    private void startPokeballAnimation() {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        floatingPokeballs.clear();
        
        // OCULTAR display blocks durante la animación
        hideDisplayBlocksDuringAnimation();
        
        // Crear entidades flotantes para cada pokeball en los pedestales
        int[][] pedestalOffsets = {
            {0, 0, -3}, {3, 0, 0}, {0, 0, 3}, {-3, 0, 0},
            {2, 0, -2}, {2, 0, 2}, {-2, 0, 2}, {-2, 0, -2}
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity pedestal) {
                if (pedestal.hasTrophy()) {
                    ItemStack pokeball = pedestal.getTrophy().copy();
                    
                    // Crear entidad flotante
                    net.minecraft.entity.ItemEntity floatingItem = new net.minecraft.entity.ItemEntity(
                        serverWorld, 
                        pedestalPos.getX() + 0.5, 
                        pedestalPos.getY() + 1.2, 
                        pedestalPos.getZ() + 0.5, 
                        pokeball
                    );
                    
                    // Configurar para que no se recoja y no tenga gravedad
                    floatingItem.setPickupDelay(Integer.MAX_VALUE);
                    floatingItem.setNoGravity(true);
                    floatingItem.setVelocity(0, 0.1, 0); // Velocidad inicial hacia arriba
                    
                    // Spawnear y agregar a la lista
                    if (serverWorld.spawnEntity(floatingItem)) {
                        floatingPokeballs.add(floatingItem);
                    }
                }
            }
        }
        
        isAnimatingPokeballs = true;
        animationPhase = 0;
        
        // Sonido de inicio de animación
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 
            net.minecraft.sound.SoundCategory.BLOCKS, 0.8f, 1.2f);
    }
    
    private void updatePokeballAnimation() {
        if (world == null || world.isClient || !isAnimatingPokeballs) return;
        
        // Limpiar entidades muertas
        floatingPokeballs.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
        
        if (floatingPokeballs.isEmpty()) return;
        
        int animationTick = summoningTimer;
        
        // FASE 1: Elevación (0-15 ticks)
        if (animationTick <= 15) {
            animationPhase = 0;
            for (net.minecraft.entity.ItemEntity pokeball : floatingPokeballs) {
                // Elevación suave con rotación
                pokeball.setVelocity(0, 0.1, 0);
                pokeball.setYaw(pokeball.getYaw() + 8); // Rotación lenta
                
                // Partículas mágicas siguiendo cada pokeball
                spawnPokeballTrail(pokeball);
            }
        }
        // FASE 2: Espiral Horaria (16-40 ticks)
        else if (animationTick <= 40) {
            if (animationPhase != 1) {
                animationPhase = 1;
                // Sonido de espiral
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_BEACON_POWER_SELECT, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.5f);
            }
            
            for (int i = 0; i < floatingPokeballs.size(); i++) {
                net.minecraft.entity.ItemEntity pokeball = floatingPokeballs.get(i);
                
                // Progreso suave de 0 a 1
                double spiralProgress = (animationTick - 15) / 25.0;
                
                // Ángulo base fijo para cada pokeball
                double baseAngle = (i * Math.PI * 2) / floatingPokeballs.size();
                
                // Espiral suave hacia el centro
                double currentRadius = 2.5 - (spiralProgress * 1.5); // De 2.5 a 1.0 bloques
                double currentHeight = pos.getY() + 2.5;
                
                // Posición objetivo en círculo
                double targetX = pos.getX() + 0.5 + Math.cos(baseAngle) * currentRadius;
                double targetZ = pos.getZ() + 0.5 + Math.sin(baseAngle) * currentRadius;
                
                // Movimiento suave hacia la posición
                pokeball.setPos(targetX, currentHeight, targetZ);
                pokeball.setVelocity(0, 0, 0); // Sin velocidad errática
                pokeball.setYaw(pokeball.getYaw() + 8); // Rotación constante
                
                // Trails de espiral
                spawnSpiralTrail(pokeball);
            }
        }
        // FASE 3: Convergencia Final (41-55 ticks)
        else if (animationTick <= 55) {
            if (animationPhase != 2) {
                animationPhase = 2;
                // Sonido de convergencia
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 0.8f, 1.2f);
            }
            
            // Progreso de convergencia
            double convergenceProgress = (animationTick - 40) / 15.0; // 0 a 1
            
            for (int i = 0; i < floatingPokeballs.size(); i++) {
                net.minecraft.entity.ItemEntity pokeball = floatingPokeballs.get(i);
                
                // Posición inicial (final de espiral)
                double baseAngle = (i * Math.PI * 2) / floatingPokeballs.size();
                double startX = pos.getX() + 0.5 + Math.cos(baseAngle) * 1.0;
                double startZ = pos.getZ() + 0.5 + Math.sin(baseAngle) * 1.0;
                
                // Posición final (centro)
                double endX = pos.getX() + 0.5;
                double endZ = pos.getZ() + 0.5;
                
                // Interpolación suave
                double currentX = startX + (endX - startX) * convergenceProgress;
                double currentZ = startZ + (endZ - startZ) * convergenceProgress;
                double currentY = pos.getY() + 2.5 + (0.5 * convergenceProgress); // Subir ligeramente
                
                pokeball.setPos(currentX, currentY, currentZ);
                pokeball.setVelocity(0, 0, 0);
                pokeball.setYaw(pokeball.getYaw() + 12); // Rotación media
                
                // Trails intensos durante convergencia
                spawnConvergenceTrail(pokeball);
            }
        }
        // FASE 4: Fusión y Desvanecimiento (56-60 ticks)
        else if (animationTick <= 60) {
            if (animationPhase != 3) {
                animationPhase = 3;
                // Sonido de fusión
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.ITEM_TOTEM_USE, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.8f);
            }
            
            // Desvanecimiento gradual de pokeballs
            if (animationTick >= 58) {
                // Últimos 2 ticks - remover pokeballs con efectos
                for (net.minecraft.entity.ItemEntity pokeball : floatingPokeballs) {
                    // Efectos finales de desvanecimiento
                    spawnDisappearEffects(pokeball);
                    pokeball.discard();
                }
                floatingPokeballs.clear();
            } else {
                // Convergencia y efectos de fusión
                for (net.minecraft.entity.ItemEntity pokeball : floatingPokeballs) {
                    pokeball.setPos(pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5);
                    pokeball.setVelocity(0, 0, 0);
                    pokeball.setYaw(pokeball.getYaw() + 25); // Rotación muy rápida
                    
                    // Efectos de fusión intensos
                    spawnFusionEffects(pokeball);
                }
            }
        }
    }
    
    private void spawnPokeballTrail(net.minecraft.entity.ItemEntity pokeball) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Partículas mágicas siguiendo la pokeball
        for (int i = 0; i < 3; i++) {
            double x = pokeball.getX() + (Math.random() - 0.5) * 0.3;
            double y = pokeball.getY() + (Math.random() - 0.5) * 0.3;
            double z = pokeball.getZ() + (Math.random() - 0.5) * 0.3;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.WITCH, 
                x, y, z, 1, 0, 0.02, 0, 0.01);
        }
    }
    
    private void spawnSpiralTrail(net.minecraft.entity.ItemEntity pokeball) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Efectos especiales para movimiento en espiral
        for (int i = 0; i < 4; i++) {
            double x = pokeball.getX() + (Math.random() - 0.5) * 0.3;
            double y = pokeball.getY() + (Math.random() - 0.5) * 0.3;
            double z = pokeball.getZ() + (Math.random() - 0.5) * 0.3;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.PORTAL, 
                x, y, z, 1, 0, 0.03, 0, 0.015);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.DRAGON_BREATH, 
                x, y, z, 1, 0, 0.02, 0, 0.01);
        }
    }
    
    private void spawnConvergenceTrail(net.minecraft.entity.ItemEntity pokeball) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Trails más intensos durante convergencia
        for (int i = 0; i < 5; i++) {
            double x = pokeball.getX() + (Math.random() - 0.5) * 0.2;
            double y = pokeball.getY() + (Math.random() - 0.5) * 0.2;
            double z = pokeball.getZ() + (Math.random() - 0.5) * 0.2;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANT, 
                x, y, z, 1, 0, 0.05, 0, 0.02);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, 
                x, y, z, 1, 0, 0.02, 0, 0.01);
        }
    }
    
    private void spawnFusionEffects(net.minecraft.entity.ItemEntity pokeball) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Efectos intensos de fusión
        for (int i = 0; i < 8; i++) {
            double x = pokeball.getX() + (Math.random() - 0.5) * 0.5;
            double y = pokeball.getY() + (Math.random() - 0.5) * 0.5;
            double z = pokeball.getZ() + (Math.random() - 0.5) * 0.5;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, 
                x, y, z, 1, 0, 0.1, 0, 0.05);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.DRAGON_BREATH, 
                x, y, z, 1, 0.1, 0.1, 0.1, 0.03);
        }
    }
    
    private void spawnDisappearEffects(net.minecraft.entity.ItemEntity pokeball) {
        if (world == null || world.isClient) return;
        
        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) world;
        
        // Efectos de desvanecimiento mágico
        for (int i = 0; i < 15; i++) {
            double x = pokeball.getX() + (Math.random() - 0.5) * 0.8;
            double y = pokeball.getY() + (Math.random() - 0.5) * 0.8;
            double z = pokeball.getZ() + (Math.random() - 0.5) * 0.8;
            
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING, 
                x, y, z, 1, 0, 0.2, 0, 0.1);
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANT, 
                x, y, z, 1, 0, 0.1, 0, 0.05);
        }
    }
    
    private void cleanupPokeballAnimation() {
        // Remover todas las entidades flotantes
        for (net.minecraft.entity.ItemEntity pokeball : floatingPokeballs) {
            if (pokeball.isAlive() && !pokeball.isRemoved()) {
                pokeball.discard();
            }
        }
        
        floatingPokeballs.clear();
        isAnimatingPokeballs = false;
        animationPhase = 0;
        
        // RESTAURAR display blocks después de la animación
        restoreDisplayBlocksAfterAnimation();
    }
    
    public void giveGuideBookIfNeeded(net.minecraft.server.network.ServerPlayerEntity player) {
        // Verificar si ya tiene el libro en el inventario
        if (hasGuideBook(player)) {
            return; // Ya tiene el libro, no dar otro
        }
        
        // Crear el libro guía
        ItemStack guideBook = createGuideBook();
        
        // Intentar darlo al jugador
        if (!player.giveItemStack(guideBook)) {
            // Si el inventario está lleno, tirarlo al suelo
            net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
                world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, guideBook);
            world.spawnEntity(itemEntity);
        }
        
        // Notificar al jugador
        player.sendMessage(net.minecraft.text.Text.literal("📚 ").formatted(net.minecraft.util.Formatting.GOLD)
            .append(net.minecraft.text.Text.literal("You received the Pokemon Altar Guide! ").formatted(net.minecraft.util.Formatting.GREEN))
            .append(net.minecraft.text.Text.literal("It contains 3 test patterns to get you started.").formatted(net.minecraft.util.Formatting.GOLD)), false);
    }
    
    private boolean hasGuideBook(net.minecraft.server.network.ServerPlayerEntity player) {
        // Buscar en el inventario principal
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isGuideBook(stack)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isGuideBook(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(net.minecraft.item.Items.WRITTEN_BOOK)) {
            return false;
        }
        
        net.minecraft.component.type.NbtComponent customData = stack.getComponents().get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return false;
        
        net.minecraft.nbt.NbtCompound nbt = customData.copyNbt();
        return nbt.contains("poke_altar_guide") && nbt.getBoolean("poke_altar_guide");
    }
    
    private ItemStack createGuideBook() {
        ItemStack book = new ItemStack(net.minecraft.item.Items.WRITTEN_BOOK);
        
        // Create custom data component
        net.minecraft.nbt.NbtCompound customData = new net.minecraft.nbt.NbtCompound();
        customData.putBoolean("poke_altar_guide", true);
        book.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(customData));
        
        // Set book metadata
        book.set(net.minecraft.component.DataComponentTypes.WRITTEN_BOOK_CONTENT, 
            new net.minecraft.component.type.WrittenBookContentComponent(
                net.minecraft.text.RawFilteredPair.of("Pokemon Altar Guide"),
                "Poke Notifier",
                0,
                createBookPages(),
                true
            ));
        
        return book;
    }
    
    private java.util.List<net.minecraft.text.RawFilteredPair<net.minecraft.text.Text>> createBookPages() {
        java.util.List<net.minecraft.text.RawFilteredPair<net.minecraft.text.Text>> pages = new java.util.ArrayList<>();
        
        // Página 1: Introducción
        pages.add(net.minecraft.text.RawFilteredPair.of(
            net.minecraft.text.Text.literal("§6§lPokemon Altar Guide\n\n")
                .append(net.minecraft.text.Text.literal("§rWelcome! You built the Pokemon Summoning Altar.\n\n"))
                .append(net.minecraft.text.Text.literal("§6This guide has test patterns.\n\n"))
                .append(net.minecraft.text.Text.literal("§7Each Pokemon needs specific pokeballs on pedestals."))));
        
        // Página 2: Cómo usar
        pages.add(net.minecraft.text.RawFilteredPair.of(
            net.minecraft.text.Text.literal("§6§lHow to Use:\n\n")
                .append(net.minecraft.text.Text.literal("§r1. Place pokeballs on pedestals\n\n"))
                .append(net.minecraft.text.Text.literal("§r2. Each has a position: N, E, S, W, NE, SE, SW, NW\n\n"))
                .append(net.minecraft.text.Text.literal("§r3. Right-click altar to summon"))));
        
        // Página 3: Test Pattern 1 - Alakazam
        pages.add(net.minecraft.text.RawFilteredPair.of(
            net.minecraft.text.Text.literal("§6§lPattern 1: §b§lAlakazam\n\n")
                .append(net.minecraft.text.Text.literal("§rN: Quick Ball\n"))
                .append(net.minecraft.text.Text.literal("§rE: Ultra Ball\n"))
                .append(net.minecraft.text.Text.literal("§rS: Luxury Ball\n"))
                .append(net.minecraft.text.Text.literal("§rW: Timer Ball\n\n"))
                .append(net.minecraft.text.Text.literal("§7Others empty.\n\n"))
                .append(net.minecraft.text.Text.literal("§6RARE tier - needs 4 balls!"))));
        
        // Página 4: Test Pattern 2 - Lucario
        pages.add(net.minecraft.text.RawFilteredPair.of(
            net.minecraft.text.Text.literal("§6§lPattern 2: §d§lLucario\n\n")
                .append(net.minecraft.text.Text.literal("§rN: Master, E: Friend\n"))
                .append(net.minecraft.text.Text.literal("§rS: Ultra, W: Luxury\n"))
                .append(net.minecraft.text.Text.literal("§rNE: Repeat, SE: Timer\n"))
                .append(net.minecraft.text.Text.literal("§rSW: Dusk, NW: Quick\n\n"))
                .append(net.minecraft.text.Text.literal("§6ULTRA_RARE - needs all 8!"))));
        
        // Página 5: Consejos
        pages.add(net.minecraft.text.RawFilteredPair.of(
            net.minecraft.text.Text.literal("§6§lTips:\n\n")
                .append(net.minecraft.text.Text.literal("§r• RARE need 3-4 balls\n"))
                .append(net.minecraft.text.Text.literal("§r• ULTRA_RARE+ need all 8\n"))
                .append(net.minecraft.text.Text.literal("§r• Each has unique pattern\n"))
                .append(net.minecraft.text.Text.literal("§r• Legendaries announce globally\n\n"))
                .append(net.minecraft.text.Text.literal("§7Experiment to find more!"))));
        
        return pages;
    }
    
    private void hideDisplayBlocksDuringAnimation() {
        if (world == null || world.isClient) return;
        
        int[][] pedestalOffsets = {
            {0, 0, -3}, {3, 0, 0}, {0, 0, 3}, {-3, 0, 0},
            {2, 0, -2}, {2, 0, 2}, {-2, 0, 2}, {-2, 0, -2}
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            BlockPos displayPos = pedestalPos.up();
            
            // Temporalmente remover display blocks
            if (world.getBlockState(displayPos).getBlock() instanceof com.zehro_mc.pokenotifier.block.TrophyDisplayBlock) {
                world.removeBlock(displayPos, false);
            }
        }
    }
    
    private void restoreDisplayBlocksAfterAnimation() {
        if (world == null || world.isClient) return;
        
        int[][] pedestalOffsets = {
            {0, 0, -3}, {3, 0, 0}, {0, 0, 3}, {-3, 0, 0},
            {2, 0, -2}, {2, 0, 2}, {-2, 0, 2}, {-2, 0, -2}
        };
        
        for (int[] offset : pedestalOffsets) {
            BlockPos pedestalPos = pos.add(offset[0], offset[1], offset[2]);
            
            if (world.getBlockEntity(pedestalPos) instanceof TrophyPedestalBlockEntity pedestal) {
                if (pedestal.hasTrophy()) {
                    // Recrear display block si el pedestal aún tiene item
                    BlockPos displayPos = pedestalPos.up();
                    if (world.getBlockState(displayPos).isAir()) {
                        world.setBlockState(displayPos, com.zehro_mc.pokenotifier.block.ModBlocks.TROPHY_DISPLAY_BLOCK.getDefaultState());
                        if (world.getBlockEntity(displayPos) instanceof com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity displayEntity) {
                            String itemId = net.minecraft.registry.Registries.ITEM.getId(pedestal.getTrophy().getItem()).toString();
                            displayEntity.setTrophyData(itemId, "pedestal");
                        }
                    }
                }
            }
        }
    }
}