/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.globalhunt;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.random.Random;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.networking.GlobalHuntPayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;

import java.util.Iterator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GlobalHuntEvent {
    private final ServerWorld world;
    private final BlockPos coordinates;
    private final String pokemonName;
    private final boolean isShiny;
    private final int durationMinutes;
    
    private PokemonEntity spawnedPokemon;
    private boolean isActive = false;
    private boolean isCaptured = false;
    private ScheduledFuture<?> timeoutTask;
    private ScheduledExecutorService scheduler;
    private BlockPos beaconPos;
    
    public GlobalHuntEvent(ServerWorld world, BlockPos coordinates, String pokemonName, boolean isShiny, int durationMinutes) {
        this.world = world;
        this.coordinates = findGroundLevel(world, coordinates);
        this.pokemonName = pokemonName;
        this.isShiny = isShiny;
        this.durationMinutes = durationMinutes;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void start() {
        if (isActive) return;
        
        isActive = true;
        
        PokeNotifier.LOGGER.info("Starting Global Hunt Event: {} {} at {} for {} minutes", 
            isShiny ? "Shiny" : "", pokemonName, coordinates, durationMinutes);
        
        // Send global notifications first
        sendGlobalNotifications();
        
        // Spawn the Pokemon after notifications
        spawnPokemon();
        
        // Schedule timeout
        timeoutTask = scheduler.schedule(this::timeout, durationMinutes, TimeUnit.MINUTES);
        
        // Schedule periodic Pokemon status checks (every 30 seconds)
        scheduler.scheduleAtFixedRate(this::checkPokemonStatus, 30, 30, TimeUnit.SECONDS);
        
        // Schedule position maintenance (every 5 seconds)
        scheduler.scheduleAtFixedRate(this::maintainPokemonPosition, 5, 5, TimeUnit.SECONDS);
        
        PokeNotifier.LOGGER.info("Global Hunt Event fully started: {} at {}", pokemonName, coordinates);
    }
    
    private void spawnPokemon() {
        try {
            // Force load the chunk and wait
            world.getChunk(coordinates.getX() >> 4, coordinates.getZ() >> 4);
            
            // Use server execute to ensure proper thread
            world.getServer().execute(() -> {
                try {
                    // Calculate dynamic level based on server average
                    int dynamicLevel = calculateDynamicLevel();
                    
                    // Create entity using PokemonProperties
                    PokemonProperties props = PokemonProperties.Companion.parse(pokemonName.toLowerCase());
                    if (isShiny) {
                        props.setShiny(true);
                    }
                    props.setLevel(dynamicLevel);
                    
                    spawnedPokemon = props.createEntity(world);
                    // Spawn Pokemon above the red glass (2 blocks above beacon)
                    double spawnX = coordinates.getX() + 0.5;
                    double spawnY = coordinates.getY() + 2.0;
                    double spawnZ = coordinates.getZ() + 0.5;
                    
                    spawnedPokemon.setPosition(spawnX, spawnY, spawnZ);
                    
                    // Make Pokemon immobile but able to battle
                    spawnedPokemon.setNoGravity(true); // Float in place
                    spawnedPokemon.setInvulnerable(false); // Can still take damage in battle
                    spawnedPokemon.setAiDisabled(false); // Keep AI for battle
                    
                    // Prevent movement but allow battle
                    spawnedPokemon.getNavigation().stop();
                    
                    PokeNotifier.LOGGER.info("Spawned immobile {} at position: {}, {}, {}", pokemonName, spawnX, spawnY, spawnZ);
                    
                    // Add Global Hunt identifier
                    spawnedPokemon.getPokemon().getPersistentData().putBoolean("pokenotifier_global_hunt", true);
                    
                    // Add special effects
                    addGlobalHuntEffects(spawnedPokemon);
                    
                    // Clear area first, then create terrain, then spawn Pokemon
                    clearSpawnArea();
                    createGlobalHuntTerrain();
                    createBeacon();
                    
                    // Spawn in world
                    boolean spawned = world.spawnEntity(spawnedPokemon);
                    
                    PokeNotifier.LOGGER.info("Global Hunt spawn: {} {} Lvl {} at {} - Success: {}", 
                        isShiny ? "Shiny" : "", pokemonName, dynamicLevel, coordinates, spawned);
                        
                    if (!spawned) {
                        PokeNotifier.LOGGER.error("Failed to spawn Global Hunt Pokemon - world.spawnEntity returned false");
                    }
                } catch (Exception e) {
                    PokeNotifier.LOGGER.error("Exception during Global Hunt spawn: {}", e.getMessage(), e);
                }
            });
                
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to spawn Pokemon for Global Hunt: {}", e.getMessage(), e);
            cancel();
        }
    }
    
    private void sendGlobalNotifications() {
        // Play thunder sound globally
        world.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.playSoundToPlayer(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 1.0f, 1.0f);
        });
        
        // Calculate dynamic level for display
        int dynamicLevel = calculateDynamicLevel();
        
        // Send dramatic messages with original format
        String shinyText = isShiny ? "SHINY " : "";
        String worldName = getWorldDisplayName();
        
        Text announcement = Text.literal("⚡ GLOBAL HUNT ALERT! ⚡").formatted(Formatting.RED, Formatting.BOLD)
            .append(Text.literal("\nA " + shinyText + pokemonName + " Lvl " + dynamicLevel + " has been spotted!").formatted(Formatting.GOLD))
            .append(Text.literal("\nLocation: " + coordinates.getX() + ", " + coordinates.getY() + ", " + coordinates.getZ()).formatted(Formatting.YELLOW))
            .append(Text.literal("\nWorld: " + worldName).formatted(Formatting.YELLOW))
            .append(Text.literal("\nTime Limit: " + durationMinutes + " minutes!").formatted(Formatting.GREEN))
            .append(Text.literal("\nFirst to capture wins!").formatted(Formatting.RED, Formatting.BOLD));
        
        world.getServer().getPlayerManager().broadcast(announcement, false);
        
        // Send networking payload to clients for special effects
        GlobalHuntPayload payload = new GlobalHuntPayload(
            GlobalHuntPayload.ACTION_START,
            pokemonName,
            world.getRegistryKey().getValue().toString(),
            coordinates.getX(),
            coordinates.getY(), 
            coordinates.getZ(),
            isShiny,
            durationMinutes
        );
        
        world.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            ServerPlayNetworking.send(player, payload);
        });
    }
    
    private String getWorldDisplayName() {
        String worldKey = world.getRegistryKey().getValue().toString();
        return switch (worldKey) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> worldKey;
        };
    }
    
    public void onPokemonCaptured(String playerName) {
        if (!isActive || isCaptured) return;
        
        isCaptured = true;
        
        // Announce winner with original format
        Text winnerAnnouncement = Text.literal("🏆 GLOBAL HUNT COMPLETE! 🏆").formatted(Formatting.GREEN, Formatting.BOLD)
            .append(Text.literal("\n" + playerName + " captured the " + (isShiny ? "Shiny " : "") + pokemonName + "!").formatted(Formatting.GOLD))
            .append(Text.literal("\nCongratulations to the winner!").formatted(Formatting.YELLOW));
        
        world.getServer().getPlayerManager().broadcast(winnerAnnouncement, false);
        
        // Send completion payload to clients
        GlobalHuntPayload payload = new GlobalHuntPayload(
            GlobalHuntPayload.ACTION_CAPTURED,
            pokemonName,
            world.getRegistryKey().getValue().toString(),
            coordinates.getX(),
            coordinates.getY(),
            coordinates.getZ(),
            isShiny,
            0 // Duration not needed for completion
        );
        
        world.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            ServerPlayNetworking.send(player, payload);
        });
        
        PokeNotifier.LOGGER.info("Global Hunt completed: {} captured by {}", pokemonName, playerName);
        
        // Destroy the beacon when Pokemon is captured
        destroyBeacon();
        
        // End the event
        cancel();
    }
    
    private void timeout() {
        if (!isActive || isCaptured) return;
        
        // Announce timeout with original format
        Text timeoutAnnouncement = Text.literal("⏰ GLOBAL HUNT EXPIRED! ⏰").formatted(Formatting.RED, Formatting.BOLD)
            .append(Text.literal("\nThe " + (isShiny ? "Shiny " : "") + pokemonName + " has escaped!").formatted(Formatting.YELLOW))
            .append(Text.literal("\nBetter luck next time...").formatted(Formatting.GRAY));
        
        world.getServer().getPlayerManager().broadcast(timeoutAnnouncement, false);
        
        // Send timeout payload to clients
        GlobalHuntPayload payload = new GlobalHuntPayload(
            GlobalHuntPayload.ACTION_TIMEOUT,
            pokemonName,
            world.getRegistryKey().getValue().toString(),
            coordinates.getX(),
            coordinates.getY(),
            coordinates.getZ(),
            isShiny,
            0
        );
        
        world.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            ServerPlayNetworking.send(player, payload);
        });
        
        PokeNotifier.LOGGER.info("Global Hunt timed out: {} escaped", pokemonName);
        
        // Remove the Pokemon
        if (spawnedPokemon != null && !spawnedPokemon.isRemoved()) {
            spawnedPokemon.discard();
        }
        
        // Destroy the beacon on timeout too
        destroyBeacon();
        
        // Leave the terrain as a permanent mark
        PokeNotifier.LOGGER.info("Global Hunt terrain remains as a monument at {}", coordinates);
        
        cancel();
    }
    
    public void cancel() {
        if (!isActive) return;
        
        isActive = false;
        
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(false);
        }
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        PokeNotifier.LOGGER.info("Global Hunt Event cancelled");
    }
    
    /**
     * Makes a block indestructible using the same method as trophy blocks
     */
    private void makeBlockIndestructible(ServerWorld world, BlockPos pos) {
        // Get current block and replace with indestructible version
        Block currentBlock = world.getBlockState(pos).getBlock();
        
        if (currentBlock == Blocks.BEACON) {
            // Create indestructible beacon
            world.setBlockState(pos, Blocks.BEACON.getDefaultState());
            // Note: We can't modify block properties at runtime, but the bedrock cage provides protection
        } else if (currentBlock == Blocks.RED_STAINED_GLASS) {
            // Red glass is protected by bedrock cage
            world.setBlockState(pos, Blocks.RED_STAINED_GLASS.getDefaultState());
        } else if (currentBlock == Blocks.IRON_BLOCK) {
            // Iron blocks are protected by bedrock cage
            world.setBlockState(pos, Blocks.IRON_BLOCK.getDefaultState());
        }
        
        PokeNotifier.LOGGER.debug("Protected Global Hunt block at {}", pos);
    }
    

    
    /**
     * Checks if Pokemon is still alive and active
     */
    public void checkPokemonStatus() {
        if (!isActive || isCaptured || spawnedPokemon == null) return;
        
        // Check if Pokemon was killed or removed
        if (spawnedPokemon.isRemoved() || spawnedPokemon.isDead()) {
            PokeNotifier.LOGGER.info("Global Hunt Pokemon {} was defeated/killed, ending event", pokemonName);
            
            // Announce Pokemon defeat
            world.getServer().getPlayerManager().broadcast(
                Text.literal("💀 Global Hunt ").formatted(Formatting.RED)
                    .append(Text.literal((isShiny ? "Shiny " : "") + pokemonName).formatted(Formatting.YELLOW))
                    .append(Text.literal(" was defeated! Event ended.").formatted(Formatting.GRAY)), 
                false
            );
            
            // Destroy beacon and end event
            destroyBeacon();
            cancel();
        }
    }
    
    /**
     * Keeps Pokemon in the center position while allowing battle
     */
    public void maintainPokemonPosition() {
        if (!isActive || isCaptured || spawnedPokemon == null || spawnedPokemon.isRemoved()) return;
        
        // Check if Pokemon moved too far from center
        double centerX = coordinates.getX() + 0.5;
        double centerY = coordinates.getY() + 2.0;
        double centerZ = coordinates.getZ() + 0.5;
        
        double currentX = spawnedPokemon.getX();
        double currentZ = spawnedPokemon.getZ();
        
        // If Pokemon moved more than 2 blocks from center, teleport back
        double distance = Math.sqrt(Math.pow(currentX - centerX, 2) + Math.pow(currentZ - centerZ, 2));
        
        if (distance > 2.0) {
            spawnedPokemon.setPosition(centerX, centerY, centerZ);
            spawnedPokemon.setNoGravity(true);
            PokeNotifier.LOGGER.debug("Repositioned Global Hunt Pokemon back to center");
        }
        
        // Ensure it stays floating
        if (spawnedPokemon.hasNoGravity() == false) {
            spawnedPokemon.setNoGravity(true);
        }
    }
    
    /**
     * Destroys the beacon and its protection when event ends
     */
    private void destroyBeacon() {
        if (beaconPos != null) {
            // Remove bedrock protection and red glass
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = -2; y <= 1; y++) {
                        BlockPos protectPos = beaconPos.add(x, y, z);
                        Block block = world.getBlockState(protectPos).getBlock();
                        if (block == Blocks.BEDROCK || block == Blocks.RED_STAINED_GLASS) {
                            world.setBlockState(protectPos, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
            
            // Remove beacon and base
            world.setBlockState(beaconPos, Blocks.AIR.getDefaultState());
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos basePos = beaconPos.add(x, -1, z);
                    world.setBlockState(basePos, Blocks.AIR.getDefaultState());
                }
            }
            
            PokeNotifier.LOGGER.info("Destroyed beacon at {}", beaconPos);
        }
    }
    
    /**
     * Finds safe surface level for spawning, prioritizing land over water
     */
    private BlockPos findGroundLevel(ServerWorld world, BlockPos pos) {
        int seaLevel = world.getSeaLevel();
        
        // First try to find land within a reasonable radius (prefer land 80% of the time)
        if (world.getRandom().nextFloat() < 0.8f) {
            BlockPos landPos = findNearbyLand(world, pos, 100);
            if (landPos != null) {
                PokeNotifier.LOGGER.info("Found nearby land, spawning at: {}", landPos);
                return landPos;
            }
        }
        
        // Check if we're over water - if so, spawn at sea level (20% chance or fallback)
        BlockPos seaLevelPos = new BlockPos(pos.getX(), seaLevel, pos.getZ());
        if (isOverWater(world, seaLevelPos)) {
            PokeNotifier.LOGGER.info("Spawning over water at sea level: {}", seaLevelPos);
            return seaLevelPos;
        }
        
        // Start from world surface and work down to find the first solid, safe surface
        int surfaceY = world.getTopY() - 1;
        
        // Find the actual surface by going down from the top
        for (int y = surfaceY; y >= world.getBottomY() + 10; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            Block blockAt = world.getBlockState(checkPos).getBlock();
            Block blockBelow = world.getBlockState(checkPos.down()).getBlock();
            
            // Check if we found a good surface level
            if (isSafeGroundBlock(blockBelow) && isSafeAirBlock(blockAt)) {
                // Verify it's not underground by checking if there's sky access
                if (hasOpenSky(world, checkPos)) {
                    // Ensure there's enough space (3 blocks high)
                    if (isSafeAirBlock(world.getBlockState(checkPos.up()).getBlock()) &&
                        isSafeAirBlock(world.getBlockState(checkPos.up(2)).getBlock())) {
                        return checkPos;
                    }
                }
            }
        }
        
        // Fallback: force create a safe platform at a reasonable height
        BlockPos fallbackPos = new BlockPos(pos.getX(), Math.max(seaLevel + 5, 70), pos.getZ());
        PokeNotifier.LOGGER.warn("No safe surface found, creating platform at {}", fallbackPos);
        return fallbackPos;
    }
    
    private boolean isOverWater(ServerWorld world, BlockPos pos) {
        // Check a 5x5 area around the position to see if it's mostly water
        int waterBlocks = 0;
        int totalBlocks = 0;
        
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();
                totalBlocks++;
                if (block == Blocks.WATER) {
                    waterBlocks++;
                }
            }
        }
        
        // If more than 60% is water, consider it a water area
        return (waterBlocks / (float) totalBlocks) > 0.6f;
    }
    
    private BlockPos findNearbyLand(ServerWorld world, BlockPos center, int radius) {
        Random random = world.getRandom();
        
        // Try 20 random positions within radius to find land
        for (int attempts = 0; attempts < 20; attempts++) {
            int x = center.getX() + random.nextInt(radius * 2) - radius;
            int z = center.getZ() + random.nextInt(radius * 2) - radius;
            
            BlockPos testPos = new BlockPos(x, world.getSeaLevel() + 5, z);
            
            // Check if this position is over land
            if (!isOverWater(world, testPos)) {
                // Find the actual ground level at this position
                for (int y = world.getTopY() - 1; y >= world.getBottomY() + 10; y--) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    Block blockAt = world.getBlockState(checkPos).getBlock();
                    Block blockBelow = world.getBlockState(checkPos.down()).getBlock();
                    
                    if (isSafeGroundBlock(blockBelow) && isSafeAirBlock(blockAt)) {
                        if (hasOpenSky(world, checkPos)) {
                            return checkPos;
                        }
                    }
                }
            }
        }
        
        return null; // No land found
    }
    
    private boolean hasOpenSky(ServerWorld world, BlockPos pos) {
        // Check if there's a clear path to sky (no solid blocks above for at least 10 blocks)
        for (int y = 1; y <= 10; y++) {
            Block blockAbove = world.getBlockState(pos.up(y)).getBlock();
            if (blockAbove != Blocks.AIR && blockAbove.getDefaultState().isOpaque()) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isSafeGroundBlock(Block block) {
        return block != Blocks.AIR && 
               block != Blocks.LAVA && 
               block != Blocks.WATER && 
               block != Blocks.FIRE &&
               block != Blocks.MAGMA_BLOCK &&
               block.getDefaultState().isOpaque();
    }
    
    private boolean isSafeAirBlock(Block block) {
        return block == Blocks.AIR || 
               block == Blocks.SHORT_GRASS || 
               block == Blocks.TALL_GRASS ||
               block == Blocks.FERN;
    }
    
    /**
     * Adds special visual effects to Global Hunt Pokemon
     */
    private void addGlobalHuntEffects(PokemonEntity pokemon) {
        // Use the same glow method as regular rare Pokemon detection
        int glowingTicks = Integer.MAX_VALUE; // Permanent glow for Global Hunt
        pokemon.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, glowingTicks, 0, false, false));
        
        // Add beacon-like effects for visibility and power
        pokemon.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 1, false, false));
        pokemon.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        pokemon.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 0, false, false));
        
        PokeNotifier.LOGGER.info("Applied Global Hunt effects (including glow) to {} at {}", pokemonName, coordinates);
    }
    
    /**
     * Clears the spawn area of any existing structures, ensuring safe Pokemon spawn
     */
    private void clearSpawnArea() {
        // Clear a larger area: 11x11 base and extra height for Pokemon spawn
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -2; y <= 8; y++) { // Extra height for Pokemon spawn at Y+2
                    BlockPos clearPos = coordinates.add(x, y, z);
                    Block block = world.getBlockState(clearPos).getBlock();
                    
                    // Don't clear bedrock or important blocks
                    if (block != Blocks.BEDROCK && block != Blocks.END_PORTAL_FRAME) {
                        world.setBlockState(clearPos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
        
        // Extra clearing around Pokemon spawn position (Y+2)
        BlockPos pokemonSpawnPos = coordinates.up(2);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 4; y++) { // Clear 5 blocks above Pokemon spawn
                    BlockPos clearPos = pokemonSpawnPos.add(x, y, z);
                    Block block = world.getBlockState(clearPos).getBlock();
                    
                    if (block != Blocks.BEDROCK && block != Blocks.END_PORTAL_FRAME) {
                        world.setBlockState(clearPos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
        
        PokeNotifier.LOGGER.info("Cleared expanded spawn area at {} (11x11x10 + Pokemon safety zone)", coordinates);
    }
    
    /**
     * Creates enhanced ruined portal-like terrain with netherrack and magma
     */
    private void createGlobalHuntTerrain() {
        Random random = world.getRandom();
        
        // Create a 7x7 area around the spawn point
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos pos = coordinates.add(x, 0, z);
                double distance = Math.sqrt(x * x + z * z);
                
                // Create circular pattern with some randomness
                if (distance <= 3.5) {
                    // Replace ground blocks
                    BlockPos groundPos = pos.down();
                    
                    if (distance <= 1.5) {
                        // Center: Crying obsidian, blackstone, and netherrack
                        float rand = random.nextFloat();
                        if (rand < 0.5f) {
                            world.setBlockState(groundPos, Blocks.CRYING_OBSIDIAN.getDefaultState());
                        } else if (rand < 0.8f) {
                            world.setBlockState(groundPos, Blocks.BLACKSTONE.getDefaultState());
                        } else {
                            world.setBlockState(groundPos, Blocks.NETHERRACK.getDefaultState());
                        }
                    } else if (distance <= 2.5) {
                        // Middle ring: Mix with more netherrack and magma
                        float rand = random.nextFloat();
                        if (rand < 0.3f) {
                            world.setBlockState(groundPos, Blocks.BLACKSTONE.getDefaultState());
                        } else if (rand < 0.5f) {
                            world.setBlockState(groundPos, Blocks.NETHERRACK.getDefaultState());
                        } else if (rand < 0.7f) {
                            world.setBlockState(groundPos, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState());
                        } else if (rand < 0.85f) {
                            world.setBlockState(groundPos, Blocks.GLOWSTONE.getDefaultState());
                        } else {
                            world.setBlockState(groundPos, Blocks.GILDED_BLACKSTONE.getDefaultState());
                        }
                    } else {
                        // Outer ring: Scattered blocks
                        float rand = random.nextFloat();
                        if (rand < 0.2f) {
                            world.setBlockState(groundPos, Blocks.BLACKSTONE.getDefaultState());
                        } else if (rand < 0.3f) {
                            world.setBlockState(groundPos, Blocks.NETHERRACK.getDefaultState());
                        }
                    }
                    
                    // Add scattered obsidian pillars with netherrack
                    if (distance > 1.0 && distance <= 2.5 && random.nextFloat() < 0.2f) {
                        int height = random.nextInt(3) + 1;
                        for (int y = 0; y < height; y++) {
                            BlockPos pillarPos = pos.up(y);
                            if (world.getBlockState(pillarPos).isAir()) {
                                if (y == height - 1 && random.nextFloat() < 0.4f) {
                                    world.setBlockState(pillarPos, Blocks.CRYING_OBSIDIAN.getDefaultState());
                                } else if (random.nextFloat() < 0.7f) {
                                    world.setBlockState(pillarPos, Blocks.OBSIDIAN.getDefaultState());
                                } else {
                                    world.setBlockState(pillarPos, Blocks.NETHERRACK.getDefaultState());
                                }
                            }
                        }
                    }
                    
                    // Add glowstone blocks underground for ambient lighting
                    if (distance <= 2.5 && random.nextFloat() < 0.15f) {
                        BlockPos glowPos = pos.down(random.nextInt(3) + 1);
                        if (world.getBlockState(glowPos).getBlock() != Blocks.BEDROCK) {
                            world.setBlockState(glowPos, Blocks.GLOWSTONE.getDefaultState());
                        }
                    }
                }
            }
        }
        
        PokeNotifier.LOGGER.info("Created enhanced Global Hunt terrain at {}", coordinates);
    }
    
    /**
     * Creates an indestructible beacon underground for area effects
     */
    private void createBeacon() {
        beaconPos = coordinates.down(2);
        
        // Create beacon base (3x3 of iron blocks)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos basePos = beaconPos.add(x, -1, z);
                world.setBlockState(basePos, Blocks.IRON_BLOCK.getDefaultState());
            }
        }
        
        // Place the beacon and make it indestructible
        world.setBlockState(beaconPos, Blocks.BEACON.getDefaultState());
        makeBlockIndestructible(world, beaconPos);
        
        // Make iron base indestructible too
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos basePos = beaconPos.add(x, -1, z);
                makeBlockIndestructible(world, basePos);
            }
        }
        
        // Make beacon area indestructible by surrounding with bedrock and glass on top
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -2; y <= 1; y++) {
                    BlockPos protectPos = beaconPos.add(x, y, z);
                    
                    if (y == 1) {
                        // Top layer: Red stained glass for epic effect
                        if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                            world.setBlockState(protectPos, Blocks.RED_STAINED_GLASS.getDefaultState());
                            // Make glass indestructible
                            makeBlockIndestructible(world, protectPos);
                        }
                    } else {
                        // Other layers: Bedrock protection around edges
                        if (Math.abs(x) == 2 || Math.abs(z) == 2 || y == -2) {
                            world.setBlockState(protectPos, Blocks.BEDROCK.getDefaultState());
                        }
                    }
                }
            }
        }
        
        PokeNotifier.LOGGER.info("Created protected beacon at {}", beaconPos);
    }
    
    /**
     * Calculates dynamic level based on server Pokemon average
     */
    private int calculateDynamicLevel() {
        try {
            int totalLevels = 0;
            int pokemonCount = 0;
            
            // Scan all online players' Pokemon
            for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
                // Check party
                PokemonStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
                Iterator<Pokemon> partyIterator = party.iterator();
                while (partyIterator.hasNext()) {
                    Pokemon p = partyIterator.next();
                    if (p != null) {
                        totalLevels += p.getLevel();
                        pokemonCount++;
                    }
                }
                
                // Check PC (sample first 30 to avoid lag)
                PokemonStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
                Iterator<Pokemon> pcIterator = pc.iterator();
                int pcSampled = 0;
                while (pcIterator.hasNext() && pcSampled < 30) {
                    Pokemon p = pcIterator.next();
                    if (p != null) {
                        totalLevels += p.getLevel();
                        pokemonCount++;
                        pcSampled++;
                    }
                }
            }
            
            if (pokemonCount == 0) {
                return 35; // Fallback level
            }
            
            int averageLevel = totalLevels / pokemonCount;
            
            // Apply rarity-based bonus
            int bonus = isShiny ? 15 : 10; // Shiny gets higher bonus
            int finalLevel = averageLevel + bonus;
            
            // Ensure reasonable bounds
            return Math.max(20, Math.min(80, finalLevel));
            
        } catch (Exception e) {
            PokeNotifier.LOGGER.warn("Failed to calculate dynamic level: {}", e.getMessage());
            return 35; // Safe fallback
        }
    }
    
    // Getters
    public boolean isActive() { return isActive; }
    public boolean isCaptured() { return isCaptured; }
    public ServerWorld getWorld() { return world; }
    public BlockPos getCoordinates() { return coordinates; }
    public String getPokemonName() { return pokemonName; }
    public boolean isShiny() { return isShiny; }
    public int getDurationMinutes() { return durationMinutes; }
    public PokemonEntity getSpawnedPokemon() { return spawnedPokemon; }
}