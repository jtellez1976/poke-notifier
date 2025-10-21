/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.event;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.zehro_mc.pokenotifier.client.compat.WaypointTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles automatic waypoint removal when Pokemon are captured, die, or disappear.
 */
public class PokemonWaypointHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokemonWaypointHandler.class);
    
    private static final Set<UUID> trackedPokemon = new HashSet<>();
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Check every second (20 ticks)
    
    /**
     * Initializes the Pokemon waypoint monitoring system.
     */
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                return;
            }
            
            tickCounter++;
            if (tickCounter >= CHECK_INTERVAL) {
                tickCounter = 0;
                checkPokemonStatus(client);
            }
        });
        
        LOGGER.info("[POKEMON WAYPOINT HANDLER] Initialized Pokemon waypoint monitoring");
    }
    
    /**
     * Checks the status of all tracked Pokemon and removes waypoints if necessary.
     */
    private static void checkPokemonStatus(MinecraftClient client) {
        if (client.world == null) {
            return;
        }
        
        Set<UUID> currentPokemon = new HashSet<>();
        
        // Collect all current Pokemon entities
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PokemonEntity pokemonEntity) {
                currentPokemon.add(pokemonEntity.getUuid());
                
                // Check if this Pokemon should have its waypoint removed
                WaypointTracker.checkAndRemoveWaypoint(pokemonEntity);
            }
        }
        
        // Find Pokemon that are no longer in the world (disappeared/despawned)
        Set<UUID> disappearedPokemon = new HashSet<>(trackedPokemon);
        disappearedPokemon.removeAll(currentPokemon);
        
        // Remove waypoints for disappeared Pokemon
        for (UUID pokemonId : disappearedPokemon) {
            WaypointTracker.unregisterWaypoint(pokemonId);
            LOGGER.debug("[POKEMON WAYPOINT HANDLER] Removed waypoint for disappeared Pokemon: {}", pokemonId);
        }
        
        // Update tracked Pokemon set
        trackedPokemon.clear();
        trackedPokemon.addAll(currentPokemon);
    }
    
    /**
     * Manually registers a Pokemon for waypoint tracking.
     * @param pokemonEntity The Pokemon entity to track
     */
    public static void registerPokemonForTracking(PokemonEntity pokemonEntity) {
        if (pokemonEntity != null) {
            trackedPokemon.add(pokemonEntity.getUuid());
            LOGGER.debug("[POKEMON WAYPOINT HANDLER] Registered Pokemon for tracking: {}", pokemonEntity.getUuid());
        }
    }
    
    /**
     * Manually unregisters a Pokemon from waypoint tracking.
     * @param pokemonEntity The Pokemon entity to stop tracking
     */
    public static void unregisterPokemonFromTracking(PokemonEntity pokemonEntity) {
        if (pokemonEntity != null) {
            trackedPokemon.remove(pokemonEntity.getUuid());
            WaypointTracker.unregisterWaypoint(pokemonEntity.getUuid());
            LOGGER.debug("[POKEMON WAYPOINT HANDLER] Unregistered Pokemon from tracking: {}", pokemonEntity.getUuid());
        }
    }
    
    /**
     * Gets the number of currently tracked Pokemon.
     * @return Number of tracked Pokemon
     */
    public static int getTrackedPokemonCount() {
        return trackedPokemon.size();
    }
    
    /**
     * Clears all tracked Pokemon (useful for cleanup).
     */
    public static void clearAllTrackedPokemon() {
        trackedPokemon.clear();
        LOGGER.info("[POKEMON WAYPOINT HANDLER] Cleared all tracked Pokemon");
    }
}