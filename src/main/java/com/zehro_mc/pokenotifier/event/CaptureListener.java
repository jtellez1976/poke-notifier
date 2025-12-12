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
import com.zehro_mc.pokenotifier.ConfigServer;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.model.BountyRewardsConfig;
import com.zehro_mc.pokenotifier.component.ModDataComponents;
import com.zehro_mc.pokenotifier.model.CatchemallRewardsConfig;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.item.ModItems;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.GlobalAnnouncementPayload;
import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import com.zehro_mc.pokenotifier.util.PrestigeEffects;
import com.zehro_mc.pokenotifier.util.PlayerRankManager;
import com.zehro_mc.pokenotifier.globalhunt.GlobalHuntManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Random;

/**
 * Listens for Pokémon capture events to update "Catch 'em All" progress and grant rewards.
 */
public class CaptureListener {

    public static void onPokemonCaptured(PokemonCapturedEvent event) {
        Pokemon pokemon = event.getPokemon();
        ServerPlayerEntity player = event.getPlayer();
        String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();
        
        // Check if this is a Global Hunt Pokemon
        boolean isGlobalHuntPokemon = pokemon.getPersistentData().getBoolean("pokenotifier_global_hunt");
        if (isGlobalHuntPokemon) {
            // Notify Global Hunt Manager of capture
            GlobalHuntManager manager = GlobalHuntManager.getInstance();
            if (manager.hasActiveEvent()) {
                manager.getCurrentEvent().onPokemonCaptured(player.getName().getString());
                PokeNotifier.LOGGER.info("Global Hunt Pokemon {} captured by {}", pokemonName, player.getName().getString());
            }
        }

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
                    PrestigeEffects.launchCelebratoryFireworks(player, progress.completed_generations.size());
                }

                ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);

                // Update and sync the player's rank after each relevant capture.
                PlayerRankManager.updateAndSyncRank(player);

                // --- Rival System Logic ---
                // After a successful capture in Catch 'em All mode, check for online rivals.
                for (ServerPlayerEntity otherPlayer : player.getServer().getPlayerManager().getPlayerList()) {
                    // Skip the player who made the capture.
                    if (otherPlayer.equals(player)) continue;

                    PlayerCatchProgress rivalProgress = ConfigManager.getPlayerCatchProgress(otherPlayer.getUuid());
                    // Check if the other player is a rival (tracking the same generation).
                    if (rivalProgress.active_generations.contains(activeGen)) {
                        // Check if the rival had NOT already caught this Pokémon.
                        boolean rivalHadCaught = rivalProgress.caught_pokemon.getOrDefault(activeGen, java.util.Collections.emptySet()).contains(pokemonName);                        if (!rivalHadCaught) {
                            ConfigServer config = ConfigManager.getServerConfig();
                            long now = System.currentTimeMillis();
                            long lastNotificationTime = PokeNotifier.RIVAL_NOTIFICATION_COOLDOWNS.getOrDefault(otherPlayer.getUuid(), 0L);
                            long cooldownMillis = (long) config.rival_notification_cooldown_seconds * 1000;

                            boolean onCooldown = (now - lastNotificationTime) < cooldownMillis;

                            // Check for proximity override
                            double distanceSq = player.getPos().squaredDistanceTo(otherPlayer.getPos());
                            double overrideDistanceSq = Math.pow(config.rival_notification_override_distance, 2);
                            boolean inProximity = distanceSq <= overrideDistanceSq;

                            // Notify if not on cooldown OR if in proximity (override).
                            if (!onCooldown || inProximity) {
                                // The captor got it first! Send a taunt to the rival.
                                Text message = Text.literal("Your rival, ").formatted(Formatting.RED)
                                        .append(player.getDisplayName())
                                        .append(Text.literal(", just caught a ").formatted(Formatting.RED))
                                        .append(pokemon.getDisplayName(true).copy().formatted(Formatting.GOLD))
                                        .append(Text.literal(" before you! Better hurry!").formatted(Formatting.RED));
                                otherPlayer.sendMessage(message, false);

                                // Update the cooldown timer for the rival.
                                PokeNotifier.RIVAL_NOTIFICATION_COOLDOWNS.put(otherPlayer.getUuid(), now);
                            }
                        }
                    }
                }
            }
        }

        // --- Bounty System Logic ---
        String activeBounty = PokeNotifier.getActiveBounty();
        if (activeBounty != null && activeBounty.equalsIgnoreCase(pokemonName)) {
            // Announce the winner.
            Text bountyMessage = Text.literal("🎉 ").formatted(Formatting.GOLD)
                    .append(player.getDisplayName())
                    .append(Text.literal(" has claimed the bounty by capturing the ").formatted(Formatting.YELLOW))
                    .append(pokemon.getDisplayName(true).copy().formatted(Formatting.GOLD))
                    .append(Text.literal("!").formatted(Formatting.YELLOW));
            player.getServer().getPlayerManager().broadcast(bountyMessage, false);

            // --- MEJORA: Reutilizamos el sistema de anuncios de logros para el ganador ---
            ServerPlayNetworking.send(player, new GlobalAnnouncementPayload(player.getName().getString(), "Bounty Claimed!"));

            // Give the reward.
            BountyRewardsConfig rewardsConfig = ConfigManager.getBountyRewardsConfig();
            List<CatchemallRewardsConfig.RewardItem> rewardPool = rewardsConfig.bounty_reward;
            if (rewardPool != null && !rewardPool.isEmpty()) {
                // --- MEJORA: Pick one random reward from the list ---
                CatchemallRewardsConfig.RewardItem randomReward = rewardPool.get(new Random().nextInt(rewardPool.size()));
                Registries.ITEM.getOrEmpty(Identifier.of(randomReward.item)).ifPresent(item -> 
                    player.getInventory().offerOrDrop(new ItemStack(item, randomReward.count))
                );
            }
            // Clear the bounty so it can't be claimed again.
            PokeNotifier.clearActiveBounty(false);
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
                pokemon.getDisplayName(true).getString(),
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