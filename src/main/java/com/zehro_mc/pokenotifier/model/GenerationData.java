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
 * Represents the data for a single Pokémon generation, including its region name
 * and the set of Pokémon belonging to it. Loaded from resource JSON files.
 */
public class GenerationData {
    public String region = "Unknown";
    public Set<String> pokemon = new HashSet<>();
}