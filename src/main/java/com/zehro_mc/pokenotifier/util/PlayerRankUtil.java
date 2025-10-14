package com.zehro_mc.pokenotifier.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayerRankUtil {

    /**
     * Calcula el Rank y el número de iconos a mostrar.
     */
    public static RankInfo getRankInfo(int completedCount) {
        if (completedCount >= 9) return new RankInfo(Rank.MASTER, 1);
        if (completedCount >= 7) return new RankInfo(Rank.VETERAN, completedCount - 6);
        if (completedCount >= 4) return new RankInfo(Rank.EXPERT, completedCount - 3);
        if (completedCount >= 1) return new RankInfo(Rank.GREAT, completedCount);
        return new RankInfo(Rank.TRAINEE, 1);
    }

    /**
     * Genera el nombre decorado solo con texto para el chat.
     */
    public static Text getDecoratedPlayerNameForChat(int completedCount, Text originalName) {
        MutableText prefix = Text.empty();

        if (completedCount == 0) {
            // 🛡[Trainee] (Gris)
            prefix.append(Text.literal("🛡[Trainee] ").formatted(Formatting.GRAY)); 
        } else if (completedCount >= 1 && completedCount <= 3) {
            // 🏆 [Great] (Copa dorada, texto bronce)
            prefix.append(Text.literal("🏆 ").formatted(Formatting.GOLD));
            prefix.append(Text.literal("[Great] ").formatted(Formatting.GOLD)); // Usamos GOLD como el color "bronce"
        } else if (completedCount >= 4 && completedCount <= 6) {
            // 🏆🏆 [Expert] (Copas doradas, texto plata)
            prefix.append(Text.literal("🏆🏆 ").formatted(Formatting.GOLD));
            prefix.append(Text.literal("[Expert] ").formatted(Formatting.GRAY)); // GRAY es el color "plata"
        } else if (completedCount >= 7 && completedCount <= 8) {
            // 🏆🏆🏆 [Veteran] (Copas doradas, texto amarillo)
            prefix.append(Text.literal("🏆🏆🏆 ").formatted(Formatting.GOLD));
            prefix.append(Text.literal("[Veteran] ").formatted(Formatting.YELLOW));
        } else { // 9 o más
            // ⚡(verde)🏆(dorado)⚡(verde) [Master](morado)
            prefix.append(Text.literal("⚡").formatted(Formatting.GREEN));
            prefix.append(Text.literal("🏆").formatted(Formatting.GOLD));
            prefix.append(Text.literal("⚡").formatted(Formatting.GREEN));
            prefix.append(Text.literal("[Master] ").formatted(Formatting.LIGHT_PURPLE));
        }

        // Para el campeón, coloreamos su nombre.
        MutableText playerName = originalName.copy();
        if (completedCount >= 9) {
            playerName.formatted(Formatting.GOLD);
        }

        return prefix.append(playerName);
    }
}