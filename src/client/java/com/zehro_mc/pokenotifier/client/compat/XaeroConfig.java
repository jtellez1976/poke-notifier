/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.compat;

/**
 * Configuration constants for Xaero's integration.
 */
public class XaeroConfig {
    
    // Waypoint colors for different Pokemon categories
    public static final int SHINY_COLOR = 0xFF6B00;      // Orange for shiny
    public static final int LEGENDARY_COLOR = 0xFF0000;   // Red for legendary
    public static final int MYTHICAL_COLOR = 0x9400D3;    // Purple for mythical
    public static final int ULTRA_BEAST_COLOR = 0x00FFFF; // Cyan for ultra beast
    public static final int HUNT_COLOR = 0x00FF00;        // Green for hunt targets
    public static final int DEFAULT_COLOR = 0xFFFF00;     // Yellow for others
    
    /**
     * Gets the appropriate waypoint color based on Pokemon category.
     * @param category The rarity category name
     * @return RGB color integer
     */
    public static int getWaypointColor(String category) {
        if (category == null) return DEFAULT_COLOR;
        
        return switch (category.toUpperCase()) {
            case "SHINY" -> SHINY_COLOR;
            case "LEGENDARY" -> LEGENDARY_COLOR;
            case "MYTHICAL" -> MYTHICAL_COLOR;
            case "ULTRA_BEAST" -> ULTRA_BEAST_COLOR;
            case "HUNT" -> HUNT_COLOR;
            default -> DEFAULT_COLOR;
        };
    }
    
    /**
     * Formats the waypoint name for better readability.
     * @param pokemonName The Pokemon name
     * @param category The rarity category
     * @return Formatted waypoint name
     */
    public static String formatWaypointName(String pokemonName, String category) {
        if ("HUNT".equals(category)) {
            return "🎯 " + pokemonName + " (Hunt)";
        } else if ("SHINY".equals(category)) {
            return "✨ " + pokemonName + " (Shiny)";
        } else {
            return "⭐ " + pokemonName + " (" + formatCategory(category) + ")";
        }
    }
    
    private static String formatCategory(String category) {
        if (category == null || category.isEmpty()) {
            return "Rare";
        }
        String[] words = category.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }
}