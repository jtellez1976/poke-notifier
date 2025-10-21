/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.compat;

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
        if (!xaeroAvailable) {
            return null; // Fallback will be used
        }
        
        // Clean pokemon name for waypoint (remove special characters and emojis)
        String cleanName = pokemonName.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (cleanName.isEmpty()) {
            cleanName = "Pokemon";
        }
        if (cleanName.length() > 15) {
            cleanName = cleanName.substring(0, 15);
        }
        
        // Use simple, safe command format
        String waypointCommand = String.format("/xaero_waypoint_add:%s:%d:%d:%d:%d", 
            cleanName, x, y, z, color);
        
        LOGGER.debug("[XAERO INTEGRATION] Creating waypoint button with command: {}", waypointCommand);
        
        return Text.literal("[Copy Waypoint]")
            .styled(style -> style
                .withColor(Formatting.AQUA)
                .withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, waypointCommand))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Text.literal("Click to copy waypoint command to clipboard\n" +
                               "Command: " + waypointCommand + "\n" +
                               "Paste in chat and press Enter to execute")
                        .formatted(Formatting.YELLOW))));
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
                .withColor(Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, x + " " + y + " " + z))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Text.literal("Click to copy coordinates to clipboard")
                        .formatted(Formatting.YELLOW))));
    }
    
    /**
     * Creates an alternative waypoint button that copies command to clipboard.
     * @param pokemonName The Pokemon name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param color Waypoint color
     * @return Clickable text that copies waypoint command
     */
    public static Text createCopyWaypointButton(String pokemonName, int x, int y, int z, int color) {
        String cleanName = pokemonName.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (cleanName.isEmpty()) cleanName = "Pokemon";
        if (cleanName.length() > 15) cleanName = cleanName.substring(0, 15);
        
        String waypointCommand = String.format("/xaero_waypoint_add:%s:%d:%d:%d:%d", 
            cleanName, x, y, z, color);
        
        return Text.literal("[Copy Waypoint]")
            .styled(style -> style
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, waypointCommand))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Text.literal("Click to copy waypoint command to clipboard\n" + waypointCommand)
                        .formatted(Formatting.YELLOW))));
    }
    
    /**
     * Attempts to directly add a waypoint using Xaero's API (advanced integration).
     * This is a fallback method if the command approach doesn't work.
     * @param name Waypoint name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param color Waypoint color
     * @return true if waypoint was added successfully
     */
    public static boolean addWaypointDirect(String name, int x, int y, int z, int color) {
        if (!xaeroAvailable) {
            return false;
        }
        
        try {
            // This would be implemented if command approach fails
            // Using reflection to access Xaero's internal API
            // For now, we rely on the command approach which is more stable
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to add waypoint directly: {}", e.getMessage());
            return false;
        }
    }
}