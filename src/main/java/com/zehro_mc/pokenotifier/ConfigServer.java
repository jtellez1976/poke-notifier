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
            "debug_mode_enabled: If true, the server will print detailed informational logs to the console.",
            "enable_test_mode: If true, the notifier will also trigger for Pokémon spawned with commands (e.g., /pokespawn)."
    };

    public boolean debug_mode_enabled = false;
    public boolean enable_test_mode = false;
}