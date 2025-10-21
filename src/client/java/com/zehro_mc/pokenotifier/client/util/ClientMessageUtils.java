/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.util;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.client.compat.XaeroIntegration;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Client-side utility class for creating waypoint messages with proper tracking.
 */
public class ClientMessageUtils {

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
        if (ConfigManager.getClientConfig().create_waypoints_enabled && XaeroIntegration.isXaeroAvailable()) {
            // Create waypoint button using XaeroIntegration
            Text waypointButton = XaeroIntegration.createWaypointButton(name, x, y, z, color);
            if (waypointButton != null) {
                return (MutableText) waypointButton;
            }
        }
        // Fallback to coordinates
        return (MutableText) XaeroIntegration.createCoordinateFallback(x, y, z);
    }
    
    /**
     * Creates a text component with waypoint button for a specific Pokemon entity.
     * This enables automatic waypoint tracking and removal.
     * @param pokemonEntity The Pokemon entity
     * @param name Display name for the waypoint
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param color Waypoint color (0xRRGGBB format)
     * @return Text component with waypoint button OR coordinates (not both)
     */
    public static MutableText createLocationTextForPokemon(PokemonEntity pokemonEntity, String name, int x, int y, int z, int color) {
        // Check if waypoint creation is enabled and Xaero's is available
        if (ConfigManager.getClientConfig().create_waypoints_enabled && XaeroIntegration.isXaeroAvailable()) {
            // Create waypoint button with Pokemon tracking
            Text waypointButton = XaeroIntegration.createWaypointButtonForPokemon(pokemonEntity, name, x, y, z, color);
            if (waypointButton != null) {
                return (MutableText) waypointButton;
            }
        }
        // Fallback to coordinates
        return (MutableText) XaeroIntegration.createCoordinateFallback(x, y, z);
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
}