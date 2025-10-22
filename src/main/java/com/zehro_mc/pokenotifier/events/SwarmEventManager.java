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
    
    // Lazy loading system
    private boolean entitiesSpawned = false;
    private static final int PLAYER_DETECTION_RADIUS = 64; // blocks

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
            // Check for nearby players and spawn if needed (every 3 seconds)
            if (swarmCheckTimer % 60 == 0) {
                checkForNearbyPlayersAndSpawn();
            }
            
            // Validate entities every 5 seconds (100 ticks)
            if (swarmCheckTimer % 100 == 0) {
                validateAndSyncEntities();
            }
            
            if (isSwarmExpired()) {
                endCurrentSwarm("time");
            }
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
        startSwarmWithShiny(pokemonName, location, biomeName, triggeredBy, false);
    }
    
    public void startSwarmWithShiny(String pokemonName, BlockPos location, String biomeName, String triggeredBy, boolean forceShiny) {
        swarmEntities.clear();
        entitiesSpawned = false;

        announceSwarmStart(pokemonName, biomeName, location);
        
        // Record statistics with pending spawn count
        String swarmType = biomeName.equals("Admin Location") ? "MANUAL" : "AUTOMATIC";
        int pendingCount = ThreadLocalRandom.current().nextInt(swarmConfig.pokemon_count_min, swarmConfig.pokemon_count_max + 1);
        SwarmStatistics.recordSwarmStart(pokemonName, swarmType, triggeredBy, location, biomeName, pendingCount);
        
        // Start monitoring for nearby players
        syncStatusWithClients();

        PokeNotifier.LOGGER.info("[SwarmManager] Started {} swarm: {} by {} ({} entities pending)", 
            biomeName.equals("Admin Location") ? "manual" : "automatic", pokemonName, triggeredBy, pendingCount);
    }

    public boolean startManualSwarm(String pokemonName) {
        return startManualSwarm(pokemonName, "ADMIN", false);
    }
    
    public boolean startManualSwarm(String pokemonName, String adminName) {
        return startManualSwarm(pokemonName, adminName, false);
    }
    
    public boolean startManualSwarm(String pokemonName, String adminName, boolean forceShiny) {
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

        startSwarmWithShiny(pokemonName, location, biomeName, adminName, forceShiny);
        return true;
    }
    
    public boolean startManualSwarmAt(String pokemonName, BlockPos playerPos, String adminName) {
        return startManualSwarmAt(pokemonName, playerPos, adminName, false);
    }
    
    public boolean startManualSwarmAt(String pokemonName, BlockPos playerPos, String adminName, boolean forceShiny) {
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

        startSwarmWithShiny(pokemonName, location, biomeName, adminName, forceShiny);
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

        // Clean up remaining swarm entities if they were spawned
        if (entitiesSpawned) {
            cleanupSwarmEntities();
        }

        // Record statistics before clearing data
        long duration = System.currentTimeMillis() - current.startTimestamp;
        int durationMinutes = (int) (duration / (60 * 1000));
        SwarmStatistics.recordSwarmEnd(current.pokemonName, reason.toUpperCase(), current.entitiesCaptured, durationMinutes);

        announceSwarmEnd(reason, current.pokemonName, current.entitiesCaptured);

        lastSwarmEndTime = System.currentTimeMillis();
        swarmEntities.clear();
        entitiesSpawned = false;

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
        
        // Debug logging for entity tracking
        if (ConfigManager.getServerConfig().debug_mode_enabled && previousCount > 0) {
            PokeNotifier.LOGGER.debug("[SwarmManager] Checking {} tracked entities", previousCount);
        }
        
        swarmEntities.removeIf(uuid -> {
            com.cobblemon.mod.common.entity.pokemon.PokemonEntity entity = 
                (com.cobblemon.mod.common.entity.pokemon.PokemonEntity) overworld.getEntity(uuid);
            boolean shouldRemove = entity == null || !entity.isAlive() || entity.isRemoved();
            
            if (shouldRemove && ConfigManager.getServerConfig().debug_mode_enabled) {
                PokeNotifier.LOGGER.debug("[SwarmManager] Removing entity {} - entity null: {}, alive: {}, removed: {}", 
                    uuid, entity == null, entity != null ? entity.isAlive() : "N/A", entity != null ? entity.isRemoved() : "N/A");
            }
            
            return shouldRemove;
        });
        
        // Update statistics with current alive count
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        if (current != null) {
            SwarmStatistics.updateCurrentSwarm(swarmEntities.size(), current.entitiesCaptured);
        }
        
        // Only sync if count changed to reduce network spam
        if (swarmEntities.size() != previousCount) {
            syncStatusWithClients();
            if (ConfigManager.getServerConfig().debug_mode_enabled) {
                PokeNotifier.LOGGER.debug("[SwarmManager] Entity count changed: {} -> {}", previousCount, swarmEntities.size());
            }
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
    public int getRemainingEntities() { 
        if (!entitiesSpawned) {
            SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
            return current != null ? current.totalEntitiesSpawned : 0;
        }
        return swarmEntities.size();
    }
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
                
                if (ConfigManager.getServerConfig().debug_mode_enabled) {
                    PokeNotifier.LOGGER.debug("[SwarmManager] Captured swarm entity, {} remaining", swarmEntities.size());
                }
                
                // Check if swarm should end naturally
                if (swarmEntities.isEmpty()) {
                    server.execute(() -> endCurrentSwarm("entities"));
                }
            }
            syncStatusWithClients();
        }
    }
    
    private void checkForNearbyPlayersAndSpawn() {
        if (entitiesSpawned) return; // Already spawned
        
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        if (current == null) return;
        
        BlockPos eventLocation = new BlockPos(current.location.x, current.location.y, current.location.z);
        
        // Check if any player is near the event location
        boolean playerNearby = false;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            double distance = player.getPos().distanceTo(eventLocation.toCenterPos());
            if (distance <= PLAYER_DETECTION_RADIUS) {
                playerNearby = true;
                PokeNotifier.LOGGER.info("[SwarmManager] Player {} detected near swarm - spawning entities", 
                    player.getName().getString());
                break;
            }
        }

        if (playerNearby) {
            int spawnedCount = spawnSwarmPokemon(eventLocation, current.pokemonName, false);
            entitiesSpawned = true;
            
            // Update statistics with actual spawned count
            SwarmStatistics.updateCurrentSwarm(swarmEntities.size(), current.entitiesCaptured);
            
            PokeNotifier.LOGGER.info("[SwarmManager] Spawned {} entities due to player proximity", spawnedCount);
        }
    }

    /**
     * Validates tracked entities and syncs with clients
     */
    private void validateAndSyncEntities() {
        if (!SwarmStatistics.hasActiveSwarm()) {
            return;
        }
        
        if (!entitiesSpawned) {
            // Entities not spawned yet - just sync status
            syncStatusWithClients();
            return;
        }
        
        ServerWorld overworld = server.getOverworld();
        int previousCount = swarmEntities.size();
        
        // Validate all tracked entities exist and are alive
        Set<java.util.UUID> validUUIDs = new HashSet<>();
        for (java.util.UUID uuid : swarmEntities) {
            com.cobblemon.mod.common.entity.pokemon.PokemonEntity entity = 
                (com.cobblemon.mod.common.entity.pokemon.PokemonEntity) overworld.getEntity(uuid);
            
            if (entity != null && entity.isAlive() && !entity.isRemoved()) {
                validUUIDs.add(uuid);
            }
        }
        
        // Update tracked set with only valid entities
        swarmEntities.clear();
        swarmEntities.addAll(validUUIDs);
        
        // Check if swarm should end due to no entities remaining
        if (previousCount > 0 && swarmEntities.isEmpty()) {
            PokeNotifier.LOGGER.info("[SwarmManager] All swarm entities gone - ending event");
            endCurrentSwarm("entities");
            return;
        }
        
        syncStatusWithClients();
    }
    
    /**
     * Syncs swarm status with all connected clients
     */
    private void syncStatusWithClients() {
        SwarmStatistics.CurrentSwarm current = SwarmStatistics.getCurrentSwarm();
        String locationStr = current != null ? 
            "X: " + current.location.x + ", Z: " + current.location.z : "";
        
        int currentEntityCount;
        if (!entitiesSpawned) {
            // Show total expected count while waiting for players
            currentEntityCount = current != null ? current.totalEntitiesSpawned : 0;
        } else {
            // Show actual tracked count after spawning
            currentEntityCount = swarmEntities.size();
        }
        
        SwarmStatusPayload payload = new SwarmStatusPayload(
            current != null,
            current != null ? current.pokemonName : "",
            locationStr,
            current != null ? current.biome : "",
            getRemainingMinutes(),
            currentEntityCount
        );
        

        
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
            
            // Clear previous entities before spawning new ones
            swarmEntities.clear();
            
            // Use configurable spawn count
            int spawnCount = ThreadLocalRandom.current().nextInt(
                swarmConfig.pokemon_count_min, 
                swarmConfig.pokemon_count_max + 1
            );
            
            boolean shinySpawned = false;
            int actualSpawned = 0;
            
            for (int i = 0; i < spawnCount; i++) {
                com.cobblemon.mod.common.api.pokemon.PokemonProperties props = 
                    com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse(pokemonName);
                
                // Handle shiny logic
                boolean shouldBeShiny;
                if (forceAllShiny) {
                    shouldBeShiny = true; // Force all to be shiny
                } else {
                    // Normal logic - only one shiny per swarm
                    shouldBeShiny = !shinySpawned && (i == spawnCount - 1 || 
                        Math.random() < (1.0 / 4096.0) * swarmConfig.shiny_multiplier);
                }
                
                if (shouldBeShiny) {
                    props.setShiny(true);
                    if (!forceAllShiny) {
                        shinySpawned = true; // Only set flag if not forcing all shiny
                    }
                }
                
                BlockPos spawnPos = findSafeSpawnLocation(overworld, centerLocation);
                if (spawnPos == null) continue;
                
                com.cobblemon.mod.common.entity.pokemon.PokemonEntity entity = props.createEntity(overworld);
                entity.refreshPositionAndAngles(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
                
                // Set randomized level based on server player average
                int randomLevel = calculateRandomLevel();
                entity.getPokemon().setLevel(randomLevel);
                
                // Mark as swarm spawn
                entity.getPokemon().getPersistentData().putBoolean("pokenotifier_swarm_spawn", true);
                
                // Spawn entity first, then track it
                if (overworld.spawnEntity(entity)) {
                    // Ensure entity is fully loaded before tracking
                    java.util.UUID entityUUID = entity.getUuid();
                    swarmEntities.add(entityUUID);
                    actualSpawned++;
                    
                    if (ConfigManager.getServerConfig().debug_mode_enabled) {
                        PokeNotifier.LOGGER.debug("[SwarmManager] Spawned entity: {} (Level: {})", 
                            pokemonName, entity.getPokemon().getLevel());
                    }
                } else {
                    PokeNotifier.LOGGER.warn("[SwarmManager] FAILED to spawn entity for {}", pokemonName);
                }
            }
            
            PokeNotifier.LOGGER.info("[SwarmManager] Spawned {} {} entities, tracking {} entities", 
                actualSpawned, pokemonName, swarmEntities.size());
            
            return actualSpawned;
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
    
    /**
     * Clean up remaining swarm entities when the event ends
     */
    private void cleanupSwarmEntities() {
        if (swarmEntities.isEmpty()) {
            PokeNotifier.LOGGER.debug("[SwarmManager] No entities to cleanup");
            return;
        }
        
        ServerWorld overworld = server.getOverworld();
        int removedCount = 0;
        
        for (java.util.UUID uuid : new HashSet<>(swarmEntities)) {
            try {
                com.cobblemon.mod.common.entity.pokemon.PokemonEntity entity = 
                    (com.cobblemon.mod.common.entity.pokemon.PokemonEntity) overworld.getEntity(uuid);
                
                if (entity != null && entity.isAlive() && !entity.isRemoved()) {
                    // Check if this is actually a swarm Pokemon
                    if (entity.getPokemon().getPersistentData().getBoolean("pokenotifier_swarm_spawn")) {
                        // Remove the entity with a nice particle effect
                        entity.discard();
                        removedCount++;
                        
                        if (ConfigManager.getServerConfig().debug_mode_enabled) {
                            PokeNotifier.LOGGER.debug("[SwarmManager] Removed swarm entity: {} (UUID: {})", 
                                entity.getPokemon().getDisplayName().getString(), uuid);
                        }
                    }
                }
            } catch (Exception e) {
                PokeNotifier.LOGGER.warn("[SwarmManager] Error removing entity {}: {}", uuid, e.getMessage());
            }
        }
        
        if (removedCount > 0) {
            PokeNotifier.LOGGER.info("[SwarmManager] Cleaned up {} remaining swarm entities", removedCount);
            
            // Notify players about the cleanup
            Text cleanupMessage = Text.literal("🌪️ The remaining swarm Pokémon have dispersed into the wild...").formatted(Formatting.GRAY);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(cleanupMessage, false);
            }
        }
    }
    
    /**
     * Calculate a random level for spawned Pokemon based on server player average
     */
    private int calculateRandomLevel() {
        try {
            List<net.minecraft.server.network.ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
            if (players.isEmpty()) {
                return ThreadLocalRandom.current().nextInt(15, 35); // Default range if no players
            }
            
            // Calculate average level of all players' Pokemon
            int totalLevels = 0;
            int pokemonCount = 0;
            
            for (net.minecraft.server.network.ServerPlayerEntity player : players) {
                try {
                    com.cobblemon.mod.common.api.storage.PokemonStore party = 
                        com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
                    
                    // Use iterator to access Pokemon in party
                    java.util.Iterator<com.cobblemon.mod.common.pokemon.Pokemon> iterator = party.iterator();
                    while (iterator.hasNext()) {
                        com.cobblemon.mod.common.pokemon.Pokemon pokemon = iterator.next();
                        if (pokemon != null) {
                            totalLevels += pokemon.getLevel();
                            pokemonCount++;
                        }
                    }
                } catch (Exception e) {
                    // Skip this player if there's an error accessing their party
                    continue;
                }
            }
            
            int averageLevel;
            if (pokemonCount > 0) {
                averageLevel = totalLevels / pokemonCount;
            } else {
                averageLevel = 25; // Default if no Pokemon found
            }
            
            // Create a range around the average level (±10 levels)
            int minLevel = Math.max(5, averageLevel - 10);
            int maxLevel = Math.min(80, averageLevel + 10);
            
            int randomLevel = ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
            
            if (ConfigManager.getServerConfig().debug_mode_enabled) {
                PokeNotifier.LOGGER.debug("[SwarmManager] Level calculation - Players: {}, Pokemon: {}, Avg: {}, Range: {}-{}, Selected: {}", 
                    players.size(), pokemonCount, averageLevel, minLevel, maxLevel, randomLevel);
            }
            
            return randomLevel;
            
        } catch (Exception e) {
            PokeNotifier.LOGGER.warn("[SwarmManager] Error calculating random level, using default: {}", e.getMessage());
            return ThreadLocalRandom.current().nextInt(20, 40); // Fallback range
        }
    }
}