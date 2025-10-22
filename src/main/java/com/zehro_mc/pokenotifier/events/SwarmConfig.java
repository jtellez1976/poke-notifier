/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zehro_mc.pokenotifier.PokeNotifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SwarmConfig {
    public int config_version = 1;
    
    public String[] _instructions = new String[]{
        "Poke Notifier - Swarm System Configuration",
        "This file contains all settings for the Swarm Events system.",
        "system_enabled: If true, the server will automatically start random swarms.",
        "notifications_enabled: If true, a global announcement will be made when a swarm starts.",
        "check_interval_minutes: How often (in minutes) the system tries to start a new swarm.",
        "start_chance_percent: The percentage chance (0-100) of a swarm starting during each interval.",
        "duration_minutes: How long (in minutes) a swarm lasts before it expires.",
        "cooldown_minutes: Minimum time (in minutes) between swarms.",
        "spawn_multiplier: How many times more spawns occur during a swarm (5-15x recommended).",
        "shiny_multiplier: Shiny odds multiplier during swarms (2-3x recommended).",
        "radius_blocks: Radius in blocks around the swarm center where increased spawns occur.",
        "pokemon_count_min: Minimum number of Pokémon to spawn in a swarm.",
        "pokemon_count_max: Maximum number of Pokémon to spawn in a swarm.",
        "min_distance: Minimum distance from players to generate swarm location.",
        "max_distance: Maximum distance from players to generate swarm location.",
        "NOTE: Active swarm state is now managed in swarm_statistics.json"
    };
    
    // System settings
    public boolean system_enabled = true;
    public boolean notifications_enabled = true;
    
    // Timing settings
    public int check_interval_minutes = 180; // 3 hours
    public int start_chance_percent = 35;
    public int duration_minutes = 25;
    public int cooldown_minutes = 120; // 2 hours minimum
    
    // Spawn settings
    public int spawn_multiplier = 8;
    public double shiny_multiplier = 2.5;
    public int radius_blocks = 250;
    public int pokemon_count_min = 5;
    public int pokemon_count_max = 10;
    public int min_distance = 1500;
    public int max_distance = 4000;
    
    // Active swarm state moved to swarm_statistics.json
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/poke-notifier/events";
    private static final String CONFIG_FILE = "config-swarm.json";
    
    public static SwarmConfig load() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File configFile = new File(configDir, CONFIG_FILE);
        if (!configFile.exists()) {
            SwarmConfig defaultConfig = new SwarmConfig();
            // Try to migrate from old server config
            migrateFromServerConfig(defaultConfig);
            defaultConfig.save();
            return defaultConfig;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            SwarmConfig config = GSON.fromJson(reader, SwarmConfig.class);
            if (config == null) {
                config = new SwarmConfig();
                migrateFromServerConfig(config);
            }
            return config;
        } catch (IOException e) {
            PokeNotifier.LOGGER.error("Failed to load swarm config", e);
            SwarmConfig fallback = new SwarmConfig();
            migrateFromServerConfig(fallback);
            return fallback;
        }
    }
    
    private static void migrateFromServerConfig(SwarmConfig swarmConfig) {
        // No migration needed - swarm fields have been removed from ConfigServer
        // SwarmConfig will use its default values
        PokeNotifier.LOGGER.info("[SwarmConfig] Using default swarm configuration values");
    }
    
    public void save() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File configFile = new File(configDir, CONFIG_FILE);
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            PokeNotifier.LOGGER.error("Failed to save swarm config", e);
        }
    }
}