/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A client-side cache that stores the number of completed generations for each player.
 * This data is received from the server and used to display rank prefixes.
 */
public class ClientRankCache {

    private static Map<UUID, Integer> ranks = new ConcurrentHashMap<>();

    public static void updateRanks(Map<UUID, Integer> newRanks) {
        ranks = new ConcurrentHashMap<>(newRanks);
    }

    public static int getRank(UUID playerUuid) {
        return ranks.getOrDefault(playerUuid, 0);
    }
}