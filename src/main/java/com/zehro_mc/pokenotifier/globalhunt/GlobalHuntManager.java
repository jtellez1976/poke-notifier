/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.globalhunt;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.EventsConfig;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.world.Heightmap;

public class GlobalHuntManager {
    private static GlobalHuntManager instance;
    private MinecraftServer server;
    private GlobalHuntEvent currentEvent;
    private ScheduledExecutorService scheduler;
    private GlobalHuntConfig config;
    private Random random = new Random();
    
    private GlobalHuntManager() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.config = new GlobalHuntConfig();
    }
    
    public static GlobalHuntManager getInstance() {
        if (instance == null) {
            instance = new GlobalHuntManager();
        }
        return instance;
    }
    
    public void initialize(MinecraftServer server) {
        this.server = server;
        PokeNotifier.LOGGER.info("Global Hunt Manager initialized for server: " + 
            (server.isDedicated() ? "Dedicated" : "Integrated"));
        
        // Start automatic scheduling
        scheduleNextEvent();
    }
    
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        if (currentEvent != null) {
            currentEvent.cancel();
        }
    }
    
    private void scheduleNextEvent() {
        EventsConfig eventsConfig = ConfigManager.getEventsConfig();
        if (server == null || !eventsConfig.globalHuntEnabled) return;
        
        // Calculate random delay between min and max interval
        int minHours = config.getMinIntervalHours();
        int maxHours = config.getMaxIntervalHours();
        int delayHours = minHours + random.nextInt(maxHours - minHours + 1);
        
        PokeNotifier.LOGGER.info("Next Global Hunt scheduled in {} hours", delayHours);
        
        scheduler.schedule(() -> {
            if (server != null && !server.isStopped()) {
                server.execute(this::startRandomEvent);
            }
        }, delayHours, TimeUnit.HOURS);
    }
    
    public void startRandomEvent() {
        if (currentEvent != null && currentEvent.isActive()) {
            PokeNotifier.LOGGER.warn("Attempted to start Global Hunt while one is already active");
            return;
        }
        
        // Select random world
        ServerWorld world = selectRandomWorld();
        if (world == null) {
            PokeNotifier.LOGGER.error("No valid world found for Global Hunt");
            scheduleNextEvent();
            return;
        }
        
        // Generate random coordinates near active players
        BlockPos coordinates = generateRandomCoordinatesNearPlayers(world);
        if (coordinates == null) {
            PokeNotifier.LOGGER.error("Failed to generate valid coordinates for Global Hunt");
            scheduleNextEvent();
            return;
        }
        
        // Select random Pokemon
        String pokemon = config.getRandomPokemon();
        boolean isShiny = random.nextDouble() < config.getShinyChance();
        
        startEvent(world, coordinates, pokemon, isShiny, false);
    }
    
    public void startManualEvent(ServerWorld world, String pokemon, boolean isShiny) {
        if (currentEvent != null && currentEvent.isActive()) {
            PokeNotifier.LOGGER.warn("Cannot start manual Global Hunt while one is already active");
            return;
        }
        
        // Generate challenging coordinates for manual events too
        BlockPos coordinates = generateRandomCoordinatesNearPlayers(world);
        if (coordinates == null) {
            PokeNotifier.LOGGER.error("Failed to generate coordinates for manual Global Hunt");
            return;
        }
        
        startEvent(world, coordinates, pokemon, isShiny, true);
    }
    
    public void startManualEventAtLocation(ServerWorld world, BlockPos coordinates, String pokemon, boolean isShiny) {
        if (currentEvent != null && currentEvent.isActive()) {
            PokeNotifier.LOGGER.warn("Cannot start manual Global Hunt while one is already active");
            return;
        }
        
        startEvent(world, coordinates, pokemon, isShiny, true);
    }
    
    private void startEvent(ServerWorld world, BlockPos coordinates, String pokemon, boolean isShiny, boolean isManual) {
        EventsConfig eventsConfig = ConfigManager.getEventsConfig();
        currentEvent = new GlobalHuntEvent(world, coordinates, pokemon, isShiny, eventsConfig.globalHuntDurationMinutes);
        
        // Update statistics
        eventsConfig.totalGlobalHuntEvents++;
        eventsConfig.lastGlobalHuntEventTime = System.currentTimeMillis();
        eventsConfig.lastGlobalHuntPokemon = (isShiny ? "Shiny " : "") + pokemon;
        ConfigManager.saveConfig();
        
        PokeNotifier.LOGGER.info("Starting Global Hunt: {} {} at {} in {}", 
            isShiny ? "Shiny" : "", pokemon, coordinates, world.getRegistryKey().getValue());
        
        currentEvent.start();
        
        // Schedule next automatic event only if this wasn't manual
        if (!isManual) {
            scheduleNextEvent();
        }
    }
    
    private ServerWorld selectRandomWorld() {
        var worlds = server.getWorlds();
        var validWorlds = new ArrayList<ServerWorld>();
        for (ServerWorld world : worlds) {
            if (config.isWorldEnabled(world.getRegistryKey().getValue())) {
                validWorlds.add(world);
            }
        }
        
        if (validWorlds.isEmpty()) return null;
        
        return validWorlds.get(random.nextInt(validWorlds.size()));
    }
    
    private BlockPos generateRandomCoordinatesNearPlayers(ServerWorld world) {
        // Get active players in this world
        var playersInWorld = new ArrayList<net.minecraft.server.network.ServerPlayerEntity>();
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (player.getServerWorld() == world) {
                playersInWorld.add(player);
            }
        }
            
        if (playersInWorld.isEmpty()) {
            return generateFallbackCoordinates(world);
        }
        
        // Pick a random player as reference
        var referencePlayer = playersInWorld.get(random.nextInt(playersInWorld.size()));
        
        // Use distances from EventsConfig
        EventsConfig eventsConfig = ConfigManager.getEventsConfig();
        int minDistance = eventsConfig.globalHuntMinDistance;
        int maxDistance = eventsConfig.globalHuntMaxDistance;
        
        for (int attempts = 0; attempts < 20; attempts++) {
            // Generate random angle
            double angle = random.nextDouble() * 2 * Math.PI;
            
            // Generate random distance within range
            int distance = minDistance + random.nextInt(maxDistance - minDistance);
            
            // Calculate coordinates
            int x = (int) (referencePlayer.getX() + Math.cos(angle) * distance);
            int z = (int) (referencePlayer.getZ() + Math.sin(angle) * distance);
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
            
            BlockPos pos = new BlockPos(x, y, z);
            
            // Verify it's a valid spawn location
            if (isValidSpawnLocation(world, pos)) {
                PokeNotifier.LOGGER.info("Generated Global Hunt coordinates {} blocks from player {}", 
                    (int) Math.sqrt(Math.pow(x - referencePlayer.getX(), 2) + Math.pow(z - referencePlayer.getZ(), 2)),
                    referencePlayer.getName().getString());
                return pos;
            }
        }
        
        PokeNotifier.LOGGER.warn("Failed to generate challenging coordinates, using fallback");
        return generateFallbackCoordinates(world);
    }
    
    private BlockPos generateFallbackCoordinates(ServerWorld world) {
        // Fallback: spawn at challenging distance from world spawn
        BlockPos spawn = world.getSpawnPos();
        
        // Even fallback should be challenging (2000-3000 blocks from spawn)
        double angle = random.nextDouble() * 2 * Math.PI;
        int distance = 2000 + random.nextInt(1000);
        
        int x = (int) (spawn.getX() + Math.cos(angle) * distance);
        int z = (int) (spawn.getZ() + Math.sin(angle) * distance);
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        
        PokeNotifier.LOGGER.info("Using fallback coordinates {} blocks from world spawn", distance);
        return new BlockPos(x, y, z);
    }
    
    private boolean isValidSpawnLocation(ServerWorld world, BlockPos pos) {
        // Check if it's not in void or too high
        if (pos.getY() < world.getBottomY() + 10 || pos.getY() > world.getTopY() - 10) {
            return false;
        }
        
        // Check if coordinates are within world border
        if (Math.abs(pos.getX()) > 29999900 || Math.abs(pos.getZ()) > 29999900) {
            return false;
        }
        
        // For Nether, ensure it's not too close to bedrock ceiling
        if (world.getRegistryKey() == World.NETHER) {
            if (pos.getY() > 120 || pos.getY() < 10) {
                return false;
            }
        }
        
        return true;
    }
    
    public GlobalHuntEvent getCurrentEvent() {
        return currentEvent;
    }
    
    public boolean hasActiveEvent() {
        return currentEvent != null && currentEvent.isActive();
    }
    
    public GlobalHuntConfig getConfig() {
        return config;
    }
    
    public void cancelCurrentEvent() {
        if (currentEvent != null && currentEvent.isActive()) {
            currentEvent.cancel();
            PokeNotifier.LOGGER.info("Global Hunt event cancelled by admin");
        }
    }
    
    public String getEventStatus() {
        EventsConfig eventsConfig = ConfigManager.getEventsConfig();
        StringBuilder status = new StringBuilder();
        status.append("Global Hunt System: ").append(eventsConfig.globalHuntEnabled ? "ENABLED" : "DISABLED").append("\n");
        
        if (currentEvent != null && currentEvent.isActive()) {
            status.append("Active Event: ").append(currentEvent.isShiny() ? "Shiny " : "").append(currentEvent.getPokemonName()).append("\n");
            status.append("Location: ").append(currentEvent.getCoordinates()).append("\n");
            status.append("World: ").append(currentEvent.getWorld().getRegistryKey().getValue()).append("\n");
        } else {
            status.append("No active event\n");
        }
        
        status.append("\nStatistics:\n");
        status.append("Total Events: ").append(eventsConfig.totalGlobalHuntEvents).append("\n");
        status.append("Successful Events: ").append(eventsConfig.successfulGlobalHuntEvents).append("\n");
        
        if (eventsConfig.totalGlobalHuntEvents > 0) {
            int successRate = (eventsConfig.successfulGlobalHuntEvents * 100) / eventsConfig.totalGlobalHuntEvents;
            status.append("Success Rate: ").append(successRate).append("%\n");
        }
        
        if (!eventsConfig.lastGlobalHuntWinner.isEmpty()) {
            status.append("Last Winner: ").append(eventsConfig.lastGlobalHuntWinner).append("\n");
        }
        
        return status.toString();
    }
    
    public void setSystemEnabled(boolean enabled) {
        EventsConfig eventsConfig = ConfigManager.getEventsConfig();
        eventsConfig.globalHuntEnabled = enabled;
        ConfigManager.saveConfig();
        PokeNotifier.LOGGER.info("Global Hunt system " + (enabled ? "enabled" : "disabled"));
        
        if (enabled) {
            scheduleNextEvent();
        } else {
            cancelCurrentEvent();
        }
    }
    
    public boolean isSystemEnabled() {
        return ConfigManager.getEventsConfig().globalHuntEnabled;
    }
    
    public EventsConfig getEventsConfig() {
        return ConfigManager.getEventsConfig();
    }
    
    public void onEventCompleted(boolean successful) {
        EventsConfig eventsConfig = ConfigManager.getEventsConfig();
        if (successful) {
            eventsConfig.successfulGlobalHuntEvents++;
            ConfigManager.saveConfig();
            PokeNotifier.LOGGER.info("Global Hunt completed successfully!");
        } else {
            PokeNotifier.LOGGER.info("Global Hunt timed out or was cancelled");
        }
        
        currentEvent = null;
        
        if (eventsConfig.globalHuntEnabled) {
            scheduleNextEvent();
        }
    }
    
    public void onEventWon(String playerName) {
        EventsConfig eventsConfig = ConfigManager.getEventsConfig();
        eventsConfig.lastGlobalHuntWinner = playerName;
        ConfigManager.saveConfig();
    }
}