/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores a player's progress in the "Catch 'em All" mode, including active generations,
 * caught Pokémon, and completed generations. Saved per-player in the catch_progress directory.
 */
public class PlayerCatchProgress {

    /** A flag to track if the initial PC/party sync has been performed for this player. */
    public boolean initialPcSyncCompleted = false;

    /** The generation(s) the player is actively tracking. */
    public Set<String> active_generations = new HashSet<>();
    /** A map storing the set of caught Pokémon for each generation ID (e.g., "gen1"). */
    public Map<String, Set<String>> caught_pokemon = new HashMap<>();
    /** A set of generation IDs that the player has already completed. */
    public Set<String> completed_generations = new HashSet<>();
}