/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier;

/**
 * Defines all server-side only configuration options.
 * This class is serialized to config-server.json.
 */
public class ConfigServer {

    public int config_version = 6;

    public String[] _instructions = new String[]{
            "Poke Notifier Configuration - Server-Only Settings",
            "This file contains settings that only affect the server.",
            "debug_mode_enabled: If true, prints detailed logs to the console.",
            "enable_test_mode: If true, also triggers for command-spawned Pokémon.",
            "bounty_system_enabled: If true, the server will automatically post bounties for random Pokémon.",
            "bounty_check_interval_seconds: How often (in seconds) the system tries to start a new bounty.",
            "bounty_start_chance_percent: The percentage chance (0-100) of a bounty starting during each interval.",
            "bounty_duration_minutes: How long (in minutes) a bounty lasts before it expires.",
            "bounty_reminder_interval_minutes: How often (in minutes) to remind players about an active bounty. Set to 0 to disable.",
            "active_bounty: The currently active bounty Pokémon. Do not edit this manually. To clear, set to null.",
            "rival_notification_cooldown_seconds: How long (in seconds) a player must wait between rival notifications.",
            "rival_notification_override_distance: If a rival is within this distance (in blocks), they will be notified regardless of the cooldown.",
            "update_checker_source: The platform to check for updates ('modrinth', 'curseforge', 'none', or 'unknown').",
            "swarm_notifications_enabled: If true, a global announcement will be made when a Pokémon swarm starts.",
            "swarm_system_enabled: If true, the server will automatically start random swarms.",
            "swarm_check_interval_minutes: How often (in minutes) the system tries to start a new swarm.",
            "swarm_start_chance_percent: The percentage chance (0-100) of a swarm starting during each interval.",
            "swarm_duration_minutes: How long (in minutes) a swarm lasts before it expires.",
            "swarm_cooldown_minutes: Minimum time (in minutes) between swarms.",
            "swarm_spawn_multiplier: How many times more spawns occur during a swarm (5-15x recommended).",
            "swarm_shiny_multiplier: Shiny odds multiplier during swarms (2-3x recommended).",
            "swarm_radius_blocks: Radius in blocks around the swarm center where increased spawns occur.",
            "swarm_pokemon_count_min: Minimum number of Pokémon to spawn in a swarm (default: 5).",
            "swarm_pokemon_count_max: Maximum number of Pokémon to spawn in a swarm (default: 10).",
            "swarm_min_distance: Minimum distance from players to generate swarm location.",
            "swarm_max_distance: Maximum distance from players to generate swarm location.",
            "active_swarm_pokemon: The currently active swarm Pokémon. Do not edit manually.",
            "active_swarm_location: The current swarm location coordinates. Do not edit manually.",
            "active_swarm_biome: The biome where the current swarm is active. Do not edit manually.",
            "global_hunt_system_enabled: If true, the Global Hunt system is available for use."
    };

    public boolean debug_mode_enabled = false;
    public boolean enable_test_mode = false;
    public boolean bounty_system_enabled = false;
    public int bounty_check_interval_seconds = 60;
    public int bounty_start_chance_percent = 15;
    public int bounty_duration_minutes = 60;
    public int bounty_reminder_interval_minutes = 15;
    public int rival_notification_cooldown_seconds = 60;
    public int rival_notification_override_distance = 200;
    public String update_checker_source = "unknown";
    public boolean swarm_notifications_enabled = true;
    public boolean swarm_system_enabled = true;
    public int swarm_check_interval_minutes = 180; // 3 hours
    public int swarm_start_chance_percent = 35;
    public int swarm_duration_minutes = 25;
    public int swarm_cooldown_minutes = 120; // 2 hours minimum
    public int swarm_spawn_multiplier = 8;
    public double swarm_shiny_multiplier = 2.5;
    public int swarm_radius_blocks = 250;
    public int swarm_pokemon_count_min = 5;
    public int swarm_pokemon_count_max = 10;
    public int swarm_min_distance = 1500;
    public int swarm_max_distance = 4000;
    public String active_swarm_pokemon = null;
    public String active_swarm_location = null;
    public String active_swarm_biome = null;
    public boolean global_hunt_system_enabled = false;
    public String active_bounty = null;
}