/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration file for tracked waypoints.
 */
public class WaypointConfig {
    public int config_version = 1;
    public List<WaypointData> tracked_waypoints = new ArrayList<>();
}