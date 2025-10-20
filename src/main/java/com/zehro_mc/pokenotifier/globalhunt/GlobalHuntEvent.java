/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.globalhunt;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.networking.GlobalHuntPayload;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GlobalHuntEvent {
    private final ServerWorld world;
    private final BlockPos coordinates;
    private final String pokemonName;
    private final boolean isShiny;
    private final int durationMinutes;
    
    private PokemonEntity spawnedPokemon;
    private boolean isActive = false;
    private boolean isCaptured = false;
    private ScheduledFuture<?> timeoutTask;
    private ScheduledExecutorService scheduler;
    
    public GlobalHuntEvent(ServerWorld world, BlockPos coordinates, String pokemonName, boolean isShiny, int durationMinutes) {
        this.world = world;
        this.coordinates = coordinates;
        this.pokemonName = pokemonName;
        this.isShiny = isShiny;
        this.durationMinutes = durationMinutes;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void start() {
        if (isActive) return;
        
        isActive = true;
        
        PokeNotifier.LOGGER.info("Starting Global Hunt Event: {} {} at {} for {} minutes", 
            isShiny ? "Shiny" : "", pokemonName, coordinates, durationMinutes);
        
        // Send global notifications first
        sendGlobalNotifications();
        
        // Spawn the Pokemon after notifications
        spawnPokemon();
        
        // Schedule timeout
        timeoutTask = scheduler.schedule(this::timeout, durationMinutes, TimeUnit.MINUTES);
        
        PokeNotifier.LOGGER.info("Global Hunt Event fully started: {} at {}", pokemonName, coordinates);
    }
    
    private void spawnPokemon() {
        try {
            // Force load the chunk and wait
            world.getChunk(coordinates.getX() >> 4, coordinates.getZ() >> 4);
            
            // Use server execute to ensure proper thread
            world.getServer().execute(() -> {
                try {
                    // Create entity using PokemonProperties
                    PokemonProperties props = PokemonProperties.Companion.parse(pokemonName.toLowerCase());
                    if (isShiny) {
                        props.setShiny(true);
                    }
                    
                    spawnedPokemon = props.createEntity(world);
                    spawnedPokemon.setPosition(coordinates.getX() + 0.5, coordinates.getY(), coordinates.getZ() + 0.5);
                    
                    // Add Global Hunt identifier
                    spawnedPokemon.getPokemon().getPersistentData().putBoolean("pokenotifier_global_hunt", true);
                    
                    // Spawn in world
                    boolean spawned = world.spawnEntity(spawnedPokemon);
                    
                    PokeNotifier.LOGGER.info("Global Hunt spawn attempt: {} {} at {} - Success: {} (chunk loaded: {})", 
                        isShiny ? "Shiny" : "", pokemonName, coordinates, spawned,
                        world.isChunkLoaded(coordinates.getX() >> 4, coordinates.getZ() >> 4));
                        
                    if (!spawned) {
                        PokeNotifier.LOGGER.error("Failed to spawn Global Hunt Pokemon - world.spawnEntity returned false");
                    }
                } catch (Exception e) {
                    PokeNotifier.LOGGER.error("Exception during Global Hunt spawn: {}", e.getMessage(), e);
                }
            });
                
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to spawn Pokemon for Global Hunt: {}", e.getMessage(), e);
            cancel();
        }
    }
    
    private void sendGlobalNotifications() {
        // Play thunder sound globally
        world.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.playSoundToPlayer(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 1.0f, 1.0f);
        });
        
        // Send dramatic messages
        String shinyText = isShiny ? "SHINY " : "";
        String worldName = getWorldDisplayName();
        
        Text announcement = Text.literal("⚡ GLOBAL HUNT ALERT! ⚡").formatted(Formatting.RED, Formatting.BOLD)
            .append(Text.literal("\nA " + shinyText + pokemonName + " has been spotted!").formatted(Formatting.GOLD))
            .append(Text.literal("\nLocation: " + coordinates.getX() + ", " + coordinates.getY() + ", " + coordinates.getZ()).formatted(Formatting.YELLOW))
            .append(Text.literal("\nWorld: " + worldName).formatted(Formatting.YELLOW))
            .append(Text.literal("\nTime Limit: " + durationMinutes + " minutes!").formatted(Formatting.GREEN))
            .append(Text.literal("\nFirst to capture wins!").formatted(Formatting.RED, Formatting.BOLD));
        
        world.getServer().getPlayerManager().broadcast(announcement, false);
        
        // Send networking payload to clients for special effects
        GlobalHuntPayload payload = new GlobalHuntPayload(
            GlobalHuntPayload.ACTION_START,
            pokemonName,
            world.getRegistryKey().getValue().toString(),
            coordinates.getX(),
            coordinates.getY(), 
            coordinates.getZ(),
            isShiny,
            durationMinutes
        );
        
        world.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            ServerPlayNetworking.send(player, payload);
        });
    }
    
    private String getWorldDisplayName() {
        String worldKey = world.getRegistryKey().getValue().toString();
        return switch (worldKey) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> worldKey;
        };
    }
    
    public void onPokemonCaptured(String playerName) {
        if (!isActive || isCaptured) return;
        
        isCaptured = true;
        
        // Announce winner
        Text winnerAnnouncement = Text.literal("🏆 GLOBAL HUNT COMPLETE! 🏆").formatted(Formatting.GREEN, Formatting.BOLD)
            .append(Text.literal("\n" + playerName + " captured the " + (isShiny ? "Shiny " : "") + pokemonName + "!").formatted(Formatting.GOLD))
            .append(Text.literal("\nCongratulations to the winner!").formatted(Formatting.YELLOW));
        
        world.getServer().getPlayerManager().broadcast(winnerAnnouncement, false);
        
        // Send completion payload to clients
        GlobalHuntPayload payload = new GlobalHuntPayload(
            GlobalHuntPayload.ACTION_CAPTURED,
            pokemonName,
            world.getRegistryKey().getValue().toString(),
            coordinates.getX(),
            coordinates.getY(),
            coordinates.getZ(),
            isShiny,
            0 // Duration not needed for completion
        );
        
        world.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            ServerPlayNetworking.send(player, payload);
        });
        
        PokeNotifier.LOGGER.info("Global Hunt completed: {} captured by {}", pokemonName, playerName);
        
        // End the event
        cancel();
    }
    
    private void timeout() {
        if (!isActive || isCaptured) return;
        
        // Announce timeout
        Text timeoutAnnouncement = Text.literal("⏰ GLOBAL HUNT EXPIRED! ⏰").formatted(Formatting.RED, Formatting.BOLD)
            .append(Text.literal("\nThe " + (isShiny ? "Shiny " : "") + pokemonName + " has escaped!").formatted(Formatting.YELLOW))
            .append(Text.literal("\nBetter luck next time...").formatted(Formatting.GRAY));
        
        world.getServer().getPlayerManager().broadcast(timeoutAnnouncement, false);
        
        // Send timeout payload to clients
        GlobalHuntPayload payload = new GlobalHuntPayload(
            GlobalHuntPayload.ACTION_TIMEOUT,
            pokemonName,
            world.getRegistryKey().getValue().toString(),
            coordinates.getX(),
            coordinates.getY(),
            coordinates.getZ(),
            isShiny,
            0
        );
        
        world.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            ServerPlayNetworking.send(player, payload);
        });
        
        PokeNotifier.LOGGER.info("Global Hunt timed out: {} escaped", pokemonName);
        
        // Remove the Pokemon
        if (spawnedPokemon != null && !spawnedPokemon.isRemoved()) {
            spawnedPokemon.discard();
        }
        
        cancel();
    }
    
    public void cancel() {
        if (!isActive) return;
        
        isActive = false;
        
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(false);
        }
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        PokeNotifier.LOGGER.info("Global Hunt Event cancelled");
    }
    
    // Getters
    public boolean isActive() { return isActive; }
    public boolean isCaptured() { return isCaptured; }
    public ServerWorld getWorld() { return world; }
    public BlockPos getCoordinates() { return coordinates; }
    public String getPokemonName() { return pokemonName; }
    public boolean isShiny() { return isShiny; }
    public int getDurationMinutes() { return durationMinutes; }
    public PokemonEntity getSpawnedPokemon() { return spawnedPokemon; }
}