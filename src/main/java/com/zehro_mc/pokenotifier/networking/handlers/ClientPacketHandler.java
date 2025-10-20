/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all client-side packet processing.
 * Note: Client-side handlers are typically registered in the client module.
 */
public class ClientPacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientPacketHandler.class);
    
    /**
     * Registers client packet receivers.
     * This is a placeholder - actual client handlers are in the client module.
     */
    public static void register() {
        // Client-side packet handlers are registered in the client module
        // This method exists for consistency with the networking architecture
        LOGGER.info("[Networking] Client packet handler registration completed");
    }
}