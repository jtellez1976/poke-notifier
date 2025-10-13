package com.zehro_mc.pokenotifier.util;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayerRankUtil {

    public static Text getDecoratedPlayerName(int completedCount, Text originalName) {
        MutableText prefix = Text.empty();

        if (completedCount == 0) {
            // Rango Trainee por defecto
            prefix.append(Text.literal("⚡[Trainee] ").formatted(Formatting.AQUA));
        } else {
            // Acumula trofeos
            prefix.append(Text.literal("🏆".repeat(completedCount) + " ").formatted(Formatting.GOLD));
        }

        // Para el campeón, además del prefijo, podríamos colorear su nombre
        MutableText playerName = originalName.copy();
        if (completedCount >= 9) {
            playerName.formatted(Formatting.GOLD);
        }

        return prefix.append(playerName);


    }
}