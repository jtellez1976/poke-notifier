/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.util;

import com.zehro_mc.pokenotifier.PokeNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects whether the mod is installed on the server.
 * Used to determine if client should run in client-only mode or full mode.
 */
public class ServerModDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokeNotifier.MOD_ID + "-Detector");
    
    private static boolean serverHasMod = false;
    private static boolean detectionComplete = false;
    private static long detectionStartTime = 0;
    private static final long DETECTION_TIMEOUT = 3000; // 3 seconds
    
    /**
     * Starts the detection process when joining a server.
     */
    public static void startDetection() {
        // Check if this is singleplayer/integrated server
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.isInSingleplayer() || (client.getServer() != null && client.getServer().isHost(client.player.getGameProfile()))) {
            // Singleplayer or LAN - mod is always installed
            serverHasMod = true;
            detectionComplete = true;
            LOGGER.info("Singleplayer/Integrated server detected - Running in FULL MODE");
            return;
        }
        
        // External server - start detection
        serverHasMod = false;
        detectionComplete = false;
        detectionStartTime = System.currentTimeMillis();
        LOGGER.info("Starting server mod detection for external server...");
    }
    
    /**
     * Called when server responds to handshake.
     */
    public static void onServerResponse() {
        serverHasMod = true;
        detectionComplete = true;
        LOGGER.info("Server has Poke Notifier installed - Running in FULL MODE");
    }
    
    /**
     * Checks if detection has timed out (server doesn't have mod).
     */
    public static void checkTimeout() {
        if (!detectionComplete && detectionStartTime > 0) {
            long elapsed = System.currentTimeMillis() - detectionStartTime;
            if (elapsed >= DETECTION_TIMEOUT) {
                detectionComplete = true;
                serverHasMod = false;
                LOGGER.info("Server detection timeout - Running in CLIENT-ONLY MODE");
            }
        }
    }
    
    /**
     * Resets detection state when disconnecting.
     */
    public static void reset() {
        serverHasMod = false;
        detectionComplete = false;
        detectionStartTime = 0;
        LOGGER.info("Server mod detection reset");
    }
    
    /**
     * @return true if running in client-only mode (server doesn't have mod)
     */
    public static boolean isClientOnly() {
        return detectionComplete && !serverHasMod;
    }
    
    /**
     * @return true if running in full mode (server has mod)
     */
    public static boolean isFullMode() {
        return detectionComplete && serverHasMod;
    }
    
    /**
     * @return true if detection is still in progress
     */
    public static boolean isDetecting() {
        return !detectionComplete;
    }
    
    /**
     * @return current mode as string for display
     */
    public static String getModeString() {
        if (!detectionComplete) {
            return "Detecting...";
        }
        return serverHasMod ? "Full Mode" : "Client-Only Mode";
    }
}
