/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a player's individual configuration, primarily their custom list of tracked Pokémon.
 * This is saved per-player in the player_data directory.
 */
public class CustomListConfig {

    public Set<String> tracked_pokemon = new HashSet<>();

}