package com.zehro_mc.pokenotifier.event;

import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class CaptureListener {

    public static void onPokemonCaptured(PokemonCapturedEvent event) {
        Pokemon pokemon = event.getPokemon();
        ServerPlayerEntity player = event.getPlayer();
        String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();

        // --- LÓGICA CATCH 'EM ALL ---
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());
        if (!progress.active_generations.isEmpty()) {
            String activeGen = progress.active_generations.iterator().next();
            GenerationData genData = ConfigManager.getGenerationData(activeGen);

            // Si el Pokémon capturado pertenece a la generación activa...
            if (genData != null && genData.pokemon.contains(pokemonName)) {
                // ...lo añadimos a la lista de capturados de esa generación.
                progress.caught_pokemon.computeIfAbsent(activeGen, k -> new java.util.HashSet<>()).add(pokemonName);
                ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
            }
        }

        // --- LÓGICA DE NOTIFICACIÓN DE CAPTURA (existente) ---
        RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon, player);

        // No notificar si la rareza es COMMON por defecto
        if (rarity == RarityUtil.RarityCategory.COMMON) {
            return;
        }

        MinecraftServer server = event.getPlayer().getServer();
        if (server == null) return;

        StatusUpdatePayload payload = new StatusUpdatePayload( // Corrected constructor call
                pokemon.getUuid().toString(),
                pokemon.getDisplayName().getString(),
                rarity.name(),
                StatusUpdatePayload.UpdateType.CAPTURED,
                player.getName().getString()
        );

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, payload);
        }
    }
}