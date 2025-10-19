/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.CatchProgressPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

/**
 * A utility class for server-side helper methods.
 */
public class PokeNotifierServerUtils {

    /**
     * Sends a "Catch 'em All" progress update to a specific player.
     * @param player The player to send the update to.
     */
    public static void sendCatchProgressUpdate(ServerPlayerEntity player) {
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());
        if (progress.active_generations.isEmpty()) {
            // FIX: Provide all 4 required arguments to the constructor.
            int customHuntListSize = ConfigManager.getPlayerConfig(player.getUuid()).tracked_pokemon.size();
            ServerPlayNetworking.send(player, new CatchProgressPayload("none", 0, 0, customHuntListSize));
        } else {
            String activeGen = progress.active_generations.iterator().next();
            GenerationData genData = ConfigManager.getGenerationData(activeGen);
            int caughtCount = progress.caught_pokemon.getOrDefault(activeGen, Set.of()).size();
            int totalCount = genData != null ? genData.pokemon.size() : 0;
            int customHuntListSize = ConfigManager.getPlayerConfig(player.getUuid()).tracked_pokemon.size();

            ServerPlayNetworking.send(player, new CatchProgressPayload(
                    activeGen,
                    caughtCount,
                    totalCount,
                    customHuntListSize));
        }
    }
}