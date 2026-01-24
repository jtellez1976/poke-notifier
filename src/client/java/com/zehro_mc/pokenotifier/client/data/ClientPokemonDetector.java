/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.data;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigPokemon;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.client.compat.XaeroIntegration;
import com.zehro_mc.pokenotifier.util.MessageUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Client-side detector for nearby wild Pokemon in client-only mode.
 * Replicates full server-side functionality including waypoints, status, and coordinates.
 */
public class ClientPokemonDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokeNotifier.MOD_ID + "-Detector");
    private static final Set<UUID> notifiedPokemon = new HashSet<>();
    private static final Map<UUID, String> pokemonWaypoints = new HashMap<>();
    private static final Map<String, Long> speciesCooldowns = new HashMap<>(); // Species cooldown tracking
    private static final long COOLDOWN_MS = 5000; // 5 seconds cooldown per species
    private static int scanTick = 0;
    private static int initialDelayTicks = 100; // 5 seconds delay
    
    public enum RarityCategory {
        CUSTOM(Formatting.LIGHT_PURPLE, 0xAA00FF),
        SHINY(Formatting.YELLOW, 0xFFFF00),
        LEGENDARY(Formatting.GOLD, 0xFFD700),
        MYTHICAL(Formatting.AQUA, 0x00FFFF),
        ULTRA_BEAST(Formatting.BLUE, 0x0000FF),
        PARADOX(Formatting.LIGHT_PURPLE, 0xAA00FF),
        ULTRA_RARE(Formatting.RED, 0xFF0000),
        RARE(Formatting.YELLOW, 0xFFFF00),
        HUNT(Formatting.GREEN, 0x00FF00),
        COMMON(Formatting.WHITE, 0xFFFFFF);
        
        private final Formatting color;
        private final int waypointColor;
        
        RarityCategory(Formatting color, int waypointColor) {
            this.color = color;
            this.waypointColor = waypointColor;
        }
        
        public Formatting getColor() {
            return color;
        }
        
        public int getWaypointColor() {
            return waypointColor;
        }
    }
    
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            
            // Initial delay before starting detection
            if (initialDelayTicks > 0) {
                initialDelayTicks--;
                return;
            }
            
            ConfigClient config = ConfigManager.getClientConfig();
            if (!config.searching_enabled) return;
            
            // Scan every 20 ticks (1 second)
            scanTick++;
            if (scanTick >= 20) {
                scanTick = 0;
                scanNearbyPokemon(client);
                cleanupWaypoints(client);
            }
        });
        
        // Listen for captures to remove waypoints
        com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_CAPTURED.subscribe(com.cobblemon.mod.common.api.Priority.NORMAL, event -> {
            String speciesName = event.getPokemon().getSpecies().getResourceIdentifier().getPath();
            removeWaypointForSpecies(speciesName);
            return kotlin.Unit.INSTANCE;
        });
    }
    
    private static void scanNearbyPokemon(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        
        ConfigClient config = ConfigManager.getClientConfig();
        if (!config.searching_enabled) return;
        
        List<String> huntList = ClientOnlyDataManager.getCustomHuntList();
        ConfigPokemon pokemonConfig = ConfigManager.getPokemonConfig();
        String activeGen = ClientOnlyDataManager.getActiveGeneration();
        
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PokemonEntity pokemonEntity)) continue;
            if (notifiedPokemon.contains(entity.getUuid())) continue;
            if (pokemonEntity.getOwnerUuid() != null) continue;
            
            double distance = Math.sqrt(client.player.squaredDistanceTo(entity));
            if (distance > config.notification_distance) continue;
            
            Pokemon pokemon = pokemonEntity.getPokemon();
            String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();
            
            RarityCategory rarity = getRarity(pokemon, pokemonName, huntList, pokemonConfig);
            
            // Check Catch 'em All (only if not already a higher priority)
            if (rarity == RarityCategory.COMMON && !"none".equals(activeGen)) {
                if (isInActiveGeneration(pokemonName, activeGen) && !ClientOnlyDataManager.hasCaught(activeGen, pokemonName)) {
                    rarity = RarityCategory.HUNT;
                }
            }
            
            if (rarity != RarityCategory.COMMON) {
                String status = hasCaughtSpecies(pokemon) ? "CAUGHT" : "NEW";
                
                // Skip CAUGHT notifications for Catch 'em All to reduce spam
                if (rarity == RarityCategory.HUNT && "CAUGHT".equals(status)) {
                    notifiedPokemon.add(entity.getUuid());
                    continue;
                }
                
                // Check species cooldown ONLY for non-priority Pokemon
                // Priority Pokemon (SHINY, LEGENDARY, MYTHICAL, CUSTOM) always notify immediately
                boolean isPriority = rarity == RarityCategory.SHINY || 
                                    rarity == RarityCategory.LEGENDARY || 
                                    rarity == RarityCategory.MYTHICAL || 
                                    rarity == RarityCategory.CUSTOM;
                
                if (!isPriority) {
                    long currentTime = System.currentTimeMillis();
                    Long lastNotified = speciesCooldowns.get(pokemonName);
                    if (lastNotified != null && (currentTime - lastNotified) < COOLDOWN_MS) {
                        notifiedPokemon.add(entity.getUuid());
                        continue; // Skip notification, still in cooldown
                    }
                    speciesCooldowns.put(pokemonName, currentTime);
                }
                
                notifiedPokemon.add(entity.getUuid());
                sendFullNotification(pokemonEntity, pokemon, rarity, distance, status, activeGen);
            }
        }
        
        if (notifiedPokemon.size() > 100) {
            notifiedPokemon.clear();
        }
    }
    
    private static boolean hasCaughtSpecies(Pokemon pokemon) {
        String speciesName = pokemon.getSpecies().getResourceIdentifier().getPath();
        
        // Check local registry (includes PC history)
        if (ClientOnlyDataManager.hasCaughtSpecies(speciesName)) {
            return true;
        }
        
        // Check current party
        try {
            var party = com.cobblemon.mod.common.client.CobblemonClient.INSTANCE.getStorage().getParty();
            if (party != null) {
                for (var p : party) {
                    if (p != null && p.getSpecies().getResourceIdentifier().getPath().equals(speciesName)) {
                        ClientOnlyDataManager.markSpeciesAsSeen(speciesName);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // If we can't access storage, check local registry only
        }
        return false;
    }
    
    private static boolean isInActiveGeneration(String pokemonName, String generation) {
        var genData = ConfigManager.getGenerationData(generation);
        return genData != null && genData.pokemon.contains(pokemonName);
    }
    
    private static RarityCategory getRarity(Pokemon pokemon, String name, List<String> huntList, ConfigPokemon config) {
        // Priority order matches server-side logic
        if (huntList.stream().anyMatch(h -> h.equalsIgnoreCase(name))) {
            return RarityCategory.CUSTOM;
        }
        if (pokemon.getShiny()) {
            return RarityCategory.SHINY;
        }
        if (config.LEGENDARY.contains(name)) {
            return RarityCategory.LEGENDARY;
        }
        if (config.MYTHICAL.contains(name)) {
            return RarityCategory.MYTHICAL;
        }
        if (config.ULTRA_BEAST.contains(name)) {
            return RarityCategory.ULTRA_BEAST;
        }
        if (config.PARADOX.contains(name)) {
            return RarityCategory.PARADOX;
        }
        if (config.ULTRA_RARE.contains(name)) {
            return RarityCategory.ULTRA_RARE;
        }
        if (config.RARE.contains(name)) {
            return RarityCategory.RARE;
        }
        return RarityCategory.COMMON;
    }
    
    private static void sendFullNotification(PokemonEntity pokemonEntity, Pokemon pokemon, RarityCategory rarity, double distance, String status, String activeGen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        
        ConfigClient config = ConfigManager.getClientConfig();
        String displayName = pokemon.getDisplayName(true).getString();
        BlockPos pos = pokemonEntity.getBlockPos();
        UUID pokemonUuid = pokemonEntity.getUuid();
        
        // Store waypoint for cleanup
        String waypointName = displayName + "_" + System.currentTimeMillis();
        pokemonWaypoints.put(pokemonUuid, waypointName);
        
        // Toast notification
        if (config.alert_toast_enabled) {
            MutableText title;
            if (rarity == RarityCategory.HUNT) {
                title = Text.literal("Hunting Target: ").append(Text.literal(displayName).formatted(Formatting.GREEN));
            } else {
                MutableText pokemonText;
                if (rarity == RarityCategory.SHINY) {
                    pokemonText = createRainbowText(formatCategoryName(rarity.name()) + " " + displayName);
                } else {
                    pokemonText = Text.literal(formatCategoryName(rarity.name()) + " " + displayName)
                        .formatted(rarity.getColor());
                }
                title = Text.literal("A ").append(pokemonText).append(Text.literal(" has appeared"));
            }
            
            // Get sprite identifier
            String spriteName = pokemon.getForm().getSpecies().getResourceIdentifier().getPath();
            String spritePath = "textures/pokemon/";
            if (pokemon.getShiny()) {
                spritePath += "shiny/";
                spriteName += "shiny";
            }
            var spriteId = net.minecraft.util.Identifier.of(PokeNotifier.MOD_ID, spritePath + spriteName + ".png");
            
            com.zehro_mc.pokenotifier.client.NotificationHUD.show(title, Text.empty(), spriteId);
        }
        
        // Chat notification
        if (config.alert_chat_enabled) {
            MutableText prefix = Text.literal("[").formatted(Formatting.GREEN)
                .append(Text.literal("Poke Notifier").formatted(Formatting.GOLD))
                .append(Text.literal("] ").formatted(Formatting.GREEN));
            
            MutableText message = prefix;
            
            // Add category prefix for non-HUNT Pokemon
            if (rarity != RarityCategory.HUNT) {
                if (rarity == RarityCategory.SHINY) {
                    message.append(Text.literal("A wild ").formatted(Formatting.YELLOW))
                        .append(createRainbowText(formatCategoryName(rarity.name())))
                        .append(Text.literal(" ").formatted(Formatting.GRAY));
                } else {
                    message.append(Text.literal("A wild ").formatted(Formatting.YELLOW))
                        .append(Text.literal(formatCategoryName(rarity.name()) + " ")
                            .formatted(rarity.getColor()));
                }
            }
            
            // Pokemon name
            if (rarity == RarityCategory.HUNT) {
                message.append(Text.literal("Found ").formatted(Formatting.YELLOW))
                    .append(Text.literal(displayName).formatted(Formatting.GREEN));
            } else {
                message.append(Text.literal(displayName).formatted(Formatting.WHITE));
            }
            
            // Status with action text
            if ("NEW".equals(status)) {
                message.append(Text.literal(" appeared!").formatted(Formatting.YELLOW));
            } else {
                message.append(Text.literal(" is here again").formatted(Formatting.GRAY));
            }
            
            // Level
            message.append(Text.literal(" · ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal("Lvl " + pokemon.getLevel()).formatted(Formatting.AQUA));
            
            // Check if Xaero's is available
            boolean hasXaero = config.create_waypoints_enabled && XaeroIntegration.isXaeroAvailable();
            
            if (hasXaero) {
                // Format: [Poke Notifier] A wild Shiny Mewtwo appeared! · Lvl 70 · 50 blocks away [Track]
                message.append(Text.literal(" · ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal((int)distance + " blocks away").formatted(Formatting.GOLD))
                    .append(Text.literal(" ").formatted(Formatting.DARK_GRAY));
                
                // Waypoint button [Track]
                final int x = pos.getX();
                final int y = pos.getY();
                final int z = pos.getZ();
                String tempName = displayName.replaceAll("[^a-zA-Z0-9 ]", "").trim();
                if (tempName.isEmpty()) tempName = "Pokemon";
                if (tempName.length() > 15) tempName = tempName.substring(0, 15);
                final String cleanName = tempName;
                
                message.append(Text.literal("[Track]")
                    .styled(style -> style
                        .withColor(Formatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/xaero_waypoint_add:" + cleanName + ":" + cleanName.substring(0, Math.min(1, cleanName.length())) + ":" + x + ":" + y + ":" + z + ":6:false:0:Internal-xaero-waypoint"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Text.literal("Click to track on map").formatted(Formatting.YELLOW)))));
            } else {
                // Format: [Poke Notifier] A wild Shiny Mewtwo appeared! · Lvl 70 · At 100, 64, 200 · 50 blocks away
                String coordinates = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                
                message.append(Text.literal(" · At ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(coordinates)
                        .styled(style -> style
                            .withColor(Formatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coordinates))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Click to copy").formatted(Formatting.YELLOW)))))
                    .append(Text.literal(" · ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal((int)distance + " blocks away").formatted(Formatting.GOLD));
            }
            
            client.player.sendMessage(message, false);
        }
        
        // Sound notification (using correct sound instance)
        if (config.alert_sounds_enabled && "NEW".equals(status)) {
            float volume = config.alert_sound_volume;
            client.getSoundManager().play(
                net.minecraft.client.sound.PositionedSoundInstance.master(
                    net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, volume, 0.5F
                )
            );
        }
        
        // Apply glow effect using team-based glowing (works in client-only)
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null && mc.player != null) {
                // Create or get a team for glowing entities
                var scoreboard = mc.world.getScoreboard();
                var teamName = "poke_notifier_glow";
                var team = scoreboard.getTeam(teamName);
                
                if (team == null) {
                    team = scoreboard.addTeam(teamName);
                    team.setColor(net.minecraft.util.Formatting.YELLOW);
                }
                
                // Add entity to team (this makes it glow)
                String entityName = pokemonEntity.getUuidAsString();
                if (!team.getPlayerList().contains(entityName)) {
                    scoreboard.addScoreHolderToTeam(entityName, team);
                }
                
                // Schedule removal after duration
                int glowingTicks = config.glowing_duration_seconds * 20;
                final UUID entityId = pokemonEntity.getUuid();
                
                new Thread(() -> {
                    try {
                        Thread.sleep(glowingTicks * 50);
                        mc.execute(() -> {
                            if (mc.world != null) {
                                var sb = mc.world.getScoreboard();
                                var t = sb.getTeam(teamName);
                                if (t != null) {
                                    sb.removeScoreHolderFromTeam(entityName, t);
                                }
                            }
                        });
                    } catch (InterruptedException ignored) {}
                }).start();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to apply glow effect", e);
        }
    }
    
    private static MutableText createLocationText(String name, BlockPos pos, int color) {
        ConfigClient config = ConfigManager.getClientConfig();
        String coordinates = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        
        if (config.create_waypoints_enabled && XaeroIntegration.isXaeroAvailable()) {
            String waypointName = name + "_" + System.currentTimeMillis();
            
            // Create [ADD] button for waypoint
            MutableText button = Text.literal("[ADD]")
                .styled(style -> style
                    .withColor(Formatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/xaero_waypoint_add:" + waypointName + ":" + name.charAt(0) + ":" + 
                        pos.getX() + ":" + pos.getY() + ":" + pos.getZ() + ":" + color + ":false:0:Internal-overworld-waypoints"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("Click to add waypoint").formatted(Formatting.YELLOW))));
            
            return button.append(Text.literal(" " + coordinates).formatted(Formatting.AQUA));
        } else {
            // Fallback to clickable coordinates
            return Text.literal(coordinates)
                .styled(style -> style
                    .withColor(Formatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coordinates))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal("Click to copy coordinates").formatted(Formatting.YELLOW))));
        }
    }
    
    private static void cleanupWaypoints(MinecraftClient client) {
        // Remove waypoints for Pokemon that no longer exist
        pokemonWaypoints.entrySet().removeIf(entry -> {
            UUID pokemonUuid = entry.getKey();
            boolean shouldRemove = true;
            
            // Check if entity still exists
            for (Entity entity : client.world.getEntities()) {
                if (entity.getUuid().equals(pokemonUuid) && entity.isAlive() && !entity.isRemoved()) {
                    shouldRemove = false;
                    break;
                }
            }
            
            if (shouldRemove && XaeroIntegration.isXaeroAvailable()) {
                XaeroIntegration.removeWaypoint(entry.getValue());
            }
            return shouldRemove;
        });
    }
    
    private static void removeWaypointForSpecies(String speciesName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        
        // Find and remove waypoints for this species
        pokemonWaypoints.entrySet().removeIf(entry -> {
            UUID pokemonUuid = entry.getKey();
            
            for (Entity entity : client.world.getEntities()) {
                if (entity.getUuid().equals(pokemonUuid) && entity instanceof PokemonEntity pokemonEntity) {
                    String entitySpecies = pokemonEntity.getPokemon().getSpecies().getResourceIdentifier().getPath();
                    if (entitySpecies.equals(speciesName)) {
                        if (XaeroIntegration.isXaeroAvailable()) {
                            XaeroIntegration.removeWaypoint(entry.getValue());
                        }
                        return true;
                    }
                }
            }
            return false;
        });
    }
    
    private static MutableText createRainbowText(String text) {
        MutableText result = Text.empty();
        Formatting[] colors = {Formatting.RED, Formatting.GOLD, Formatting.YELLOW, 
                               Formatting.GREEN, Formatting.AQUA, Formatting.LIGHT_PURPLE};
        for (int i = 0; i < text.length(); i++) {
            result.append(Text.literal(String.valueOf(text.charAt(i))).formatted(colors[i % colors.length]));
        }
        return result;
    }
    
    
    private static void spawnHighlightParticles(net.minecraft.client.world.ClientWorld world, PokemonEntity entity, int durationSeconds) {
        // Spawn continuous particles around the Pokemon for visual highlight
        double x = entity.getX();
        double y = entity.getY() + entity.getHeight() / 2;
        double z = entity.getZ();
        
        // Spawn multiple bursts of particles in a helix pattern
        for (int burst = 0; burst < 3; burst++) {
            for (int i = 0; i < 30; i++) {
                double angle = (i / 30.0) * Math.PI * 2;
                double height = (burst * 0.5) - 0.5;
                double radius = 0.8;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                
                // Use FLAME particles (more visible)
                world.addParticle(
                    net.minecraft.particle.ParticleTypes.FLAME,
                    x + offsetX, y + height, z + offsetZ,
                    0, 0.02, 0
                );
                
                // Add some sparkle particles
                if (i % 5 == 0) {
                    world.addParticle(
                        net.minecraft.particle.ParticleTypes.END_ROD,
                        x + offsetX, y + height, z + offsetZ,
                        0, 0.05, 0
                    );
                }
            }
        }
    }
    
    private static String formatCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) return "Rare";
        String[] words = categoryName.toLowerCase().split("_");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1);
        }
        return String.join(" ", words);
    }
}
