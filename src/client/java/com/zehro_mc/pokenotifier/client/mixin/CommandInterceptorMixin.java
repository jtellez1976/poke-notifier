/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.mixin;

import com.zehro_mc.pokenotifier.client.compat.WaypointTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept chat commands and register waypoints for tracking.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class CommandInterceptorMixin {
    
    @Inject(method = "sendChatCommand", at = @At("HEAD"))
    private void interceptChatCommand(String command, CallbackInfo ci) {
        // Intercept Xaero waypoint commands only when mod is creating them
        if (command.startsWith("xaero_waypoint_add:") && WaypointTracker.isModCreatingWaypoint()) {
            try {
                String[] parts = command.split(":");
                if (parts.length >= 2) {
                    String waypointName = parts[1];
                    // Register waypoint for tracking (only mod-created waypoints)
                    WaypointTracker.registerWaypointByName(waypointName);
                }
            } catch (Exception e) {
                // Ignore errors in waypoint parsing
            }
        }
    }
}