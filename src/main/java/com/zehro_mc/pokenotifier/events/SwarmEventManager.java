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
    
    private boolean hasActiveSwarm = false;
    private String activeSwarmPokemon = null;
    private BlockPos activeSwarmLocation = null;
    private String activeSwarmBiome = null;
    private long swarmStartTime = 0;
    private long lastSwarmEndTime = 0;
    private int swarmCheckTimer = 0;

    public SwarmEventManager(MinecraftServer server) {
        this.server = server;
        this.config = ConfigManager.getServerConfig();
        instance = this;
        loadActiveSwarmFromConfig();
    }

    public static SwarmEventManager getInstance() {
        return instance;
    }

    public void tick() {
        if (!config.swarm_system_enabled) return;

        if (hasActiveSwarm && isSwarmExpired()) {
            endCurrentSwarm();
        }

        swarmCheckTimer++;
        if (swarmCheckTimer >= config.swarm_check_interval_minutes * 1200) {
            swarmCheckTimer = 0;
            attemptSwarmGeneration();
        }
    }

    private void attemptSwarmGeneration() {
        if (hasActiveSwarm) return;

        long currentTime = System.currentTimeMillis();
        long timeSinceLastSwarm = currentTime - lastSwarmEndTime;
        long cooldownMs = config.swarm_cooldown_minutes * 60 * 1000L;
        
        if (timeSinceLastSwarm < cooldownMs) return;

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll >= config.swarm_start_chance_percent) return;

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
            String biomeName = "Unknown Biome";

            startSwarm(selectedPokemon, swarmLocation, biomeName);
            
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Error generating swarm", e);
        }
    }

    public void startSwarm(String pokemonName, BlockPos location, String biomeName) {
        hasActiveSwarm = true;
        activeSwarmPokemon = pokemonName;
        activeSwarmLocation = location;
        activeSwarmBiome = biomeName;
        swarmStartTime = System.currentTimeMillis();

        saveActiveSwarmToConfig();
        announceSwarmStart();
        syncStatusWithClients();
        spawnSwarmPokemon(location, pokemonName);

        PokeNotifier.LOGGER.info("Started swarm: {} at {} in {}", pokemonName, location, biomeName);
    }

    public boolean startManualSwarm(String pokemonName) {
        if (hasActiveSwarm) return false;

        BlockPos location = generateRemoteLocation();
        if (location == null) return false;

        ServerWorld overworld = server.getOverworld();
        Biome biome = overworld.getBiome(location).value();
        String biomeName = "Unknown Biome";

        startSwarm(pokemonName, location, biomeName);
        return true;
    }

    public void endCurrentSwarm() {
        if (!hasActiveSwarm) return;

        announceSwarmEnd();

        hasActiveSwarm = false;
        activeSwarmPokemon = null;
        activeSwarmLocation = null;
        activeSwarmBiome = null;
        lastSwarmEndTime = System.currentTimeMillis();

        clearActiveSwarmFromConfig();
        syncStatusWithClients();
        PokeNotifier.LOGGER.info("Ended swarm event");
    }

    private BlockPos generateRemoteLocation() {
        ServerWorld overworld = server.getOverworld();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        
        if (players.isEmpty()) {
            return new BlockPos(
                ThreadLocalRandom.current().nextInt(-10000, 10000),
                64,
                ThreadLocalRandom.current().nextInt(-10000, 10000)
            );
        }

        for (int attempt = 0; attempt < 50; attempt++) {
            ServerPlayerEntity randomPlayer = players.get(ThreadLocalRandom.current().nextInt(players.size()));
            BlockPos playerPos = randomPlayer.getBlockPos();
            
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            int distance = ThreadLocalRandom.current().nextInt(config.swarm_min_distance, config.swarm_max_distance);
            
            int x = playerPos.getX() + (int) (Math.cos(angle) * distance);
            int z = playerPos.getZ() + (int) (Math.sin(angle) * distance);
            
            BlockPos candidate = new BlockPos(x, 64, z);
            
            boolean validLocation = true;
            for (ServerPlayerEntity player : players) {
                if (player.getBlockPos().getSquaredDistance(candidate) < config.swarm_min_distance * config.swarm_min_distance) {
                    validLocation = false;
                    break;
                }
            }
            
            if (validLocation) {
                int y = overworld.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, candidate.getX(), candidate.getZ());
                return new BlockPos(candidate.getX(), y, candidate.getZ());
            }
        }
        
        return null;
    }

    private void announceSwarmStart() {
        if (!config.swarm_notifications_enabled) return;

        List<Text> announcement = Arrays.asList(
            Text.literal("").formatted(Formatting.GOLD),
            Text.literal("🌪️ ").formatted(Formatting.YELLOW)
                .append(Text.literal("SWARM ALERT").formatted(Formatting.BOLD, Formatting.GOLD))
                .append(Text.literal(" 🌪️").formatted(Formatting.YELLOW)),
            Text.literal("A massive swarm of ").formatted(Formatting.WHITE)
                .append(Text.literal(activeSwarmPokemon).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" has been spotted!").formatted(Formatting.WHITE)),
            Text.literal("📍 Location: ").formatted(Formatting.AQUA)
                .append(com.zehro_mc.pokenotifier.util.MessageUtils.createLocationText(activeSwarmPokemon + " Swarm", activeSwarmLocation, com.zehro_mc.pokenotifier.util.MessageUtils.Colors.SWARM))
                .append(Text.literal(" (" + activeSwarmBiome + ")").formatted(Formatting.GRAY)),
            Text.literal("⏰ Duration: ").formatted(Formatting.YELLOW)
                .append(Text.literal(config.swarm_duration_minutes + " minutes").formatted(Formatting.WHITE)),
            Text.literal("✨ Increased spawn rate and shiny odds active!").formatted(Formatting.GREEN),
            Text.literal("").formatted(Formatting.GOLD)
        );

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            for (Text line : announcement) {
                player.sendMessage(line, false);
            }
        }
    }

    private void announceSwarmEnd() {
        if (!config.swarm_notifications_enabled) return;

        Text endMessage = Text.literal("🌪️ The ").formatted(Formatting.YELLOW)
            .append(Text.literal(activeSwarmPokemon).formatted(Formatting.GOLD))
            .append(Text.literal(" swarm has dispersed.").formatted(Formatting.YELLOW));

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(endMessage, false);
        }
    }

    private boolean isSwarmExpired() {
        if (!hasActiveSwarm) return false;
        
        long currentTime = System.currentTimeMillis();
        long swarmDuration = config.swarm_duration_minutes * 60 * 1000L;
        
        return (currentTime - swarmStartTime) >= swarmDuration;
    }

    private void saveActiveSwarmToConfig() {
        config.active_swarm_pokemon = activeSwarmPokemon;
        config.active_swarm_location = activeSwarmLocation != null ? 
            activeSwarmLocation.getX() + "," + activeSwarmLocation.getY() + "," + activeSwarmLocation.getZ() : null;
        config.active_swarm_biome = activeSwarmBiome;
        ConfigManager.saveServerConfigToFile();
    }

    private void loadActiveSwarmFromConfig() {
        if (config.active_swarm_pokemon != null && config.active_swarm_location != null) {
            try {
                String[] coords = config.active_swarm_location.split(",");
                activeSwarmPokemon = config.active_swarm_pokemon;
                activeSwarmLocation = new BlockPos(
                    Integer.parseInt(coords[0]),
                    Integer.parseInt(coords[1]),
                    Integer.parseInt(coords[2])
                );
                activeSwarmBiome = config.active_swarm_biome;
                hasActiveSwarm = true;
                swarmStartTime = System.currentTimeMillis();
                
                PokeNotifier.LOGGER.info("Loaded active swarm: {} at {}", activeSwarmPokemon, activeSwarmLocation);
            } catch (Exception e) {
                PokeNotifier.LOGGER.warn("Failed to load active swarm from config", e);
                clearActiveSwarmFromConfig();
            }
        }
    }

    private void clearActiveSwarmFromConfig() {
        config.active_swarm_pokemon = null;
        config.active_swarm_location = null;
        config.active_swarm_biome = null;
        ConfigManager.saveServerConfigToFile();
    }

    public boolean hasActiveSwarm() { return hasActiveSwarm; }
    public String getActiveSwarmPokemon() { return activeSwarmPokemon; }
    public BlockPos getActiveSwarmLocation() { return activeSwarmLocation; }
    public String getActiveSwarmBiome() { return activeSwarmBiome; }
    public int getRemainingMinutes() {
        if (!hasActiveSwarm) return 0;
        long elapsed = System.currentTimeMillis() - swarmStartTime;
        long remaining = (config.swarm_duration_minutes * 60 * 1000L) - elapsed;
        return Math.max(0, (int) (remaining / (60 * 1000)));
    }
    
    /**
     * Syncs swarm status with all connected clients
     */
    private void syncStatusWithClients() {
        String locationStr = activeSwarmLocation != null ? 
            "X: " + activeSwarmLocation.getX() + ", Z: " + activeSwarmLocation.getZ() : "";
            
        SwarmStatusPayload payload = new SwarmStatusPayload(
            hasActiveSwarm,
            activeSwarmPokemon != null ? activeSwarmPokemon : "",
            locationStr,
            activeSwarmBiome != null ? activeSwarmBiome : "",
            getRemainingMinutes()
        );
        
        for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
        }
    }
    
    /**
     * Spawns multiple Pokémon at the swarm location with increased density
     */
    private void spawnSwarmPokemon(BlockPos centerLocation, String pokemonName) {
        try {
            net.minecraft.server.world.ServerWorld overworld = server.getOverworld();
            com.cobblemon.mod.common.api.pokemon.PokemonProperties props = 
                com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse(pokemonName);
            
            // Spawn multiple Pokémon in the area
            int spawnCount = config.swarm_spawn_multiplier + java.util.concurrent.ThreadLocalRandom.current().nextInt(5);
            
            for (int i = 0; i < spawnCount; i++) {
                // Random position within swarm radius
                double angle = Math.random() * 2 * Math.PI;
                double distance = Math.random() * config.swarm_radius_blocks;
                
                int x = centerLocation.getX() + (int) (Math.cos(angle) * distance);
                int z = centerLocation.getZ() + (int) (Math.sin(angle) * distance);
                int y = overworld.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z);
                
                // Apply shiny multiplier
                if (Math.random() < (1.0 / 4096.0) * config.swarm_shiny_multiplier) {
                    props.setShiny(true);
                }
                
                com.cobblemon.mod.common.entity.pokemon.PokemonEntity entity = props.createEntity(overworld);
                entity.refreshPositionAndAngles(x, y, z, 0, 0);
                
                // Mark as swarm spawn
                entity.getPokemon().getPersistentData().putBoolean("pokenotifier_swarm_spawn", true);
                
                overworld.spawnEntity(entity);
            }
            
            PokeNotifier.LOGGER.info("Spawned {} {} for swarm at {}", spawnCount, pokemonName, centerLocation);
            
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to spawn swarm Pokémon", e);
        }
    }
}