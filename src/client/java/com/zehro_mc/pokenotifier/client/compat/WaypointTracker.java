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
        LOGGER.debug("[WAYPOINT TRACKER] Registered waypoint '{}' for Pokemon {}", waypointName, pokemonId);
    }
    
    /**
     * Registers a waypoint by coordinates (for events without specific Pokemon entity).
     * @param x X coordinate
     * @param y Y coordinate  
     * @param z Z coordinate
     * @param waypointName The name of the created waypoint
     */
    public static void registerWaypointByLocation(int x, int y, int z, String waypointName) {
        if (!ConfigManager.getClientConfig().auto_remove_waypoints) {
            return;
        }
        
        // For location-based waypoints, we'll use a special UUID based on coordinates
        String locationKey = x + "_" + y + "_" + z;
        UUID locationId = UUID.nameUUIDFromBytes(locationKey.getBytes());
        trackedWaypoints.put(locationId, waypointName);
        LOGGER.debug("[WAYPOINT TRACKER] Registered location waypoint '{}' at {}, {}, {}", waypointName, x, y, z);
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
                LOGGER.debug("[WAYPOINT TRACKER] Removed waypoint '{}' for Pokemon {} (dead/captured/removed)", waypointName, pokemonId);
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
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                // Use Xaero's remove waypoint command
                String removeCommand = "/xaero_waypoint_remove:" + waypointName;
                client.player.networkHandler.sendChatCommand(removeCommand.substring(1)); // Remove the '/' prefix
                LOGGER.debug("[WAYPOINT TRACKER] Sent remove command for waypoint: {}", waypointName);
            }
        } catch (Exception e) {
            LOGGER.warn("[WAYPOINT TRACKER] Failed to remove waypoint '{}': {}", waypointName, e.getMessage());
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
     */
    public static void clearAllTrackedWaypoints() {
        if (!ConfigManager.getClientConfig().auto_remove_waypoints) {
            trackedWaypoints.clear();
            return;
        }
        
        for (String waypointName : trackedWaypoints.values()) {
            removeWaypoint(waypointName);
        }
        trackedWaypoints.clear();
        LOGGER.info("[WAYPOINT TRACKER] Cleared all tracked waypoints");
    }
    
    /**
     * Gets the number of currently tracked waypoints.
     * @return Number of tracked waypoints
     */
    public static int getTrackedWaypointCount() {
        return trackedWaypoints.size();
    }
    
    /**
     * Checks if waypoint auto-removal is enabled.
     * @return true if auto-removal is enabled
     */
    public static boolean isAutoRemovalEnabled() {
        return ConfigManager.getClientConfig().auto_remove_waypoints;
    }
}