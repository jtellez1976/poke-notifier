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

    public static int getRank(UUID playerUuid) {
        return ranks.getOrDefault(playerUuid, 0);
    }
}