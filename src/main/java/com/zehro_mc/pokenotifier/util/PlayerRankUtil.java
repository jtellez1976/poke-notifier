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
import net.minecraft.util.Identifier;

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
        
        // Custom font characters that map to our rank_icons.png
        final String TRAINEE_ICON = "\uE001"; // Corresponds to cup0.png
        final String GREAT_ICON = "\uE002";   // Corresponds to cup1.png
        final String EXPERT_ICON = "\uE003";  // Corresponds to cup2.png
        final String VETERAN_ICON = "\uE004"; // Corresponds to cup3.png
        final String MASTER_ICON = "\uE005";  // Corresponds to cup4_master.png
        
        // --- CORRECCIÓN: Definimos el identificador de nuestra fuente personalizada ---
        final Identifier RANK_FONT = Identifier.of("poke-notifier", "ranks");

        if (completedCount == 0) {
            prefix.append(Text.literal(TRAINEE_ICON).styled(style -> style.withFont(RANK_FONT)));
            prefix.append(Text.literal(" [Trainee] ").formatted(Formatting.GRAY));
        } else if (completedCount >= 1 && completedCount <= 3) {
            prefix.append(Text.literal(GREAT_ICON.repeat(completedCount)).styled(style -> style.withFont(RANK_FONT))).append(Text.literal(" "));
            prefix.append(Text.literal("[Great] ").formatted(Formatting.GOLD));
        } else if (completedCount >= 4 && completedCount <= 6) {
            prefix.append(Text.literal(EXPERT_ICON.repeat(completedCount - 3)).styled(style -> style.withFont(RANK_FONT))).append(Text.literal(" "));
            prefix.append(Text.literal("[Expert] ").formatted(Formatting.AQUA));
        } else if (completedCount >= 7 && completedCount <= 8) {
            prefix.append(Text.literal(VETERAN_ICON.repeat(completedCount - 6)).styled(style -> style.withFont(RANK_FONT))).append(Text.literal(" "));
            prefix.append(Text.literal("[Veteran] ").formatted(Formatting.YELLOW));
        } else { // 9 o más
            // For the Master rank, the icon texture has its own colors, so we don't apply a format color to it.
            prefix.append(Text.literal(MASTER_ICON).styled(style -> style.withFont(RANK_FONT))).append(Text.literal(" "));
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