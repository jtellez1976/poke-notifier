/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the rewards for the automatic bounty system.
 * This class is serialized to events/bounty_rewards.json.
 */
public class BountyRewardsConfig {

    public int config_version = 1;

    public String[] _instructions = new String[]{
            "Poke Notifier - Bounty System Rewards",
            "This file defines the pool of possible rewards for capturing a bounty Pokémon.",
            "When a bounty is claimed, one item will be chosen at random from this list.",
            "  'item': The item's identifier (e.g., 'minecraft:diamond', 'cobblemon:master_ball').",
            "  'count': The amount of the item to give."
    };

    public List<CatchemallRewardsConfig.RewardItem> bounty_reward = new ArrayList<>();

    public BountyRewardsConfig() {
        // Set a default bounty reward pool.
        bounty_reward.addAll(List.of(
                // Valuables
                new CatchemallRewardsConfig.RewardItem("minecraft:diamond_block", 1),
                new CatchemallRewardsConfig.RewardItem("minecraft:nether_star", 1),
                new CatchemallRewardsConfig.RewardItem("minecraft:netherite_upgrade_smithing_template", 1),
                new CatchemallRewardsConfig.RewardItem("minecraft:emerald_block", 8),
                // Cobblemon Items
                new CatchemallRewardsConfig.RewardItem("cobblemon:master_ball", 1),
                new CatchemallRewardsConfig.RewardItem("cobblemon:rare_candy", 5),
                new CatchemallRewardsConfig.RewardItem("cobblemon:park_ball", 3),
                new CatchemallRewardsConfig.RewardItem("cobblemon:xp_candy_xl", 3),
                new CatchemallRewardsConfig.RewardItem("cobblemon:pp_max", 1),
                new CatchemallRewardsConfig.RewardItem("cobblemon:pp_up", 2),
                // Vitamins
                new CatchemallRewardsConfig.RewardItem("cobblemon:hp_up", 3),
                new CatchemallRewardsConfig.RewardItem("cobblemon:protein", 3),
                new CatchemallRewardsConfig.RewardItem("cobblemon:iron", 3),
                new CatchemallRewardsConfig.RewardItem("cobblemon:calcium", 3),
                new CatchemallRewardsConfig.RewardItem("cobblemon:zinc", 3),
                new CatchemallRewardsConfig.RewardItem("cobblemon:carbos", 3)
        ));
    }
}
