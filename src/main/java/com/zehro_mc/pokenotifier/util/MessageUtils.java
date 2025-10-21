/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.zehro_mc.pokenotifier.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Utility class for creating consistent waypoint messages with automatic fallback.
 * Provides unified waypoint integration across all coordinate displays.
 */
public class MessageUtils {

    /**
     * Creates a text component with waypoint button or coordinate fallback.
     * @param name Display name for the waypoint
     * @param pos Block position
     * @param color Waypoint color (0xRRGGBB format)
     * @return Text component with waypoint button or coordinates
     */
    public static MutableText createLocationText(String name, BlockPos pos, int color) {
        return createLocationText(name, pos.getX(), pos.getY(), pos.getZ(), color);
    }

    /**
     * Creates a text component with waypoint button or coordinate fallback.
     * @param name Display name for the waypoint
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param color Waypoint color (0xRRGGBB format)
     * @return Text component with waypoint button OR coordinates (not both)
     */
    public static MutableText createLocationText(String name, int x, int y, int z, int color) {
        // Check if waypoint creation is enabled and Xaero's is available
        if (ConfigManager.getClientConfig().create_waypoints_enabled && isXaeroAvailable()) {
            // Only show waypoint button, no coordinates
            return createWaypointButton(name, x, y, z, color);
        }
        // Fallback to coordinates
        return createCoordinateFallback(x, y, z);
    }
    
    /**
     * Creates a text component with waypoint button for a specific Pokemon entity.
     * This enables automatic waypoint tracking and removal.
     * @param pokemonEntity The Pokemon entity (unused in main-side implementation)
     * @param name Display name for the waypoint
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param color Waypoint color (0xRRGGBB format)
     * @return Text component with waypoint button OR coordinates (not both)
     */
    public static MutableText createLocationTextForPokemon(PokemonEntity pokemonEntity, String name, int x, int y, int z, int color) {
        // For main-side implementation, just use regular waypoint creation
        return createLocationText(name, x, y, z, color);
    }

    /**
     * Creates a simple coordinate text without waypoint functionality.
     * @param pos Block position
     * @return Formatted coordinate text
     */
    public static MutableText createSimpleCoordinateText(BlockPos pos) {
        return Text.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                .formatted(Formatting.AQUA);
    }

    /**
     * Creates a waypoint button for Xaero's integration.
     * @param name Waypoint name
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param color Waypoint color
     * @return Clickable waypoint button
     */
    private static MutableText createWaypointButton(String name, int x, int y, int z, int color) {
        // Clean pokemon name for waypoint (remove special characters and emojis)
        String tempName = name.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (tempName.isEmpty()) {
            tempName = "Pokemon";
        }
        if (tempName.length() > 15) {
            tempName = tempName.substring(0, 15);
        }
        final String cleanName = tempName;
        
        // Create waypoint command for Xaero's
        String waypointCommand = "xaero_waypoint_add:" + cleanName + ":" + cleanName.substring(0, Math.min(1, cleanName.length())) + ":" + x + ":" + y + ":" + z + ":6:false:0:Internal-xaero-waypoint";
        
        return Text.literal("[Add Waypoint]")
                .styled(style -> style
                        .withColor(Formatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + waypointCommand))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Click to add waypoint: " + cleanName + "\n" +
                                           "Location: " + x + ", " + y + ", " + z)
                                        .formatted(Formatting.YELLOW))));
    }
    
    /**
     * Creates coordinate fallback text when Xaero's is not available.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Formatted coordinate text with copy functionality
     */
    private static MutableText createCoordinateFallback(int x, int y, int z) {
        String coordinates = x + ", " + y + ", " + z;
        return Text.literal(coordinates)
                .styled(style -> style
                        .withColor(Formatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coordinates))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Click to copy coordinates to clipboard")
                                        .formatted(Formatting.YELLOW))));
    }
    
    /**
     * Detects if Xaero's mods are available at runtime.
     * @return true if Xaero's is detected, false otherwise
     */
    private static boolean isXaeroAvailable() {
        return FabricLoader.getInstance().isModLoaded("xaerominimap") || 
               FabricLoader.getInstance().isModLoaded("xaeroworldmap");
    }
    
    /**
     * Creates a formatted message with location information for a Pokemon entity.
     * This enables automatic waypoint tracking and removal.
     * @param prefix Message prefix
     * @param pokemonEntity The Pokemon entity
     * @param locationName Name of the location/Pokemon
     * @param pos Block position
     * @param color Waypoint color
     * @return Complete formatted message
     */
    public static MutableText createLocationMessageForPokemon(String prefix, PokemonEntity pokemonEntity, String locationName, BlockPos pos, int color) {
        return Text.literal(prefix)
                .formatted(Formatting.YELLOW)
                .append(Text.literal(locationName).formatted(Formatting.GOLD))
                .append(Text.literal(" at ").formatted(Formatting.YELLOW))
                .append(createLocationTextForPokemon(pokemonEntity, locationName, pos.getX(), pos.getY(), pos.getZ(), color));
    }

    /**
     * Creates a formatted message with location information.
     * @param prefix Message prefix
     * @param locationName Name of the location/Pokemon
     * @param pos Block position
     * @param color Waypoint color
     * @return Complete formatted message
     */
    public static MutableText createLocationMessage(String prefix, String locationName, BlockPos pos, int color) {
        return Text.literal(prefix)
                .formatted(Formatting.YELLOW)
                .append(Text.literal(locationName).formatted(Formatting.GOLD))
                .append(Text.literal(" at ").formatted(Formatting.YELLOW))
                .append(createLocationText(locationName, pos, color));
    }

    /**
     * Default colors for different types of waypoints.
     */
    public static class Colors {
        public static final int SWARM = 0x00FFFF;      // Cyan
        public static final int GLOBAL_HUNT = 0xFF0000; // Red
        public static final int BOUNTY = 0xFFD700;     // Gold
        public static final int RARE = 0xFF69B4;       // Hot Pink
        public static final int LEGENDARY = 0x8A2BE2;  // Blue Violet
        public static final int SHINY = 0xFFFFFF;      // White
    }
}