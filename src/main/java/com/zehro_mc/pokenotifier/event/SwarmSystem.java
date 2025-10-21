/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.event;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigPokemon;
import com.zehro_mc.pokenotifier.ConfigServer;
import com.zehro_mc.pokenotifier.util.MessageUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages automatic swarm events for rare Pokémon.
 */
public class SwarmSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwarmSystem.class);
    private static final Random SWARM_RANDOM = new Random();
    
    private int swarmTickCounter = 0;
    
    /**
     * Ticks the swarm system, handling automatic swarm creation.
     * @param server The minecraft server instance
     */
    public void tick(MinecraftServer server) {
        ConfigServer config = ConfigManager.getServerConfig();
        if (!config.swarm_system_enabled) {
            return;
        }

        swarmTickCounter++;

        if (swarmTickCounter >= config.swarm_check_interval_minutes * 60 * 20) {
            swarmTickCounter = 0;

            if (SWARM_RANDOM.nextInt(100) < config.swarm_start_chance_percent) {
                startRandomSwarm(server, null);
            }
        }
    }
    
    /**
     * Starts a swarm event with the specified or random Pokémon.
     * @param server The minecraft server instance
     * @param forcedPokemonName Specific Pokémon name, or null for random
     * @return true if swarm was started successfully
     */
    public boolean startSwarm(MinecraftServer server, String forcedPokemonName) {
        startRandomSwarm(server, forcedPokemonName);
        return true;
    }
    
    /**
     * Starts a random swarm event.
     * @param server The minecraft server instance
     * @param forcedPokemonName Specific Pokémon name, or null for random selection
     */
    private void startRandomSwarm(MinecraftServer server, String forcedPokemonName) {
        String pokemonName;
        if (forcedPokemonName != null) {
            pokemonName = forcedPokemonName;
        } else {
            ConfigPokemon pokemonConfig = ConfigManager.getPokemonConfig();
            List<String> swarmPool = new ArrayList<>();
            swarmPool.addAll(pokemonConfig.RARE);
            swarmPool.addAll(pokemonConfig.ULTRA_RARE);

            if (swarmPool.isEmpty()) {
                LOGGER.warn("[Swarm System] No Pokémon available in RARE or ULTRA_RARE lists to create a swarm.");
                return;
            }
            pokemonName = swarmPool.get(SWARM_RANDOM.nextInt(swarmPool.size()));
        }

        // Find a random, valid location
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList().stream()
                .filter(p -> p.getServerWorld().getRegistryKey() == ServerWorld.OVERWORLD).toList();

        if (players.isEmpty()) {
            LOGGER.info("[Swarm System] No players in Overworld, skipping swarm.");
            return;
        }

        ServerPlayerEntity referencePlayer = players.get(SWARM_RANDOM.nextInt(players.size()));
        ServerWorld world = referencePlayer.getServerWorld();

        double angle = SWARM_RANDOM.nextDouble() * 2 * Math.PI;
        double distance = 30 + (SWARM_RANDOM.nextDouble() * 20); // 30-50 blocks away

        int x = (int) (referencePlayer.getX() + Math.cos(angle) * distance);
        int z = (int) (referencePlayer.getZ() + Math.sin(angle) * distance);
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);

        BlockPos swarmPos = new BlockPos(x, y, z);

        // Execute the outbreak at the found location
        try {
            PokemonProperties props = PokemonProperties.Companion.parse(pokemonName);
            Pokemon pokemon = props.create();
            String capitalizedName = pokemon.getSpecies().getName();
            String biomeName = world.getBiome(swarmPos).getKey().map(key -> key.getValue().getPath()).orElse("unknown").replace("_", " ");

            Text message = Text.literal("🌊 Swarm Alert! ").formatted(Formatting.AQUA)
                    .append(Text.literal("A large concentration of ").formatted(Formatting.YELLOW))
                    .append(Text.literal(capitalizedName).formatted(Formatting.GOLD))
                    .append(Text.literal(" has been detected in " + biomeName + " biomes at ").formatted(Formatting.YELLOW))
                    .append(MessageUtils.createLocationText(capitalizedName + " Swarm", swarmPos, MessageUtils.Colors.SWARM));
            
            // Add distance info for nearby players
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                double playerDistance = player.getPos().distanceTo(swarmPos.toCenterPos());
                if (playerDistance <= 100) { // Within 100 blocks
                    Text personalMessage = message.copy()
                            .append(Text.literal(" (" + String.format("%.0f", playerDistance) + " blocks away)").formatted(Formatting.GREEN));
                    player.sendMessage(personalMessage, false);
                } else {
                    player.sendMessage(message, false);
                }
            }

            // Message is sent individually above
            server.getPlayerManager().getPlayerList().forEach(p -> p.playSoundToPlayer(SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.NEUTRAL, 1.0F, 1.0F));

            // Spawn a random amount of Pokémon between 10 and 15
            int swarmSize = 10 + SWARM_RANDOM.nextInt(6);
            for (int i = 0; i < swarmSize; i++) {
                PokemonEntity entity = props.createEntity(world);
                entity.teleport(swarmPos.getX() + (Math.random() - 0.5) * 20, swarmPos.getY(), swarmPos.getZ() + (Math.random() - 0.5) * 20, true);
                world.spawnEntity(entity);
            }
            LOGGER.info("[Swarm System] Started a swarm of {} at {}", pokemonName, swarmPos);
        } catch (Exception e) {
            LOGGER.error("Failed to start swarm for " + pokemonName, e);
        }
    }
}