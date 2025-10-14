/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.networking.WaypointPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Handles the registration of network payloads for the client.
 * This class seems to be a remnant of a previous structure and might be deprecated.
 */
public class PokeNotifierPayloads {
    public static void register() {
        PayloadTypeRegistry.playS2C().register(WaypointPayload.ID, WaypointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatusUpdatePayload.ID, StatusUpdatePayload.CODEC);
    }
}