/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.data;

/**
 * Represents a tracked waypoint created by the mod.
 */
public class WaypointData {
    public String name;
    public int x;
    public int y;
    public int z;
    public String pokemon_uuid; // null for non-Pokemon waypoints
    public long created_timestamp;
    public WaypointType type;
    
    public enum WaypointType {
        POKEMON,
        GLOBAL_HUNT,
        EVENT
    }
    
    public WaypointData() {}
    
    public WaypointData(String name, int x, int y, int z, String pokemonUuid, WaypointType type) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pokemon_uuid = pokemonUuid;
        this.type = type;
        this.created_timestamp = System.currentTimeMillis();
    }
}