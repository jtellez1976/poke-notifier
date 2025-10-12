package com.zehro_mc.pokenotifier.event;

import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.component.ModDataComponents;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.item.ModItems;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.GlobalAnnouncementPayload;
import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

                // --- LÓGICA DE RECOMPENSA (PASOS 10 Y 11) ---
                int caughtCount = progress.caught_pokemon.get(activeGen).size();
                int totalCount = genData.pokemon.size();

                // Comprobamos si la generación está completa y si no ha sido completada antes.
                if (caughtCount >= totalCount && !progress.completed_generations.contains(activeGen)) {
                    progress.completed_generations.add(activeGen); // Marcamos como completada

                    // 1. Anuncio Global
                    String regionName = genData.region.substring(0, 1).toUpperCase() + genData.region.substring(1);
                    GlobalAnnouncementPayload announcement = new GlobalAnnouncementPayload(player.getName().getString(), regionName);
                    player.getServer().getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, announcement));

                    // 2. Recompensa de Trofeo
                    ItemStack trophy = new ItemStack(ModItems.POKEDEX_TROPHY);
                    trophy.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(regionName + " Pokédex Trophy").formatted(Formatting.GOLD));
                    // Usamos el nuevo sistema de Data Components para almacenar la región.
                    trophy.set(ModDataComponents.REGION_NAME, regionName);
                    // Damos el objeto al jugador
                    player.getInventory().offerOrDrop(trophy);
                }

                // Guardamos el progreso después de todas las modificaciones.
                ConfigManager.savePlayerCatchProgress(player.getUuid(), progress); // Guardamos el progreso
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