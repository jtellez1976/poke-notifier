/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.events;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigServer;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.api.PokeNotifierApi;
import com.zehro_mc.pokenotifier.networking.SwarmStatusPayload;
import com.zehro_mc.pokenotifier.events.SwarmStatistics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SwarmEventManager {
    private static SwarmEventManager instance;
    private final MinecraftServer server;
    private final ConfigServer config;
    private SwarmConfig swarmConfig;
    
    private long lastSwarmEndTime = 0;
    private int swarmCheckTimer = 0;
    private final Set<java.util.UUID> swarmEntities = new HashSet<>();

    public SwarmEventManager(MinecraftServer server) {
        this.server = server;
        this.config = ConfigManager.getServerConfig();
        this.swarmConfig = SwarmConfig.load();
        instance = this;
        
        // Initialize statistics system
        PokeNotifier.LOGGER.info("[SwarmManager] Initializing swarm statistics system...");
        SwarmStatistics.initialize();
        
        // Test write to ensure the system works
        SwarmStatistics.saveStatistics();
        
        PokeNotifier.LOGGER.info("[SwarmManager] SwarmEventManager initialized - System enabled: {}, Active swarm: {}", 
            swarmConfig.system_enabled, SwarmStatistics.hasActiveSwarm());
    }

    public static SwarmEventManager getInstance() {
        return instance;
    }

    public void tick() {
        // Reload config every few ticks to ensure sync (not every tick for performance)
        if (swarmCheckTimer % 100 == 0) { // Every 5 seconds
            SwarmConfig newConfig = SwarmConfig.load();
            if (newConfig.system_enabled != swarmConfig.system_enabled) {
                // Reduced logging
            }
            swarmConfig = newConfig;
        }
        
        if (!swarmConfig.system_enabled) return;

        if (SwarmStatistics.hasActiveSwarm()) {
            updateSwarmEntities();
            
            if (isSwarmExpired()) {
                endCurrentSwarm("time");
            }
            // Remove automatic ending by entities - let admin decide when to end
        }

        swarmCheckTimer++;
        if (swarmCheckTimer >= swarmConfig.check_interval_minutes * 1200) {
            swarmCheckTimer = 0;
            attemptSwarmGeneration();
        }
    }

    private void attemptSwarmGeneration() {
        if (SwarmStatistics.hasActiveSwarm()) return;

        long currentTime = System.currentTimeMillis();
        long timeSinceLastSwarm = currentTime - lastSwarmEndTime;
        long cooldownMs = swarmConfig.cooldown_minutes * 60 * 1000L;
        
        if (timeSinceLastSwarm < cooldownMs) return;

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll >= swarmConfig.start_chance_percent) return;

        generateNewSwarm();
    }

    private void generateNewSwarm() {
        try {
            List<String> allPokemon = PokeNotifierApi.getAllPokemonNames().toList();
            if (allPokemon.isEmpty()) return;

            String selectedPokemon = allPokemon.get(ThreadLocalRandom.current().nextInt(allPokemon.size()));
            BlockPos swarmLocation = generateRemoteLocation();
            if (swarmLocation == null) return;

            ServerWorld overworld = server.getOverworld();
            Biome biome = overworld.getBiome(swarmLocation).value();
            String biomeName = getBiomeName(overworld, swarmLocation);

            startSwarm(selectedPokemon, swarmLocation, biomeName);
            
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Error generating swarm", e);
        }
    }

    public void startSwarm(String pokemonName, BlockPos location, String biomeName) {
        startSwarm(pokemonName, location, biomeName, "SYSTEM");
    }
    
    public void startSwarm(String pokemonName, BlockPos location, String biomeName, String triggeredBy) {
        swarmEntities.clear();

        announceSwarmStart(pokemonName, biomeName, location);
        int spawnedCount = spawnSwarmPokemon(location, pokemonName, false); // One shiny guaranteed per swarm
        
        // Record statistics after spawning to get accurate count
        String swarmType = biomeName.equals("Admin Location") ? "MANUAL" : "AUTOMATIC";
        SwarmStatistics.recordSwarmStart(pokemonName, swarmType, triggeredBy, location, biomeName, spawnedCount);
        
        syncStatusWithClients();

        PokeNotifier.LOGGER.info("[SwarmManager] Started {} swarm: {} by {} (spawned: {})", 
            biomeName.equals("Admin Location") ? "manual" : "automatic", pokemonName, triggeredBy, spawnedCount);
    }

    public boolean startManualSwarm(String pokemonName) {
        return startManualSwarm(pokemonName, "ADMIN");
    }
    
    public boolean startManualSwarm(String pokemonName, String adminName) {
        // Manual swarms are independent of automatic system - force start even if one exists
        if (SwarmStatistics.hasActiveSwarm()) {
            endCurrentSwarm("admin");
        }

        BlockPos location = generateRemoteLocation();
        if (location == null) {
            PokeNotifier.LOGGER.warn("[SwarmManager] Could not generate remote location for swarm");
            return false;
        }

        ServerWorld overworld = server.getOverworld();
        Biome biome = overworld.getBiome(location).value();
        String biomeName = getBiomeName(overworld, location);

        startSwarm(pokemonName, location, biomeName, adminName);
        return true;
    }
    
    public boolean startManualSwarmAt(String pokemonName, BlockPos playerPos, String adminName) {
        // Manual swarms are independent of automatic system - force start even if one exists
        if (SwarmStatistics.hasActiveSwarm()) {
            endCurrentSwarm("admin");
        }

        ServerWorld overworld = server.getOverworld();
        
        // Calculate safe ground level near admin (2 blocks above solid ground)
        BlockPos location = findSafeAdminSpawnLocation(overworld, playerPos);
        if (location == null) {
            PokeNotifier.LOGGER.warn("[SwarmManager] Could not find safe spawn location near admin");
            return false;
        }

        Biome biome = overworld.getBiome(location).value();
        String biomeName = "Admin Location";

        startSwarm(pokemonName, location, biomeName, adminName);
        return true;
    }

    public void endCurrentSwarm() {
        endCurrentSwarm("admin");
    }
    
    public void endCurrentSwarm(String reason) {
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        if (current == null) {
            PokeNotifier.LOGGER.info("[SwarmManager] Attempted to end swarm but none is active");
            return;
        }

        // Record statistics before clearing data
        long duration = System.currentTimeMillis() - current.startTimestamp;
        int durationMinutes = (int) (duration / (60 * 1000));
        SwarmStatistics.recordSwarmEnd(current.pokemonName, reason.toUpperCase(), current.entitiesCaptured, durationMinutes);

        announceSwarmEnd(reason, current.pokemonName, current.entitiesCaptured);

        lastSwarmEndTime = System.currentTimeMillis();
        swarmEntities.clear();

        syncStatusWithClients();
        PokeNotifier.LOGGER.info("[SwarmManager] Ended swarm (reason: {})", reason);
    }

    private BlockPos generateRemoteLocation() {
        ServerWorld overworld = server.getOverworld();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList().stream()
                .filter(p -> p.getServerWorld().getRegistryKey() == net.minecraft.server.world.ServerWorld.OVERWORLD)
                .toList();
        
        if (players.isEmpty()) {
            return generateFallbackLocation(overworld);
        }

        for (int attempt = 0; attempt < 100; attempt++) {
            ServerPlayerEntity randomPlayer = players.get(ThreadLocalRandom.current().nextInt(players.size()));
            BlockPos playerPos = randomPlayer.getBlockPos();
            
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            int distance = ThreadLocalRandom.current().nextInt(swarmConfig.min_distance, swarmConfig.max_distance);
            
            int x = playerPos.getX() + (int) (Math.cos(angle) * distance);
            int z = playerPos.getZ() + (int) (Math.sin(angle) * distance);
            
            BlockPos candidate = validateAndAdjustLocation(overworld, x, z);
            if (candidate != null && isValidSwarmLocation(candidate, players)) {
                return candidate;
            }
        }
        
        return generateFallbackLocation(overworld);
    }
    
    private BlockPos generateFallbackLocation(ServerWorld world) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(-5000, 5000);
            int z = ThreadLocalRandom.current().nextInt(-5000, 5000);
            BlockPos candidate = validateAndAdjustLocation(world, x, z);
            if (candidate != null) {
                return candidate;
            }
        }
        return new BlockPos(0, 64, 0); // Ultimate fallback
    }
    
    private BlockPos validateAndAdjustLocation(ServerWorld world, int x, int z) {
        // Force chunk loading to get accurate terrain data
        world.getChunk(x >> 4, z >> 4);
        
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        
        // Check if location is safe (not in lava, void, etc.)
        if (y < world.getSeaLevel() - 10 || y > world.getTopY() - 5) {
            return null;
        }
        
        // Check for lava or dangerous blocks
        net.minecraft.block.BlockState blockBelow = world.getBlockState(pos.down());
        net.minecraft.block.BlockState blockAt = world.getBlockState(pos);
        
        if (blockBelow.getBlock() == net.minecraft.block.Blocks.LAVA ||
            blockAt.getBlock() == net.minecraft.block.Blocks.LAVA ||
            blockBelow.getBlock() == net.minecraft.block.Blocks.WATER) {
            // Try to find nearby safe ground
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    BlockPos testPos = new BlockPos(x + dx, world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x + dx, z + dz), z + dz);
                    net.minecraft.block.BlockState testBelow = world.getBlockState(testPos.down());
                    if (testBelow.isSolidBlock(world, testPos.down()) && 
                        testBelow.getBlock() != net.minecraft.block.Blocks.LAVA) {
                        return testPos;
                    }
                }
            }
            return null;
        }
        
        return pos;
    }
    
    private boolean isValidSwarmLocation(BlockPos pos, List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            double distance = player.getBlockPos().getSquaredDistance(pos);
            if (distance < swarmConfig.min_distance * swarmConfig.min_distance) {
                return false;
            }
        }
        return true;
    }
    
    private void updateSwarmEntities() {
        if (!SwarmStatistics.hasActiveSwarm()) return;
        
        ServerWorld overworld = server.getOverworld();
        int previousCount = swarmEntities.size();
        
        swarmEntities.removeIf(uuid -> {
            com.cobblemon.mod.common.entity.pokemon.PokemonEntity entity = 
                (com.cobblemon.mod.common.entity.pokemon.PokemonEntity) overworld.getEntity(uuid);
            return entity == null || !entity.isAlive() || entity.isRemoved();
        });
        
        // Update statistics with current alive count
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        if (current != null) {
            SwarmStatistics.updateCurrentSwarm(swarmEntities.size(), current.entitiesCaptured);
        }
        
        // Don't auto-end swarm - let admin decide when to end it
        
        // Only sync if count changed to reduce network spam
        if (swarmEntities.size() != previousCount) {
            syncStatusWithClients();
            // Reduced logging
        }
    }

    private void announceSwarmStart(String pokemonName, String biomeName, BlockPos location) {
        if (!swarmConfig.notifications_enabled) return;

        List<Text> announcement = Arrays.asList(
            Text.literal("").formatted(Formatting.GOLD),
            Text.literal("🌪️ ").formatted(Formatting.YELLOW)
                .append(Text.literal("SWARM ALERT").formatted(Formatting.GOLD))
                .append(Text.literal(" 🌪️").formatted(Formatting.YELLOW)),
            Text.literal("A massive swarm of ").formatted(Formatting.WHITE)
                .append(Text.literal(pokemonName).formatted(Formatting.GOLD))
                .append(Text.literal(" has been spotted!").formatted(Formatting.WHITE)),
            Text.literal("📍 Location: ").formatted(Formatting.AQUA)
                .append(com.zehro_mc.pokenotifier.util.MessageUtils.createLocationText(pokemonName + " Swarm", location, com.zehro_mc.pokenotifier.util.MessageUtils.Colors.SWARM))
                .append(Text.literal(" (" + biomeName + ")").formatted(Formatting.GRAY)),
            Text.literal("⏰ Duration: ").formatted(Formatting.YELLOW)
                .append(Text.literal(swarmConfig.duration_minutes + " minutes").formatted(Formatting.WHITE)),
            Text.literal("✨ Increased spawn rate and shiny odds active!").formatted(Formatting.GREEN),
            Text.literal("").formatted(Formatting.GOLD)
        );

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            for (Text line : announcement) {
                player.sendMessage(line, false);
            }
        }
    }

    private void announceSwarmEnd(String reason, String pokemonName, int capturedCount) {
        if (!swarmConfig.notifications_enabled) return;

        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        int totalSpawned = current != null ? current.totalEntitiesSpawned : 0;

        Text endMessage = switch (reason) {
            case "time" -> Text.literal("🌪️ The ").formatted(Formatting.YELLOW)
                .append(Text.literal(pokemonName).formatted(Formatting.GOLD))
                .append(Text.literal(" swarm has dispersed as time ran out.").formatted(Formatting.YELLOW));
            case "entities" -> {
                if (capturedCount > totalSpawned / 2) {
                    yield Text.literal("🎆 All ").formatted(Formatting.GREEN)
                        .append(Text.literal(pokemonName).formatted(Formatting.GOLD))
                        .append(Text.literal(" from the swarm have been captured! Well done, trainers!").formatted(Formatting.GREEN));
                } else {
                    yield Text.literal("🌪️ The ").formatted(Formatting.YELLOW)
                        .append(Text.literal(pokemonName).formatted(Formatting.GOLD))
                        .append(Text.literal(" swarm has fled, overwhelmed by the trainers' power!").formatted(Formatting.YELLOW));
                }
            }
            case "admin" -> Text.literal("🌪️ The ").formatted(Formatting.YELLOW)
                .append(Text.literal(pokemonName).formatted(Formatting.GOLD))
                .append(Text.literal(" swarm was cancelled by an administrator.").formatted(Formatting.YELLOW));
            default -> Text.literal("🌪️ The ").formatted(Formatting.YELLOW)
                .append(Text.literal(pokemonName).formatted(Formatting.GOLD))
                .append(Text.literal(" swarm has ended.").formatted(Formatting.YELLOW));
        };

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(endMessage, false);
        }
    }

    private boolean isSwarmExpired() {
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        if (current == null) return false;
        
        long currentTime = System.currentTimeMillis();
        long swarmDuration = swarmConfig.duration_minutes * 60 * 1000L;
        
        return (currentTime - current.startTimestamp) >= swarmDuration;
    }

    // Config methods removed - now using SwarmStatistics for state management

    public boolean hasActiveSwarm() { return SwarmStatistics.hasActiveSwarm(); }
    public boolean isSystemEnabled() { 
        // Always get fresh config state
        SwarmConfig currentConfig = SwarmConfig.load();
        return currentConfig.system_enabled; 
    }
    public String getActiveSwarmPokemon() { 
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        return current != null ? current.pokemonName : null;
    }
    public BlockPos getActiveSwarmLocation() { 
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        return current != null ? new BlockPos(current.location.x, current.location.y, current.location.z) : null;
    }
    public String getActiveSwarmBiome() { 
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        return current != null ? current.biome : null;
    }
    public int getRemainingMinutes() {
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        if (current == null) return 0;
        long elapsed = System.currentTimeMillis() - current.startTimestamp;
        long remaining = (swarmConfig.duration_minutes * 60 * 1000L) - elapsed;
        return Math.max(0, (int) (remaining / (60 * 1000)));
    }
    public int getRemainingEntities() { return swarmEntities.size(); }
    public int getTotalSpawnedCount() { 
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        return current != null ? current.totalEntitiesSpawned : 0;
    }
    public int getCapturedCount() { 
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        return current != null ? current.entitiesCaptured : 0;
    }
    
    public void toggleSystem() {
        swarmConfig.system_enabled = !swarmConfig.system_enabled;
        swarmConfig.save();
        PokeNotifier.LOGGER.info("[SwarmManager] System toggled to: {}", swarmConfig.system_enabled);
    }
    
    /**
     * Called when a Pokemon from this swarm is captured
     */
    public void onPokemonCaptured(java.util.UUID pokemonUuid) {
        if (swarmEntities.remove(pokemonUuid)) {
            SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
            if (current != null) {
                int newCapturedCount = current.entitiesCaptured + 1;
                SwarmStatistics.updateCurrentSwarm(swarmEntities.size(), newCapturedCount);
                // Reduced logging
                
                // Don't auto-end swarm - let admin decide when to end it
            }
            syncStatusWithClients();
        }
    }
    
    /**
     * Syncs swarm status with all connected clients
     */
    private void syncStatusWithClients() {
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        String locationStr = current != null ? 
            "X: " + current.location.x + ", Z: " + current.location.z : "";
            
        SwarmStatusPayload payload = new SwarmStatusPayload(
            current != null,
            current != null ? current.pokemonName : "",
            locationStr,
            current != null ? current.biome : "",
            getRemainingMinutes(),
            getRemainingEntities()
        );
        
        // Reduced logging
        
        for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
        }
    }
    
    /**
     * Public method to sync status with a specific player (for join events)
     */
    public void syncStatusWithPlayer(net.minecraft.server.network.ServerPlayerEntity player) {
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        String locationStr = current != null ? 
            "X: " + current.location.x + ", Z: " + current.location.z : "";
            
        SwarmStatusPayload payload = new SwarmStatusPayload(
            current != null,
            current != null ? current.pokemonName : "",
            locationStr,
            current != null ? current.biome : "",
            getRemainingMinutes(),
            getRemainingEntities()
        );
        
        // Reduced logging
        
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }
    
    /**
     * Spawns multiple Pokémon at the swarm location with increased density
     */
    private int spawnSwarmPokemon(BlockPos centerLocation, String pokemonName, boolean forceAllShiny) {
        try {
            net.minecraft.server.world.ServerWorld overworld = server.getOverworld();
            
            // Use configurable spawn count
            int spawnCount = ThreadLocalRandom.current().nextInt(
                swarmConfig.pokemon_count_min, 
                swarmConfig.pokemon_count_max + 1
            );
            
            boolean shinySpawned = false;
            
            for (int i = 0; i < spawnCount; i++) {
                com.cobblemon.mod.common.api.pokemon.PokemonProperties props = 
                    com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse(pokemonName);
                
                // Handle shiny logic - only one shiny per swarm
                boolean shouldBeShiny = !shinySpawned && (i == spawnCount - 1 || 
                    Math.random() < (1.0 / 4096.0) * swarmConfig.shiny_multiplier);
                
                if (shouldBeShiny) {
                    props.setShiny(true);
                    shinySpawned = true;
                }
                
                BlockPos spawnPos = findSafeSpawnLocation(overworld, centerLocation);
                if (spawnPos == null) continue;
                
                com.cobblemon.mod.common.entity.pokemon.PokemonEntity entity = props.createEntity(overworld);
                entity.refreshPositionAndAngles(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
                
                // Mark as swarm spawn
                entity.getPokemon().getPersistentData().putBoolean("pokenotifier_swarm_spawn", true);
                
                overworld.spawnEntity(entity);
                
                // Track this entity
                swarmEntities.add(entity.getUuid());
            }
            
            PokeNotifier.LOGGER.info("[SwarmManager] Spawned {} {} (1 shiny guaranteed)", spawnCount, pokemonName);
            
            return spawnCount;
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to spawn swarm Pokémon", e);
            return 0;
        }
    }
    
    private BlockPos findSafeAdminSpawnLocation(ServerWorld world, BlockPos adminPos) {
        // Force chunk loading around admin position
        world.getChunk(adminPos.getX() >> 4, adminPos.getZ() >> 4);
        
        // Get surface level at admin position
        int surfaceY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, adminPos.getX(), adminPos.getZ());
        BlockPos surfacePos = new BlockPos(adminPos.getX(), surfaceY + 2, adminPos.getZ()); // 2 blocks above surface
        
        // Validate the surface position
        net.minecraft.block.BlockState groundBlock = world.getBlockState(surfacePos.down(2));
        if (groundBlock.isSolidBlock(world, surfacePos.down(2)) && 
            groundBlock.getBlock() != net.minecraft.block.Blocks.LAVA) {
            return surfacePos;
        }
        
        // Search nearby for safe ground
        for (int radius = 1; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    
                    int x = adminPos.getX() + dx;
                    int z = adminPos.getZ() + dz;
                    int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z) + 2;
                    
                    BlockPos testPos = new BlockPos(x, y, z);
                    net.minecraft.block.BlockState testGround = world.getBlockState(testPos.down(2));
                    
                    if (testGround.isSolidBlock(world, testPos.down(2)) && 
                        testGround.getBlock() != net.minecraft.block.Blocks.LAVA &&
                        y > world.getSeaLevel()) {
                        return testPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    private BlockPos findSafeSpawnLocation(net.minecraft.server.world.ServerWorld world, BlockPos center) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * swarmConfig.radius_blocks;
            
            int x = center.getX() + (int) (Math.cos(angle) * distance);
            int z = center.getZ() + (int) (Math.sin(angle) * distance);
            
            // Force chunk loading
            world.getChunk(x >> 4, z >> 4);
            
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z) + 1;
            BlockPos pos = new BlockPos(x, y, z);
            
            // Validate spawn location
            net.minecraft.block.BlockState blockBelow = world.getBlockState(pos.down());
            net.minecraft.block.BlockState blockAt = world.getBlockState(pos);
            
            if (blockBelow.isSolidBlock(world, pos.down()) && 
                blockBelow.getBlock() != net.minecraft.block.Blocks.LAVA &&
                blockAt.isAir() &&
                y > world.getSeaLevel() - 5) {
                return pos;
            }
        }
        
        // Fallback to center location with proper Y adjustment
        int fallbackY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, center.getX(), center.getZ()) + 1;
        return new BlockPos(center.getX(), fallbackY, center.getZ());
    }
    
    private String getBiomeName(ServerWorld world, BlockPos pos) {
        try {
            Biome biome = world.getBiome(pos).value();
            String biomePath = world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BIOME)
                .getId(biome)
                .getPath();
            
            // Convert snake_case to Title Case
            String[] words = biomePath.replace("_", " ").split(" ");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    result.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        result.append(word.substring(1).toLowerCase());
                    }
                    result.append(" ");
                }
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Unknown Biome";
        }
    }
}