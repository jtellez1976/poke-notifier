package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.networking.WaypointPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PokeNotifierPayloads {
    public static void register() {
        // Aquí es donde registramos los paquetes que el cliente puede recibir.
        PayloadTypeRegistry.playS2C().register(WaypointPayload.ID, WaypointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatusUpdatePayload.ID, StatusUpdatePayload.CODEC);
    }
}