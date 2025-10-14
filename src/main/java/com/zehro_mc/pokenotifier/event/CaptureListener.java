/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.event;

import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.component.ModDataComponents;
import com.zehro_mc.pokenotifier.model.CatchemallRewardsConfig;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.item.ModItems;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.GlobalAnnouncementPayload;
import com.zehro_mc.pokenotifier.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import com.zehro_mc.pokenotifier.util.PrestigeEffects;
import com.zehro_mc.pokenotifier.util.PlayerRankManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Listens for Pokémon capture events to update "Catch 'em All" progress and grant rewards.
 */
public class CaptureListener {

    public static void onPokemonCaptured(PokemonCapturedEvent event) {
        Pokemon pokemon = event.getPokemon();
        ServerPlayerEntity player = event.getPlayer();
        String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();

        // "Catch 'em All" logic.
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());
        if (!progress.active_generations.isEmpty()) {
            String activeGen = progress.active_generations.iterator().next();
            GenerationData genData = ConfigManager.getGenerationData(activeGen);

            // If the captured Pokémon belongs to the active generation...
            if (genData != null && genData.pokemon.contains(pokemonName)) {
                // ...add it to the caught list for that generation.
                progress.caught_pokemon.computeIfAbsent(activeGen, k -> new java.util.HashSet<>()).add(pokemonName);

                int caughtCount = progress.caught_pokemon.get(activeGen).size();
                int totalCount = genData.pokemon.size();

                // Check if the generation is now complete and hasn't been rewarded before.
                if (caughtCount >= totalCount && !progress.completed_generations.contains(activeGen)) {
                    progress.completed_generations.add(activeGen);

                    // 1. Global Announcement
                    String regionName = genData.region.substring(0, 1).toUpperCase() + genData.region.substring(1);
                    GlobalAnnouncementPayload announcement = new GlobalAnnouncementPayload(player.getName().getString(), regionName);
                    player.getServer().getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, announcement));

                    // Special announcement for completing all 9 generations.
                    if (progress.completed_generations.size() >= 9) {
                        Text masterMessage = Text.literal(player.getName().getString()).formatted(Formatting.GOLD, Formatting.BOLD)
                                .append(Text.literal(" has achieved the impossible! They have completed all Pokédex challenges and become a Pokémon Master!").formatted(Formatting.LIGHT_PURPLE));
                        MinecraftServer server = player.getServer();
                        if (server != null) {
                            server.getPlayerManager().broadcast(masterMessage, false);
                        }
                        PrestigeEffects.playMasterAchievementEffects(player);
                    }

                    // 2. Regional Trophy Reward
                    ItemStack trophy = getTrophyForRegion(regionName);
                    if (!trophy.isEmpty()) {
                        trophy.set(ModDataComponents.OWNER_NAME, player.getName().getString());
                        trophy.set(ModDataComponents.OWNER_UUID, player.getUuid().toString());
                        player.getInventory().offerOrDrop(trophy);
                    }

                    // 3. Additional Configurable Rewards
                    CatchemallRewardsConfig rewardsConfig = ConfigManager.getCatchemallRewardsConfig();
                    List<CatchemallRewardsConfig.RewardItem> rewards = rewardsConfig.rewards_by_generation.get(activeGen);

                    if (rewards != null) {
                        for (CatchemallRewardsConfig.RewardItem reward : rewards) {
                            Registries.ITEM.getOrEmpty(Identifier.of(reward.item)).ifPresent(item -> {
                                ItemStack rewardStack = new ItemStack(item, reward.count);
                                player.getInventory().offerOrDrop(rewardStack);
                            });
                        }
                    }
                    // 4. Launch celebratory fireworks.
                    PrestigeEffects.launchCelebratoryFireworks(player);
                }

                ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);

                // Update and sync the player's rank after each relevant capture.
                PlayerRankManager.updateAndSyncRank(player);
            }
        }

        // Standard capture notification logic.
        RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon, player);

        if (rarity == RarityUtil.RarityCategory.COMMON) {
            return;
        }

        MinecraftServer server = event.getPlayer().getServer();
        if (server == null) return;

        StatusUpdatePayload payload = new StatusUpdatePayload(
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

    private static ItemStack getTrophyForRegion(String regionName) {
        return switch (regionName.toLowerCase()) {
            case "kanto" -> new ItemStack(ModItems.KANTO_TROPHY);
            case "johto" -> new ItemStack(ModItems.JOHTO_TROPHY);
            case "hoenn" -> new ItemStack(ModItems.HOENN_TROPHY);
            case "sinnoh" -> new ItemStack(ModItems.SINNOH_TROPHY);
            case "unova" -> new ItemStack(ModItems.UNOVA_TROPHY);
            case "kalos" -> new ItemStack(ModItems.KALOS_TROPHY);
            case "alola" -> new ItemStack(ModItems.ALOLA_TROPHY);
            case "galar" -> new ItemStack(ModItems.GALAR_TROPHY);
            case "paldea" -> new ItemStack(ModItems.PALDEA_TROPHY);
            default -> ItemStack.EMPTY;
        };
    }
}