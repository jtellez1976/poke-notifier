/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.RankSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player ranks based on their "Catch 'em All" progress.
 * It caches rank data and synchronizes it with clients.
 */
public class PlayerRankManager {

    // Server-side cache for player ranks (number of completed generations).
    private static final Map<UUID, Integer> PLAYER_RANKS = new ConcurrentHashMap<>();

    public static void updateAndSyncRank(ServerPlayerEntity player) {
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());
        int completedCount = progress.completed_generations.size();
        PLAYER_RANKS.put(player.getUuid(), completedCount);

        syncRanksToAll(player.getServer());
    }

    public static void syncRanksToAll(MinecraftServer server) {
        if (server == null) return;
        HashMap<UUID, Integer> ranksToSend = new HashMap<>(PLAYER_RANKS);
        RankSyncPayload payload = new RankSyncPayload(ranksToSend);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        updateAndSyncRank(player);

        // Schedule Master rank effects to ensure they trigger after the player has fully loaded.
        if (getRank(player.getUuid()) >= 9) {
            PokeNotifier.scheduleTask(() -> {
                PrestigeEffects.playMasterEffects(player);
            });
        }
    }    

    public static int getRank(UUID playerUuid) {
        return PLAYER_RANKS.getOrDefault(playerUuid, 0);
    }
}
