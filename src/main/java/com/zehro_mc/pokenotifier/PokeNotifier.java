package com.zehro_mc.pokenotifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PokeNotifier implements ModInitializer {
    public static final String MOD_ID = "poke-notifier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Definimos el Identifier aquí para que sea accesible desde otras clases.
    public static final Identifier WAYPOINT_CHANNEL_ID = Identifier.of(MOD_ID, "waypoint_payload");

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Poke Notifier...");

        // Registramos los codecs de nuestros paquetes.
        PayloadTypeRegistry.playS2C().register(WaypointPayload.ID, WaypointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatusUpdatePayload.ID, StatusUpdatePayload.CODEC);

        // =================================================================
        // PASO DE DEPURACIÓN: Reactivamos el registro de eventos.
        // =================================================================
        RarePokemonNotifier.register();
    }
}