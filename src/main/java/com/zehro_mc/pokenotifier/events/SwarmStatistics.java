/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SwarmStatistics {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls() // Force null values to appear in JSON
        .create();
    private static final File EVENTS_DIR = FabricLoader.getInstance().getConfigDir().resolve(PokeNotifier.MOD_ID).resolve("events").toFile();
    private static final String STATS_FILE = "swarm_statistics.json";
    private static SwarmData swarmData = new SwarmData();
    
    public static class SwarmData {
        public CurrentSwarm current;
        public List<SwarmRecord> statistics;
        
        public SwarmData() {
            this.current = null;
            this.statistics = new ArrayList<>();
        }
    }
    
    public static class CurrentSwarm {
        public String pokemonName;
        public String swarmType; // "AUTOMATIC" or "MANUAL"
        public String triggeredBy; // "SYSTEM" or admin username
        public String startTime;
        public SwarmLocation location;
        public String biome;
        public int totalEntitiesSpawned;
        public int entitiesAlive;
        public int entitiesCaptured;
        public int durationMinutes;
        public long startTimestamp;
        
        public CurrentSwarm(String pokemonName, String swarmType, String triggeredBy,
                          BlockPos location, String biome, int totalEntitiesSpawned) {
            this.pokemonName = pokemonName;
            this.swarmType = swarmType;
            this.triggeredBy = triggeredBy;
            this.startTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.startTimestamp = System.currentTimeMillis();
            this.location = new SwarmLocation(location.getX(), location.getY(), location.getZ());
            this.biome = biome;
            this.totalEntitiesSpawned = totalEntitiesSpawned;
            this.entitiesAlive = totalEntitiesSpawned;
            this.entitiesCaptured = 0;
            this.durationMinutes = 0;
        }
    }

    public static class SwarmRecord {
        public String swarmId;
        public String pokemonName;
        public String swarmType; // "AUTOMATIC" or "MANUAL"
        public String triggeredBy; // "SYSTEM" or admin username
        public String startTime;
        public String endTime;
        public String endReason; // "TIME", "ENTITIES", "ADMIN"
        public SwarmLocation location;
        public String biome;
        public int totalEntitiesSpawned;
        public int entitiesCaptured;
        public int durationMinutes;
        public boolean guaranteedShiny;

        public SwarmRecord(String swarmId, String pokemonName, String swarmType, String triggeredBy,
                          BlockPos location, String biome, int totalEntitiesSpawned) {
            this.swarmId = swarmId;
            this.pokemonName = pokemonName;
            this.swarmType = swarmType;
            this.triggeredBy = triggeredBy;
            this.startTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            this.location = new SwarmLocation(location.getX(), location.getY(), location.getZ());
            this.biome = biome;
            this.totalEntitiesSpawned = totalEntitiesSpawned;
            this.entitiesCaptured = 0;
            this.guaranteedShiny = true; // Always true for swarms
        }
    }

    public static class SwarmLocation {
        public int x, y, z;

        public SwarmLocation(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static void recordSwarmStart(String pokemonName, String swarmType, String triggeredBy,
                                      BlockPos location, String biome, int totalEntitiesSpawned) {
        swarmData.current = new CurrentSwarm(pokemonName, swarmType, triggeredBy, location, biome, totalEntitiesSpawned);
        saveStatistics();
        
        // Reduced logging
    }

    public static void recordSwarmEnd(String pokemonName, String endReason, int entitiesCaptured, int durationMinutes) {
        if (swarmData.current != null && swarmData.current.pokemonName.equals(pokemonName)) {
            // Move current to statistics
            String swarmId = generateSwarmId();
            SwarmRecord record = new SwarmRecord(swarmId, swarmData.current.pokemonName, swarmData.current.swarmType, 
                swarmData.current.triggeredBy, new BlockPos(swarmData.current.location.x, swarmData.current.location.y, swarmData.current.location.z), 
                swarmData.current.biome, swarmData.current.totalEntitiesSpawned);
            record.endTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            record.endReason = endReason;
            record.entitiesCaptured = entitiesCaptured;
            record.durationMinutes = durationMinutes;
            
            swarmData.statistics.add(record);
            swarmData.current = null; // Clear current swarm
            saveStatistics();
            
            // Reduced logging
        } else {
            PokeNotifier.LOGGER.warn("[SwarmStats] No active swarm found for {} to end", pokemonName);
        }
    }

    public static CurrentSwarm getCurrentSwarm() {
        return swarmData.current;
    }
    
    public static boolean hasActiveSwarm() {
        return swarmData.current != null;
    }
    
    public static void updateCurrentSwarm(int entitiesAlive, int entitiesCaptured) {
        if (swarmData.current != null) {
            swarmData.current.entitiesAlive = entitiesAlive;
            swarmData.current.entitiesCaptured = entitiesCaptured;
            swarmData.current.durationMinutes = (int) ((System.currentTimeMillis() - swarmData.current.startTimestamp) / (60 * 1000));
            saveStatistics();
        }
    }

    private static String generateSwarmId() {
        return "SWARM_" + System.currentTimeMillis();
    }

    public static void initialize() {
        loadStatistics();
        // Always force save to ensure proper JSON structure exists
        saveStatistics();
        // Reduced logging
    }
    
    public static void loadStatistics() {
        try {
            PokeNotifier.LOGGER.info("[SwarmStats] Target events directory: {}", EVENTS_DIR.getAbsolutePath());
            
            if (!EVENTS_DIR.exists()) {
                boolean created = EVENTS_DIR.mkdirs();
                PokeNotifier.LOGGER.info("[SwarmStats] Created events directory: {} at path: {}", created, EVENTS_DIR.getAbsolutePath());
                if (!created) {
                    PokeNotifier.LOGGER.error("[SwarmStats] Failed to create events directory!");
                    return;
                }
            } else {
                PokeNotifier.LOGGER.info("[SwarmStats] Events directory exists at: {}", EVENTS_DIR.getAbsolutePath());
            }
            
            File statsFile = new File(EVENTS_DIR, STATS_FILE);
            PokeNotifier.LOGGER.info("[SwarmStats] Looking for statistics file at: {}", statsFile.getAbsolutePath());
            
            if (statsFile.exists()) {
                try (FileReader reader = new FileReader(statsFile)) {
                    SwarmData loaded = GSON.fromJson(reader, SwarmData.class);
                    if (loaded != null) {
                        swarmData = loaded;
                        if (swarmData.statistics == null) swarmData.statistics = new ArrayList<>();
                        // Reduced logging
                    } else {
                        swarmData = new SwarmData();
                        // Reduced logging
                    }
                }
            } else {
                swarmData = new SwarmData();
                // Ensure the structure is properly initialized
                if (swarmData.statistics == null) swarmData.statistics = new ArrayList<>();
                saveStatistics(); // Create initial empty file with proper structure
                // Reduced logging
            }
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("[SwarmStats] Failed to load swarm statistics", e);
            swarmData = new SwarmData();
            if (swarmData.statistics == null) swarmData.statistics = new ArrayList<>();
            // Try to create the file anyway
            try {
                saveStatistics();
                // Reduced logging
            } catch (Exception saveEx) {
                PokeNotifier.LOGGER.error("[SwarmStats] Failed to create initial statistics file", saveEx);
            }
        }
    }

    public static void saveStatistics() {
        try {
            if (!EVENTS_DIR.exists()) {
                boolean created = EVENTS_DIR.mkdirs();
                if (!created) {
                    PokeNotifier.LOGGER.error("[SwarmStats] Failed to create events directory at: {}", EVENTS_DIR.getAbsolutePath());
                    return;
                }
                PokeNotifier.LOGGER.info("[SwarmStats] Created events directory for save at: {}", EVENTS_DIR.getAbsolutePath());
            }
            
            File statsFile = new File(EVENTS_DIR, STATS_FILE);
            try (FileWriter writer = new FileWriter(statsFile)) {
                GSON.toJson(swarmData, writer);
                writer.flush();
            }
            // Reduced logging
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("[SwarmStats] Failed to save swarm statistics", e);
        }
    }

    public static List<SwarmRecord> getSwarmHistory() {
        return new ArrayList<>(swarmData.statistics);
    }

    public static int getTotalSwarms() {
        return swarmData.statistics.size();
    }

    public static int getAutomaticSwarms() {
        return (int) swarmData.statistics.stream().filter(r -> "AUTOMATIC".equals(r.swarmType)).count();
    }

    public static int getManualSwarms() {
        return (int) swarmData.statistics.stream().filter(r -> "MANUAL".equals(r.swarmType)).count();
    }
}