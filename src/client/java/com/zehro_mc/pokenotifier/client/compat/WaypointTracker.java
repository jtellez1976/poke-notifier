/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.compat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.client.data.WaypointManager;
import com.zehro_mc.pokenotifier.client.data.WaypointData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks waypoints created for Pokemon and automatically removes them when appropriate.
 */
public class WaypointTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaypointTracker.class);
    
    // Map of Pokemon UUID to waypoint name for tracking
    private static final Map<UUID, String> trackedWaypoints = new ConcurrentHashMap<>();
    
    // Flag to indicate when the mod is creating a waypoint (to distinguish from manual user waypoints)
    private static volatile boolean isModCreatingWaypoint = false;
    
    /**
     * Registers a waypoint for a Pokemon entity.
     * @param pokemonEntity The Pokemon entity
     * @param waypointName The name of the created waypoint
     */
    public static void registerWaypoint(PokemonEntity pokemonEntity, String waypointName) {
        if (!ConfigManager.getClientConfig().auto_remove_waypoints) {
            return;
        }
        
        UUID pokemonId = pokemonEntity.getUuid();
        trackedWaypoints.put(pokemonId, waypointName);
        
        // Add to persistent storage
        BlockPos pos = pokemonEntity.getBlockPos();
        WaypointManager.addWaypoint(waypointName, pos.getX(), pos.getY(), pos.getZ(), 
            pokemonId.toString(), WaypointData.WaypointType.POKEMON);
        
        LOGGER.info("[WAYPOINT TRACKER] Registered waypoint '{}' for Pokemon {} (Total: {})", 
            waypointName, pokemonId, getTrackedWaypointCount());
    }
    
    /**
     * Registers a waypoint by coordinates (for events without specific Pokemon entity).
     * @param x X coordinate
     * @param y Y coordinate  
     * @param z Z coordinate
     * @param waypointName The name of the created waypoint
     */
    public static void registerWaypointByLocation(int x, int y, int z, String waypointName) {
        // For location-based waypoints, we'll use a special UUID based on coordinates
        String locationKey = x + "_" + y + "_" + z;
        UUID locationId = UUID.nameUUIDFromBytes(locationKey.getBytes());
        trackedWaypoints.put(locationId, waypointName);
        
        // Add to persistent storage
        WaypointManager.addWaypoint(waypointName, x, y, z, null, WaypointData.WaypointType.EVENT);
        
        LOGGER.info("[WAYPOINT TRACKER] Registered location waypoint '{}' at {}, {}, {} (Total: {})", 
            waypointName, x, y, z, getTrackedWaypointCount());
    }
    
    /**
     * Registers a waypoint by name only (for command-created waypoints).
     * Only registers if the mod is currently creating the waypoint.
     * @param waypointName The name of the created waypoint
     */
    public static void registerWaypointByName(String waypointName) {
        if (!isModCreatingWaypoint) {
            // This is a manual user waypoint, don't track it
            return;
        }
        
        // Create a UUID based on the waypoint name for tracking
        UUID waypointId = UUID.nameUUIDFromBytes(("waypoint_" + waypointName).getBytes());
        trackedWaypoints.put(waypointId, waypointName);
        LOGGER.info("[WAYPOINT TRACKER] Registered mod waypoint '{}' (Total tracked: {})", waypointName, trackedWaypoints.size());
    }
    
    /**
     * Sets the flag indicating the mod is about to create a waypoint.
     * This should be called before any waypoint creation by the mod.
     */
    public static void setModCreatingWaypoint(boolean creating) {
        isModCreatingWaypoint = creating;
    }
    
    /**
     * Checks if the mod is currently creating a waypoint.
     * @return true if mod is creating waypoint
     */
    public static boolean isModCreatingWaypoint() {
        return isModCreatingWaypoint;
    }
    
    /**
     * Checks if a Pokemon should have its waypoint removed and removes it if necessary.
     * @param pokemonEntity The Pokemon entity to check
     */
    public static void checkAndRemoveWaypoint(PokemonEntity pokemonEntity) {
        if (!ConfigManager.getClientConfig().auto_remove_waypoints) {
            return;
        }
        
        UUID pokemonId = pokemonEntity.getUuid();
        String waypointName = trackedWaypoints.get(pokemonId);
        
        if (waypointName != null) {
            // Check if Pokemon is dead, removed, or captured
            if (!pokemonEntity.isAlive() || pokemonEntity.isRemoved() || pokemonEntity.getOwner() != null) {
                removeWaypoint(waypointName);
                trackedWaypoints.remove(pokemonId);
                
                // Remove from persistent storage
                WaypointManager.removeWaypointByPokemon(pokemonId.toString());
                
                LOGGER.info("[WAYPOINT TRACKER] Removed waypoint '{}' for Pokemon {} (Total: {})", 
                    waypointName, pokemonId, getTrackedWaypointCount());
            }
        }
    }
    
    /**
     * Removes a waypoint by name using Xaero's command.
     * @param waypointName The name of the waypoint to remove
     */
    private static void removeWaypoint(String waypointName) {
        if (!XaeroIntegration.isXaeroAvailable()) {
            return;
        }
        
        try {
            // Use XaeroIntegration's remove method instead of direct command
            if (XaeroIntegration.removeWaypoint(waypointName)) {
                LOGGER.debug("[WAYPOINT TRACKER] Successfully removed waypoint: {}", waypointName);
            } else {
                LOGGER.warn("[WAYPOINT TRACKER] Failed to remove waypoint: {}", waypointName);
            }
        } catch (Exception e) {
            LOGGER.warn("[WAYPOINT TRACKER] Error removing waypoint '{}': {}", waypointName, e.getMessage());
        }
    }
    
    /**
     * Manually removes a waypoint and stops tracking it.
     * @param pokemonId The UUID of the Pokemon
     */
    public static void unregisterWaypoint(UUID pokemonId) {
        String waypointName = trackedWaypoints.remove(pokemonId);
        if (waypointName != null) {
            removeWaypoint(waypointName);
            LOGGER.debug("[WAYPOINT TRACKER] Manually unregistered waypoint '{}' for Pokemon {}", waypointName, pokemonId);
        }
    }
    
    /**
     * Clears all tracked waypoints (useful for cleanup).
     * Only removes waypoints that were created by the mod, not manual user waypoints.
     */
    public static void clearAllTrackedWaypoints() {
        if (!ConfigManager.getClientConfig().auto_remove_waypoints) {
            return;
        }
        
        int removedCount = 0;
        for (String waypointName : trackedWaypoints.values()) {
            removeWaypoint(waypointName);
            removedCount++;
        }
        trackedWaypoints.clear();
        
        // Clear persistent storage
        WaypointManager.clearAllWaypoints();
        
        LOGGER.info("[WAYPOINT TRACKER] Cleared {} mod-created waypoints (manual waypoints preserved)", removedCount);
    }
    
    /**
     * Gets the number of currently tracked waypoints.
     * @return Number of tracked waypoints
     */
    public static int getTrackedWaypointCount() {
        return WaypointManager.getWaypointCount();
    }
    
    /**
     * Checks if waypoint auto-removal is enabled.
     * @return true if auto-removal is enabled
     */
    public static boolean isAutoRemovalEnabled() {
        return ConfigManager.getClientConfig().auto_remove_waypoints;
    }
}