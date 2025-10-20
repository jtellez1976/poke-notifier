/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.data;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.util.PokeNotifierServerUtils;
import com.zehro_mc.pokenotifier.util.PlayerRankManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Centralized data management system for Poke Notifier.
 * Handles player data operations, PC synchronization, and backup management.
 */
public class DataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataManager.class);
    
    /**
     * Performs the initial sync of a player's PC and party with their "Catch 'em All" progress.
     * This operation only runs once per player.
     * @param player The player to sync.
     */
    public static void performInitialPcSync(ServerPlayerEntity player) {
        var progress = ConfigManager.getPlayerCatchProgress(player.getUuid());

        if (progress.initialPcSyncCompleted) {
            return;
        }

        LOGGER.info("Performing initial PC sync for player: " + player.getName().getString());

        PokemonStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PokemonStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);

        Stream<Pokemon> partyStream = StreamSupport.stream(party.spliterator(), false);
        Stream<Pokemon> pcStream = StreamSupport.stream(pc.spliterator(), false);
        Stream.concat(partyStream, pcStream).forEach(pokemon -> {
            if (pokemon == null) return;
            String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();

            for (int i = 1; i <= 9; i++) {
                String genId = "gen" + i;
                GenerationData genData = ConfigManager.getGenerationData(genId);
                if (genData != null && genData.pokemon.contains(pokemonName)) {
                    progress.caught_pokemon.computeIfAbsent(genId, k -> new HashSet<>()).add(pokemonName);
                }
            }
        });

        progress.initialPcSyncCompleted = true;
        ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);

        player.sendMessage(Text.literal("[Poke Notifier] Your Pokédex has been synchronized with the 'Catch 'em All' mode!").formatted(Formatting.GREEN), false);
        LOGGER.info("Initial PC sync completed for player: " + player.getName().getString());
    }

    /**
     * Autocompletes a generation for a player, leaving one Pokémon uncaught for testing.
     * @param player The target player
     * @param genName The generation to autocomplete
     * @param genData The generation data
     * @return The name of the Pokémon left uncaught
     */
    public static String autocompleteGenerationForPlayer(ServerPlayerEntity player, String genName, GenerationData genData) {
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());

        Set<String> allPokemonForGen = new HashSet<>(genData.pokemon);
        if (allPokemonForGen.isEmpty()) {
            return "none";
        }

        // Find the difference between all Pokémon and the ones the player has caught
        allPokemonForGen.removeAll(progress.caught_pokemon.getOrDefault(genName, new HashSet<>()));

        // The remaining Pokémon is the one to leave out
        String lastPokemon = allPokemonForGen.stream().findFirst().orElse("mew"); // Fallback

        Set<String> completedList = new HashSet<>(genData.pokemon);
        completedList.remove(lastPokemon);
        progress.caught_pokemon.put(genName, completedList);
        ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);

        player.sendMessage(Text.literal("An administrator has autocompleted your " + formatGenName(genName) + " progress for testing purposes.").formatted(Formatting.YELLOW), false);
        PokeNotifierServerUtils.sendCatchProgressUpdate(player);
        return lastPokemon;
    }

    /**
     * Creates a backup of player progress and restores from backup if available.
     * @param player The target player
     * @return true if rollback was successful, false if no backup exists
     */
    public static boolean rollbackPlayerProgress(ServerPlayerEntity player) {
        File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json");
        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json.bak");

        if (!backupFile.exists()) {
            return false;
        }

        try {
            Files.move(backupFile.toPath(), progressFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // Clear the player's progress from the cache
            ConfigManager.forceReloadPlayerCatchProgress(player.getUuid());
            PlayerRankManager.updateAndSyncRank(player);

            // Force a progress update to the client to refresh the HUD immediately
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);

            player.getInventory().remove(stack -> stack.getItem() instanceof com.zehro_mc.pokenotifier.item.PokedexTrophyItem, -1, player.getInventory());
            player.sendMessage(Text.literal("Your Pokédex Trophies have been removed as part of the rollback.").formatted(Formatting.YELLOW), false);

            player.sendMessage(Text.literal("Your 'Catch 'em All' progress has been restored by an administrator.").formatted(Formatting.GREEN), false);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to rollback progress for player " + player.getName().getString(), e);
            return false;
        }
    }

    /**
     * Creates a backup of player progress before making changes.
     * @param player The target player
     * @return true if backup was created successfully
     */
    public static boolean createPlayerBackup(ServerPlayerEntity player) {
        File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json");
        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json.bak");
        
        try {
            if (progressFile.exists() && !backupFile.exists()) {
                Files.copy(progressFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Backup created for player: " + player.getName().getString());
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create backup for " + player.getName().getString(), e);
        }
        return false;
    }

    private static String formatGenName(String genName) {
        if (genName == null || !genName.toLowerCase().startsWith("gen")) return genName;
        return "Gen" + genName.substring(3);
    }
}