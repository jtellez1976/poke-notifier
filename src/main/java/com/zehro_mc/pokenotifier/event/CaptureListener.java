package com.zehro_mc.pokenotifier.event;

import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class CaptureListener {

    public static void onPokemonCaptured(PokemonCapturedEvent event) {
        Pokemon pokemon = event.getPokemon();
        RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon);

        // No notificar si la rareza es NONE o UNCOMMON por defecto
        if (rarity == RarityUtil.RarityCategory.NONE || rarity == RarityUtil.RarityCategory.UNCOMMON) {
            return;
        }

        MinecraftServer server = event.getPlayer().getServer();
        if (server == null) return;

        StatusUpdatePayload payload = new StatusUpdatePayload( // Corrected constructor call
                pokemon.getUuid().toString(),
                pokemon.getDisplayName().getString(),
                rarity.name(),
                StatusUpdatePayload.UpdateType.CAPTURED,
                event.getPlayer().getName().getString()
        );

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}