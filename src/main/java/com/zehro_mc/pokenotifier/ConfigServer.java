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
            "rival_notification_override_distance: If a rival is within this distance (in blocks), they will be notified regardless of the cooldown."
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
    public String active_bounty = null;
}