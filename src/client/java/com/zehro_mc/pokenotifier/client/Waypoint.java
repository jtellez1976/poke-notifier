/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import net.minecraft.util.math.BlockPos;

/**
 * Represents a temporary waypoint for a spawned Pokémon.
 * This class seems to be unused in favor of directly managing waypoints in PokeNotifierClient.
 */
public class Waypoint {
    private final String id;
    private final String name;
    private final BlockPos pos;
    private final int color;

    public Waypoint(String id, String name, BlockPos pos, int color) {
        this.id = id;
        this.name = name;
        this.pos = pos;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getColor() {
        return color;
    }
}