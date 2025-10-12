package com.zehro_mc.pokenotifier.util;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.CatchProgressPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

public class PokeNotifierServerUtils {

    /**
     * Envía una actualización del progreso del modo "Catch 'em All" a un jugador.
     * @param player El jugador al que se le enviará la actualización.
     */
    public static void sendCatchProgressUpdate(ServerPlayerEntity player) {
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());
        if (progress.active_generations.isEmpty()) {
            ServerPlayNetworking.send(player, new CatchProgressPayload("none", 0, 0));
        } else {
            String activeGen = progress.active_generations.iterator().next();
            GenerationData genData = ConfigManager.getGenerationData(activeGen);
            int caughtCount = progress.caught_pokemon.getOrDefault(activeGen, Set.of()).size();
            int totalCount = genData != null ? genData.pokemon.size() : 0;
            ServerPlayNetworking.send(player, new CatchProgressPayload(activeGen, caughtCount, totalCount));
        }
    }
}