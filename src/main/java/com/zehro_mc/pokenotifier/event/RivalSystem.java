/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages rival notification cooldowns and interactions.
 */
public class RivalSystem {
    
    // Rival System Cooldowns
    private static final Map<UUID, Long> RIVAL_NOTIFICATION_COOLDOWNS = new ConcurrentHashMap<>();
    
    /**
     * Checks if a player is on cooldown for rival notifications.
     * @param playerUuid The player's UUID
     * @return true if player is on cooldown
     */
    public static boolean isOnCooldown(UUID playerUuid) {
        Long lastNotification = RIVAL_NOTIFICATION_COOLDOWNS.get(playerUuid);
        if (lastNotification == null) {
            return false;
        }
        
        long cooldownTime = 30000; // 30 seconds cooldown
        return System.currentTimeMillis() - lastNotification < cooldownTime;
    }
    
    /**
     * Sets a cooldown for rival notifications for the specified player.
     * @param playerUuid The player's UUID
     */
    public static void setCooldown(UUID playerUuid) {
        RIVAL_NOTIFICATION_COOLDOWNS.put(playerUuid, System.currentTimeMillis());
    }
    
    /**
     * Clears the cooldown for the specified player.
     * @param playerUuid The player's UUID
     */
    public static void clearCooldown(UUID playerUuid) {
        RIVAL_NOTIFICATION_COOLDOWNS.remove(playerUuid);
    }
    
    /**
     * Clears all rival cooldowns.
     */
    public static void clearAllCooldowns() {
        RIVAL_NOTIFICATION_COOLDOWNS.clear();
    }
    
    /**
     * Gets the remaining cooldown time for a player in milliseconds.
     * @param playerUuid The player's UUID
     * @return Remaining cooldown time, or 0 if no cooldown
     */
    public static long getRemainingCooldown(UUID playerUuid) {
        Long lastNotification = RIVAL_NOTIFICATION_COOLDOWNS.get(playerUuid);
        if (lastNotification == null) {
            return 0;
        }
        
        long cooldownTime = 30000; // 30 seconds cooldown
        long elapsed = System.currentTimeMillis() - lastNotification;
        return Math.max(0, cooldownTime - elapsed);
    }
    
    public static long getLastNotificationTime(UUID playerUuid) {
        return RIVAL_NOTIFICATION_COOLDOWNS.getOrDefault(playerUuid, 0L);
    }
    
    public static void updateNotificationTime(UUID playerUuid, long time) {
        RIVAL_NOTIFICATION_COOLDOWNS.put(playerUuid, time);
    }
}