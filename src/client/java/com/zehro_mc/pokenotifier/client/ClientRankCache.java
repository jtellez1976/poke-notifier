package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.util.PlayerRankUtil;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRankCache {

    private static Map<UUID, Integer> ranks = new ConcurrentHashMap<>();

    public static void updateRanks(Map<UUID, Integer> newRanks) {
        ranks = new ConcurrentHashMap<>(newRanks);
    }

    public static Text getDecoratedName(UUID playerUuid, Text originalName) {
        int rank = ranks.getOrDefault(playerUuid, 0);
        return PlayerRankUtil.getDecoratedPlayerName(rank, originalName);
    }
}