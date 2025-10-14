/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier;

/**
 * Defines all client-side configuration options.
 * These settings are managed by each player individually.
 */
public class ConfigClient {

    public String[] _instructions = new String[]{
            "Poke Notifier Configuration - Client Settings",
            "This file defines client-side preferences and general mod settings.",
            "notification_distance: The maximum distance in blocks a player can be from a Pokémon to receive a notification."
    };

    public int notification_distance = 200;
    public int glowing_duration_seconds = 120;

    public boolean alert_sounds_enabled = true;
    public boolean alert_toast_enabled = true;
    public boolean alert_chat_enabled = true;
    public boolean silent_mode_enabled = false;
    public boolean searching_enabled = true;
}