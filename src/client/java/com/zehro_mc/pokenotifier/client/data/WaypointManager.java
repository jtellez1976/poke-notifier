/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages waypoint persistence using waypoints.json file.
 */
public class WaypointManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaypointManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(PokeNotifier.MOD_ID).toFile();
    private static final File PLAYER_DATA_DIR = new File(CONFIG_DIR, "player_data");
    private static final File WAYPOINTS_FILE = new File(PLAYER_DATA_DIR, "waypoints.json");
    
    private static WaypointConfig waypointConfig;
    
    /**
     * Loads waypoint configuration from file.
     */
    public static void loadWaypoints() {
        if (!PLAYER_DATA_DIR.exists()) {
            PLAYER_DATA_DIR.mkdirs();
        }
        
        if (WAYPOINTS_FILE.exists()) {
            try (FileReader reader = new FileReader(WAYPOINTS_FILE)) {
                waypointConfig = GSON.fromJson(reader, WaypointConfig.class);
                if (waypointConfig == null) {
                    waypointConfig = new WaypointConfig();
                }
                LOGGER.info("[WAYPOINT MANAGER] Loaded {} tracked waypoints", waypointConfig.tracked_waypoints.size());
            } catch (Exception e) {
                LOGGER.error("[WAYPOINT MANAGER] Failed to load waypoints.json", e);
                waypointConfig = new WaypointConfig();
            }
        } else {
            waypointConfig = new WaypointConfig();
            saveWaypoints();
        }
    }
    
    /**
     * Saves waypoint configuration to file.
     */
    public static void saveWaypoints() {
        if (waypointConfig == null) return;
        
        try (FileWriter writer = new FileWriter(WAYPOINTS_FILE)) {
            GSON.toJson(waypointConfig, writer);
        } catch (IOException e) {
            LOGGER.error("[WAYPOINT MANAGER] Failed to save waypoints.json", e);
        }
    }
    
    /**
     * Adds a waypoint to tracking.
     */
    public static void addWaypoint(String name, int x, int y, int z, String pokemonUuid, WaypointData.WaypointType type) {
        if (waypointConfig == null) loadWaypoints();
        
        WaypointData waypoint = new WaypointData(name, x, y, z, pokemonUuid, type);
        waypointConfig.tracked_waypoints.add(waypoint);
        saveWaypoints();
        
        LOGGER.info("[WAYPOINT MANAGER] Added waypoint: {} at {},{},{} (Total: {})", 
            name, x, y, z, waypointConfig.tracked_waypoints.size());
    }
    
    /**
     * Removes waypoints by Pokemon UUID.
     */
    public static void removeWaypointByPokemon(String pokemonUuid) {
        if (waypointConfig == null || pokemonUuid == null) return;
        
        boolean removed = waypointConfig.tracked_waypoints.removeIf(w -> pokemonUuid.equals(w.pokemon_uuid));
        if (removed) {
            saveWaypoints();
            LOGGER.info("[WAYPOINT MANAGER] Removed waypoint for Pokemon {} (Total: {})", 
                pokemonUuid, waypointConfig.tracked_waypoints.size());
        }
    }
    
    /**
     * Removes waypoint by name.
     */
    public static void removeWaypointByName(String name) {
        if (waypointConfig == null || name == null) return;
        
        boolean removed = waypointConfig.tracked_waypoints.removeIf(w -> name.equals(w.name));
        if (removed) {
            saveWaypoints();
            LOGGER.info("[WAYPOINT MANAGER] Removed waypoint: {} (Total: {})", 
                name, waypointConfig.tracked_waypoints.size());
        }
    }
    
    /**
     * Clears all tracked waypoints.
     */
    public static void clearAllWaypoints() {
        if (waypointConfig == null) loadWaypoints();
        
        int count = waypointConfig.tracked_waypoints.size();
        waypointConfig.tracked_waypoints.clear();
        saveWaypoints();
        
        LOGGER.info("[WAYPOINT MANAGER] Cleared {} waypoints", count);
    }
    
    /**
     * Gets the number of tracked waypoints.
     */
    public static int getWaypointCount() {
        if (waypointConfig == null) loadWaypoints();
        return waypointConfig.tracked_waypoints.size();
    }
    
    /**
     * Gets all tracked waypoints.
     */
    public static List<WaypointData> getAllWaypoints() {
        if (waypointConfig == null) loadWaypoints();
        return new ArrayList<>(waypointConfig.tracked_waypoints);
    }
}