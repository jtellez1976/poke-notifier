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
import com.zehro_mc.pokenotifier.client.PokeNotifierClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Integration with Xaero's Minimap and Worldmap mods.
 * Provides waypoint creation functionality when the mods are available.
 */
public class XaeroIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroIntegration.class);
    
    private static boolean xaeroAvailable = false;
    private static Method addWaypointMethod;
    
    /**
     * Initializes the Xaero integration. Call this during client initialization.
     */
    public static void initialize() {
        checkXaeroAvailability();
    }
    
    static {
        // Static initialization for early detection
        checkXaeroAvailability();
    }
    
    /**
     * Checks if Xaero's mods are installed and available.
     */
    private static void checkXaeroAvailability() {
        try {
            // Check if both Xaero's Minimap and Worldmap are loaded
            boolean minimapLoaded = FabricLoader.getInstance().isModLoaded("xaerominimap");
            boolean worldmapLoaded = FabricLoader.getInstance().isModLoaded("xaeroworldmap");
            
            if (minimapLoaded || worldmapLoaded) {
                // At least one Xaero mod is available
                xaeroAvailable = true;
                LOGGER.info("[XAERO INTEGRATION] Xaero's mods detected (Minimap: {}, Worldmap: {}) - waypoint integration ENABLED", 
                    minimapLoaded, worldmapLoaded);
                System.out.println("[Poke Notifier] Xaero's integration ENABLED - waypoint buttons will appear in chat");
            } else {
                xaeroAvailable = false;
                LOGGER.info("[XAERO INTEGRATION] Xaero's mods not detected - using coordinate fallback");
                System.out.println("[Poke Notifier] Xaero's integration DISABLED - coordinates will be shown instead");
            }
        } catch (Exception e) {
            xaeroAvailable = false;
            LOGGER.warn("[XAERO INTEGRATION] Error checking Xaero's availability: {}", e.getMessage());
            System.out.println("[Poke Notifier] Error checking Xaero's mods: " + e.getMessage());
        }
    }
    
    /**
     * Checks if Xaero's mods are available for waypoint creation.
     * @return true if Xaero's mods are installed and functional
     */
    public static boolean isXaeroAvailable() {
        return xaeroAvailable;
    }
    
    /**
     * Creates a clickable waypoint button for chat messages.
     * @param pokemonName The name of the Pokemon
     * @param x X coordinate
     * @param y Y coordinate  
     * @param z Z coordinate
     * @param color Waypoint color (RGB int)
     * @return Clickable text component for adding waypoint
     */
    public static Text createWaypointButton(String pokemonName, int x, int y, int z, int color) {
        if (!xaeroAvailable || !ConfigManager.getClientConfig().create_waypoints_enabled) {
            return null; // Fallback will be used
        }
        
        // Clean pokemon name for waypoint (remove special characters and emojis)
        String tempName = pokemonName.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (tempName.isEmpty()) {
            tempName = "Pokemon";
        }
        if (tempName.length() > 15) {
            tempName = tempName.substring(0, 15);
        }
        final String cleanName = tempName;
        
        // Check if auto-waypoint is enabled and not in Catch'em All mode
        boolean shouldAutoCreate = ConfigManager.getClientConfig().auto_waypoint_enabled && 
            (PokeNotifierClient.currentCatchEmAllGeneration == null || "none".equals(PokeNotifierClient.currentCatchEmAllGeneration));
            
        if (shouldAutoCreate) {
            // Auto-create waypoint
            MinecraftClient.getInstance().execute(() -> {
                if (XaeroWaypointIntegration.addWaypoint(cleanName, x, y, z)) {
                    LOGGER.debug("[XAERO INTEGRATION] Auto-created waypoint: {}", cleanName);
                }
            });
            return Text.literal("[Added]")
                .styled(style -> style
                    .withColor(Formatting.GREEN)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Text.literal("Waypoint auto-created: " + cleanName + "\n" +
                                   "Location: " + x + ", " + y + ", " + z)
                            .formatted(Formatting.YELLOW))));
        } else {
            // Manual waypoint button - create immediately when text is clicked
            Text addButton = Text.literal("[Add]")
                .styled(style -> style
                    .withColor(Formatting.AQUA)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Text.literal("Click to add waypoint: " + cleanName + "\n" +
                                   "Location: " + x + ", " + y + ", " + z)
                            .formatted(Formatting.YELLOW))));
            
            // Create waypoint immediately when this text is created
            MinecraftClient.getInstance().execute(() -> {
                if (XaeroWaypointIntegration.addWaypoint(cleanName, x, y, z)) {
                    LOGGER.debug("[XAERO INTEGRATION] Manual waypoint created: {}", cleanName);
                }
            });
            
            return Text.literal("[Added]")
                .styled(style -> style
                    .withColor(Formatting.GREEN)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Text.literal("Waypoint created: " + cleanName + "\n" +
                                   "Location: " + x + ", " + y + ", " + z)
                            .formatted(Formatting.YELLOW))));
        }
    }
    
    /**
     * Creates coordinate fallback text when Xaero's is not available.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Formatted coordinate text
     */
    public static Text createCoordinateFallback(int x, int y, int z) {
        return Text.literal(x + ", " + y + ", " + z)
            .styled(style -> style
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, x + " " + y + " " + z))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Text.literal("Click to copy coordinates to clipboard")
                        .formatted(Formatting.YELLOW))));
    }
    
    /**
     * Creates a waypoint button for a specific Pokemon entity (enables tracking).
     * @param pokemonEntity The Pokemon entity
     * @param pokemonName The Pokemon name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param color Waypoint color
     * @return Clickable text component for adding waypoint
     */
    public static Text createWaypointButtonForPokemon(PokemonEntity pokemonEntity, String pokemonName, int x, int y, int z, int color) {
        if (!xaeroAvailable || !ConfigManager.getClientConfig().create_waypoints_enabled) {
            return null;
        }
        
        String tempName = pokemonName.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (tempName.isEmpty()) tempName = "Pokemon";
        if (tempName.length() > 15) tempName = tempName.substring(0, 15);
        final String cleanName = tempName;
        
        // Check if auto-waypoint is enabled and not in Catch'em All mode
        boolean shouldAutoCreate = ConfigManager.getClientConfig().auto_waypoint_enabled && 
            (PokeNotifierClient.currentCatchEmAllGeneration == null || "none".equals(PokeNotifierClient.currentCatchEmAllGeneration));
            
        if (shouldAutoCreate) {
            // Auto-create waypoint with Pokemon tracking
            MinecraftClient.getInstance().execute(() -> {
                if (XaeroWaypointIntegration.addWaypoint(cleanName, x, y, z)) {
                    LOGGER.debug("[XAERO INTEGRATION] Auto-created waypoint: {}", cleanName);
                    if (ConfigManager.getClientConfig().auto_remove_waypoints && pokemonEntity != null) {
                        WaypointTracker.registerWaypoint(pokemonEntity, cleanName);
                    }
                }
            });
            return Text.literal("[Added]")
                .styled(style -> style
                    .withColor(Formatting.GREEN)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Text.literal("Waypoint auto-created: " + cleanName + "\n" +
                                   "Location: " + x + ", " + y + ", " + z)
                            .formatted(Formatting.YELLOW))));
        } else {
            // Manual waypoint button - create immediately when text is created
            MinecraftClient.getInstance().execute(() -> {
                if (XaeroWaypointIntegration.addWaypoint(cleanName, x, y, z)) {
                    LOGGER.debug("[XAERO INTEGRATION] Manual Pokemon waypoint created: {}", cleanName);
                    if (ConfigManager.getClientConfig().auto_remove_waypoints && pokemonEntity != null) {
                        WaypointTracker.registerWaypoint(pokemonEntity, cleanName);
                    }
                }
            });
            
            return Text.literal("[Added]")
                .styled(style -> style
                    .withColor(Formatting.GREEN)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        Text.literal("Waypoint created: " + cleanName + "\n" +
                                   "Location: " + x + ", " + y + ", " + z)
                            .formatted(Formatting.YELLOW))));
        }
    }
    
    /**
     * Removes a waypoint by name.
     * @param waypointName The name of the waypoint to remove
     * @return true if removal command was sent successfully
     */
    public static boolean removeWaypoint(String waypointName) {
        if (!xaeroAvailable) {
            return false;
        }
        
        try {
            // Try direct integration method first
            if (XaeroWaypointIntegration.removeWaypoint(waypointName)) {
                LOGGER.debug("[XAERO INTEGRATION] Successfully removed waypoint via integration: {}", waypointName);
                return true;
            }
            
            LOGGER.warn("[XAERO INTEGRATION] Failed to remove waypoint via integration: {}", waypointName);
            return false;
            
        } catch (Exception e) {
            LOGGER.warn("[XAERO INTEGRATION] Exception removing waypoint '{}': {}", waypointName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if waypoint creation is enabled in config.
     * @return true if waypoint creation is enabled
     */
    public static boolean isWaypointCreationEnabled() {
        return ConfigManager.getClientConfig().create_waypoints_enabled;
    }
}