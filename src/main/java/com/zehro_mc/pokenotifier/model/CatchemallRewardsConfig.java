/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the rewards for completing a Pokédex generation in "Catch 'em All" mode.
 * This class is serialized to catchemall_rewards.json.
 */
public class CatchemallRewardsConfig {

    public String[] _instructions = new String[]{
            "Poke Notifier - Catch 'em All Rewards Configuration",
            "Define rewards for completing a Pokédex generation.",
            "The key is the generation ID (e.g., 'gen1', 'gen2').",
            "Each generation has a list of reward items.",
            "  'item': The item's identifier (e.g., 'minecraft:diamond', 'cobblemon:master_ball').",
            "  'count': The amount of the item to give."
    };

    public Map<String, List<RewardItem>> rewards_by_generation = new HashMap<>();

    public static class RewardItem {
        public String item;
        public int count;

        public RewardItem(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public CatchemallRewardsConfig() {
        // Set a default reward for all 9 generations, which admins can customize.
        List<RewardItem> defaultReward = new ArrayList<>(List.of(
                new RewardItem("cobblemon:master_ball", 10)
        ));

        for (int i = 1; i <= 9; i++) {
            rewards_by_generation.put("gen" + i, defaultReward);
        }
    }
}