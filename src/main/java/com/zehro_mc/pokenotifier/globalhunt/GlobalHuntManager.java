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
        if (server == null || !config.isEnabled()) return;
        
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
    
    public void startManualEvent(ServerWorld world, BlockPos coordinates, String pokemon, boolean isShiny) {
        if (currentEvent != null && currentEvent.isActive()) {
            PokeNotifier.LOGGER.warn("Cannot start manual Global Hunt while one is already active");
            return;
        }
        
        startEvent(world, coordinates, pokemon, isShiny, true);
    }
    
    private void startEvent(ServerWorld world, BlockPos coordinates, String pokemon, boolean isShiny, boolean isManual) {
        currentEvent = new GlobalHuntEvent(world, coordinates, pokemon, isShiny, config.getEventDurationMinutes());
        
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
        int maxDistance = 800; // Reasonable distance from players
        
        for (int attempts = 0; attempts < 15; attempts++) {
            int x = (int) referencePlayer.getX() + random.nextInt(maxDistance * 2) - maxDistance;
            int z = (int) referencePlayer.getZ() + random.nextInt(maxDistance * 2) - maxDistance;
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
            
            BlockPos pos = new BlockPos(x, y, z);
            
            if (y >= world.getBottomY() + 10 && y <= world.getTopY() - 10) {
                return pos;
            }
        }
        
        return generateFallbackCoordinates(world);
    }
    
    private BlockPos generateFallbackCoordinates(ServerWorld world) {
        // Fallback: spawn near world spawn
        BlockPos spawn = world.getSpawnPos();
        int x = spawn.getX() + random.nextInt(400) - 200;
        int z = spawn.getZ() + random.nextInt(400) - 200;
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        return new BlockPos(x, y, z);
    }
    
    private boolean isValidSpawnLocation(ServerWorld world, BlockPos pos) {
        // Check if chunk is loaded
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        
        // Check if it's not in void or too high
        if (pos.getY() < world.getBottomY() + 5 || pos.getY() > world.getTopY() - 5) {
            return false;
        }
        
        // Additional safety checks can be added here
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
}