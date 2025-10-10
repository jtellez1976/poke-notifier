package com.zehro_mc.pokenotifier.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PokeNotifierPackets {
    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(CustomListUpdatePayload.ID, CustomListUpdatePayload.CODEC);
    }
    public static void registerS2CPackets() {
        PayloadTypeRegistry.playS2C().register(WaypointPayload.ID, WaypointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatusUpdatePayload.ID, StatusUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDebugStatusPayload.ID, ServerDebugStatusPayload.CODEC);
    }
}