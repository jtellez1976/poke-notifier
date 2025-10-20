/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.model;

/**
 * Configuration for all server events including Global Hunt, Bounty System, Swarms, and Rivals.
 * This file is automatically created in the events/ folder when the server starts.
 * 
 * CONFIGURATION INSTRUCTIONS:
 * - Set "enabled" to true/false to enable/disable each event system
 * - Distance values are in blocks (1500-4000 recommended for Global Hunt)
 * - Duration is in minutes (5-120 recommended)
 * - Dimensions: "overworld", "nether", "end" (use lowercase)
 * - Statistics are automatically updated by the server
 * 
 * WARNING: Backup this file before making changes!
 */
public class EventsConfig {
    public int config_version = 1;
    
    // === GLOBAL HUNT SYSTEM ===
    public boolean globalHuntEnabled = false;
    public int globalHuntMinDistance = 1500; // Minimum spawn distance from players (blocks)
    public int globalHuntMaxDistance = 4000; // Maximum spawn distance from players (blocks)
    public int globalHuntDurationMinutes = 15; // Event duration in minutes
    public boolean globalHuntAllowOverworld = true; // Allow spawning in Overworld
    public boolean globalHuntAllowNether = false; // Allow spawning in Nether
    public boolean globalHuntAllowEnd = false; // Allow spawning in End
    public int globalHuntMinIntervalHours = 2; // Minimum time between automatic events
    public int globalHuntMaxIntervalHours = 6; // Maximum time between automatic events
    
    // === BOUNTY SYSTEM ===
    public boolean bountySystemEnabled = false;
    
    // === SWARM EVENTS ===
    public boolean swarmEventsEnabled = false;
    
    // === RIVAL BATTLES ===
    public boolean rivalBattlesEnabled = false;
    
    // === STATISTICS (Auto-updated by server) ===
    public int totalGlobalHuntEvents = 0;
    public int successfulGlobalHuntEvents = 0;
    public long lastGlobalHuntEventTime = 0; // Unix timestamp
    public String lastGlobalHuntWinner = ""; // Last player to win
    public String lastGlobalHuntPokemon = ""; // Last Pokemon spawned
}