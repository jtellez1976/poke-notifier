/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A utility class for creating decorated player names based on their rank.
 */
public class PlayerRankUtil {

    /**
     * Calculates the Rank and the number of icons to display based on completed generations.
     */
    public static RankInfo getRankInfo(int completedCount) {
        if (completedCount >= 9) return new RankInfo(Rank.MASTER, 1);
        if (completedCount >= 7) return new RankInfo(Rank.VETERAN, completedCount - 6);
        if (completedCount >= 4) return new RankInfo(Rank.EXPERT, completedCount - 3);
        if (completedCount >= 1) return new RankInfo(Rank.GREAT, completedCount);
        return new RankInfo(Rank.TRAINEE, 1);
    }

    /**
     * Generates a decorated player name with text-based symbols for use in chat.
     */
    public static Text getDecoratedPlayerNameForChat(int completedCount, Text originalName) {
        MutableText prefix = Text.empty();

        if (completedCount == 0) {
            // 🛡[Trainee] (Gray)
            prefix.append(Text.literal("🛡[Trainee] ").formatted(Formatting.GRAY)); 
        } else if (completedCount >= 1 && completedCount <= 3) {
            // 🏆 [Great] (Gold cup, gold text)
            prefix.append(Text.literal("🏆 ").formatted(Formatting.GOLD));
            prefix.append(Text.literal("[Great] ").formatted(Formatting.GOLD));
        } else if (completedCount >= 4 && completedCount <= 6) {
            // 🏆🏆 [Expert] (Gold cups, gray text)
            prefix.append(Text.literal("🏆🏆 ").formatted(Formatting.GOLD));
            prefix.append(Text.literal("[Expert] ").formatted(Formatting.GRAY));
        } else if (completedCount >= 7 && completedCount <= 8) {
            // 🏆🏆🏆 [Veteran] (Gold cups, yellow text)
            prefix.append(Text.literal("🏆🏆🏆 ").formatted(Formatting.GOLD));
            prefix.append(Text.literal("[Veteran] ").formatted(Formatting.YELLOW));
        } else { // 9 o más
            // ⚡(green)🏆(gold)⚡(green) [Master](purple)
            prefix.append(Text.literal("⚡").formatted(Formatting.GREEN));
            prefix.append(Text.literal("🏆").formatted(Formatting.GOLD));
            prefix.append(Text.literal("⚡").formatted(Formatting.GREEN));
            prefix.append(Text.literal("[Master] ").formatted(Formatting.LIGHT_PURPLE));
        }

        // For the champion, also color their name.
        MutableText playerName = originalName.copy();
        if (completedCount >= 9) {
            playerName.formatted(Formatting.GOLD);
        }

        return prefix.append(playerName);
    }
}