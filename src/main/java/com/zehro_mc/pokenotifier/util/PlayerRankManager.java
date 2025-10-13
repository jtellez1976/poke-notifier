package com.zehro_mc.pokenotifier.util;

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

public class PlayerRankManager {

    // Cache en el servidor para los rangos de los jugadores.
    private static final Map<UUID, Integer> PLAYER_RANKS = new ConcurrentHashMap<>();

    public static void updateAndSyncRank(ServerPlayerEntity player) {
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());
        int completedCount = progress.completed_generations.size();
        PLAYER_RANKS.put(player.getUuid(), completedCount);

        // Sincronizamos el nuevo rango con todos los clientes.
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

        // Efectos de Campeón
        if (getRank(player.getUuid()) >= 9) {
            Text championMessage = Text.literal("A Champion has joined the server!").formatted(Formatting.GOLD);
            PrestigeEffects.playChampionEffects(player);
            // --- CORRECCIÓN: Enviamos el mensaje a todos los jugadores ---
            player.getServer().getPlayerManager().broadcast(championMessage, false);
        }
    }

    public static int getRank(UUID playerUuid) {
        return PLAYER_RANKS.getOrDefault(playerUuid, 0);
    }
}