/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.networking.handlers.ClientPacketHandler;
import com.zehro_mc.pokenotifier.networking.handlers.ServerPacketHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central networking management system for Poke Notifier.
 * Handles payload registration and packet handler initialization.
 */
public class NetworkManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkManager.class);
    
    /**
     * Registers all networking components.
     */
    public static void initialize() {
        registerPayloads();
        registerPacketHandlers();
        LOGGER.info("[Networking] Network Manager initialized successfully");
    }
    
    /**
     * Registers all S2C and C2S payloads.
     */
    private static void registerPayloads() {
        // Server to Client payloads
        registerS2CPayloads();
        
        // Client to Server payloads  
        registerC2SPayloads();
        
        LOGGER.info("[Networking] All payloads registered successfully");
    }
    
    /**
     * Registers Server to Client payloads.
     */
    private static void registerS2CPayloads() {
        PayloadTypeRegistry.playS2C().register(StatusUpdatePayload.ID, StatusUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WaypointPayload.ID, WaypointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CatchProgressPayload.ID, CatchProgressPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModeStatusPayload.ID, ModeStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GlobalAnnouncementPayload.ID, GlobalAnnouncementPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RankSyncPayload.ID, RankSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDebugStatusPayload.ID, ServerDebugStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenGuiPayload.ID, OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GuiResponsePayload.ID, GuiResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AdminStatusPayload.ID, AdminStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GlobalHuntPayload.ID, GlobalHuntPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateSourceSyncPayload.ID, UpdateSourceSyncPayload.CODEC);
    }
    
    /**
     * Registers Client to Server payloads.
     */
    private static void registerC2SPayloads() {
        PayloadTypeRegistry.playC2S().register(CustomListUpdatePayload.ID, CustomListUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CatchemallUpdatePayload.ID, CatchemallUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateSourcePayload.ID, UpdateSourcePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminCommandPayload.ID, AdminCommandPayload.PACKET_CODEC);
        PayloadTypeRegistry.playC2S().register(GlobalHuntCommandPayload.ID, GlobalHuntCommandPayload.CODEC);
    }
    
    /**
     * Registers packet handlers for both client and server.
     */
    private static void registerPacketHandlers() {
        ServerPacketHandler.register();
        ClientPacketHandler.register();
        LOGGER.info("[Networking] Packet handlers registered successfully");
    }
}