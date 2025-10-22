/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking.handlers;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigServer;
import com.zehro_mc.pokenotifier.data.BackupManager;
import com.zehro_mc.pokenotifier.data.DataManager;
import com.zehro_mc.pokenotifier.event.EventManager;
// import com.zehro_mc.pokenotifier.globalhunt.GlobalHuntManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.AdminCommandPayload;
import com.zehro_mc.pokenotifier.networking.GuiResponsePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes admin command payloads from clients.
 */
public class AdminCommandProcessor {
    
    /**
     * Processes an admin command payload.
     * @param player The player who sent the command
     * @param payload The command payload
     * @param server The minecraft server instance
     */
    public static void processCommand(ServerPlayerEntity player, AdminCommandPayload payload, MinecraftServer server) {
        switch (payload.action()) {
            case TOGGLE_DEBUG_MODE -> toggleDebugMode(player);
            case TOGGLE_TEST_MODE -> toggleTestMode(player);
            case TOGGLE_BOUNTY_SYSTEM -> toggleBountySystem(player);
            case SERVER_STATUS -> sendServerStatus(player);
            case RELOAD_CONFIG -> reloadConfig(player);
            case RESET_CONFIG -> resetConfig(player);
            case START_SWARM -> startSwarm(player, payload.parameter(), server);
            case CANCEL_SWARM -> cancelSwarm(player);
            case SWARM_STATUS -> swarmStatus(player);
            case TOGGLE_SWARM_SYSTEM -> toggleSwarmSystem(player);
            case AUTOCOMPLETE_PLAYER -> autocompletePlayer(player, payload.parameter());
            case ROLLBACK_PLAYER -> rollbackPlayer(player, payload.parameter());
            case SPAWN_POKEMON -> spawnPokemon(player, payload.parameter());
            case HELP -> sendHelp(player);
            case VERSION -> sendVersion(player);
            case STATUS -> sendStatus(player);
        }
    }
    
    private static void toggleDebugMode(ServerPlayerEntity player) {
        ConfigServer config = ConfigManager.getServerConfig();
        config.debug_mode_enabled = !config.debug_mode_enabled;
        ConfigManager.saveServerConfigToFile();
        List<Text> lines = new ArrayList<>(List.of(Text.literal("Debug mode ").append(config.debug_mode_enabled ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED))));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
    }
    
    private static void toggleTestMode(ServerPlayerEntity player) {
        ConfigServer config = ConfigManager.getServerConfig();
        config.enable_test_mode = !config.enable_test_mode;
        ConfigManager.saveServerConfigToFile();
        List<Text> lines = new ArrayList<>(List.of(Text.literal("Test mode ").append(config.enable_test_mode ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED))));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
    }
    
    private static void toggleBountySystem(ServerPlayerEntity player) {
        ConfigServer config = ConfigManager.getServerConfig();
        config.bounty_system_enabled = !config.bounty_system_enabled;
        ConfigManager.saveServerConfigToFile();
        List<Text> lines = new ArrayList<>(List.of(Text.literal("Bounty system ").append(config.bounty_system_enabled ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED))));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        if (!config.bounty_system_enabled) {
            EventManager.getBountySystem().clearActiveBounty(false);
        }
    }
    
    private static void sendServerStatus(ServerPlayerEntity player) {
        ConfigServer config = ConfigManager.getServerConfig();
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("--- Poke Notifier Server Status ---").formatted(Formatting.GOLD));
        lines.add(createServerStatusLine("Debug Mode", config.debug_mode_enabled));
        lines.add(createServerStatusLine("Bounty System", config.bounty_system_enabled));
        if (config.bounty_system_enabled) {
            String currentBounty = EventManager.getBountySystem().getActiveBounty();
            MutableText bountyStatus = Text.literal("  Current Bounty = ").formatted(Formatting.WHITE);
            bountyStatus.append(currentBounty == null ? Text.literal("None").formatted(Formatting.GRAY) : Text.literal(currentBounty).formatted(Formatting.GOLD));
            lines.add(bountyStatus);
        }
        lines.add(createServerStatusLine("Test Mode", config.enable_test_mode));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
    }
    
    private static void reloadConfig(ServerPlayerEntity player) {
        try {
            ConfigManager.loadConfig();
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Poke Notifier configurations reloaded successfully.").formatted(Formatting.GREEN)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        } catch (ConfigManager.ConfigReadException e) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Error reloading configs: " + e.getMessage()).formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }
    
    private static void resetConfig(ServerPlayerEntity player) {
        try {
            ConfigManager.resetToDefault();
            List<Text> lines = new ArrayList<>(List.of(Text.literal("All Poke Notifier configurations have been reset to default.").formatted(Formatting.GREEN)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        } catch (Exception e) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Failed to generate new configs. Check server logs.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }
    
    private static void startSwarm(ServerPlayerEntity player, String parameter, MinecraftServer server) {
        String[] parts = parameter.split(" ");
        String pokemonName = parts[0].trim();
        boolean spawnHere = parameter.contains(" here");
        
        if (!pokemonName.isEmpty()) {
            com.zehro_mc.pokenotifier.events.SwarmEventManager swarmManager = 
                com.zehro_mc.pokenotifier.events.SwarmEventManager.getInstance();
            if (swarmManager != null) {
                try {
                    boolean success;
                    if (spawnHere) {
                        success = swarmManager.startManualSwarmAt(pokemonName, player.getBlockPos(), player.getName().getString());
                    } else {
                        success = swarmManager.startManualSwarm(pokemonName, player.getName().getString());
                    }
                    
                    if (success) {
                        String location = spawnHere ? " at your location" : "";
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Starting manual swarm of ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(location + " (shiny guaranteed)...").formatted(Formatting.GREEN)));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    } else {
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Failed to start swarm - another swarm is already active.").formatted(Formatting.RED)));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
                } catch (Exception e) {
                    List<Text> lines = new ArrayList<>(List.of(Text.literal("Error starting swarm: " + e.getMessage()).formatted(Formatting.RED)));
                    ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                }
            } else {
                List<Text> lines = new ArrayList<>(List.of(Text.literal("Swarm system not available.").formatted(Formatting.RED)));
                ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            }
        } else {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Please specify a Pokemon name.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }
    
    private static void cancelSwarm(ServerPlayerEntity player) {
        com.zehro_mc.pokenotifier.events.SwarmEventManager swarmManager = 
            com.zehro_mc.pokenotifier.events.SwarmEventManager.getInstance();
        if (swarmManager != null) {
            if (swarmManager.hasActiveSwarm()) {
                swarmManager.endCurrentSwarm("admin");
                List<Text> lines = new ArrayList<>(List.of(Text.literal("Swarm cancelled successfully.").formatted(Formatting.GREEN)));
                ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            } else {
                List<Text> lines = new ArrayList<>(List.of(Text.literal("No active swarm to cancel.").formatted(Formatting.YELLOW)));
                ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            }
        } else {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Swarm system not available.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }
    
    private static void swarmStatus(ServerPlayerEntity player) {
        com.zehro_mc.pokenotifier.events.SwarmEventManager swarmManager = 
            com.zehro_mc.pokenotifier.events.SwarmEventManager.getInstance();
        com.zehro_mc.pokenotifier.events.SwarmConfig swarmConfig = com.zehro_mc.pokenotifier.events.SwarmConfig.load();
        
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("--- Swarm System Status ---").formatted(Formatting.GOLD));
        lines.add(Text.literal("Automatic Mode: ").append(swarmConfig.system_enabled ? Text.literal("ENABLED").formatted(Formatting.GREEN) : Text.literal("DISABLED").formatted(Formatting.RED)));
        
        com.zehro_mc.pokenotifier.events.SwarmStatistics.CurrentSwarm current = com.zehro_mc.pokenotifier.events.SwarmStatistics.getCurrentSwarm();
        if (current != null) {
            lines.add(Text.literal("Current Swarm: ").append(Text.literal(current.pokemonName).formatted(Formatting.GOLD)));
            lines.add(Text.literal("Type: ").append(Text.literal(current.swarmType).formatted(Formatting.AQUA)));
            lines.add(Text.literal("Triggered by: ").append(Text.literal(current.triggeredBy).formatted(Formatting.AQUA)));
            lines.add(Text.literal("Location: ").append(Text.literal(current.location.x + ", " + current.location.y + ", " + current.location.z).formatted(Formatting.WHITE)));
            lines.add(Text.literal("Entities Alive: ").append(Text.literal(String.valueOf(current.entitiesAlive)).formatted(Formatting.GREEN)));
            lines.add(Text.literal("Entities Captured: ").append(Text.literal(String.valueOf(current.entitiesCaptured)).formatted(Formatting.YELLOW)));
            if (swarmManager != null) {
                int remaining = swarmManager.getRemainingMinutes();
                lines.add(Text.literal("Time Remaining: ").append(Text.literal(remaining + " minutes").formatted(Formatting.AQUA)));
            }
        } else {
            lines.add(Text.literal("Current Swarm: ").append(Text.literal("None").formatted(Formatting.GRAY)));
        }
        
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
    }
    
    private static void toggleSwarmSystem(ServerPlayerEntity player) {
        com.zehro_mc.pokenotifier.events.SwarmEventManager swarmManager = 
            com.zehro_mc.pokenotifier.events.SwarmEventManager.getInstance();
        if (swarmManager != null) {
            swarmManager.toggleSystem();
            com.zehro_mc.pokenotifier.events.SwarmConfig config = com.zehro_mc.pokenotifier.events.SwarmConfig.load();
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Swarm automatic mode ").append(config.system_enabled ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED))));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        } else {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Swarm system not available.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }
    
    private static void autocompletePlayer(ServerPlayerEntity player, String playerName) {
        ServerPlayerEntity targetPlayer = player.getServer().getPlayerManager().getPlayer(playerName.trim());
        if (targetPlayer == null) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Player " + playerName + " is not online.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            return;
        }
        
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(targetPlayer.getUuid());
        if (progress.active_generations.isEmpty()) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Player " + playerName + " does not have Catch 'em All mode active.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            return;
        }
        
        String genName = progress.active_generations.iterator().next();
        GenerationData genData = ConfigManager.getGenerationData(genName);
        if (genData == null) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Internal error: Could not find data for generation '" + genName + "'.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            return;
        }
        
        BackupManager.createPlayerProgressBackup(targetPlayer);
        String missingPokemon = DataManager.autocompleteGenerationForPlayer(targetPlayer, genName, genData);
        List<Text> response = new ArrayList<>();
        response.add(Text.literal("Autocompleted " + formatGenName(genName) + " for player " + targetPlayer.getName().getString()).formatted(Formatting.GREEN));
        response.add(Text.literal("To complete the list, capture: ").append(Text.literal(missingPokemon).formatted(Formatting.GOLD)).formatted(Formatting.AQUA));
        ServerPlayNetworking.send(player, new GuiResponsePayload(response));
    }
    
    private static void rollbackPlayer(ServerPlayerEntity player, String playerName) {
        ServerPlayerEntity targetPlayer = player.getServer().getPlayerManager().getPlayer(playerName.trim());
        if (targetPlayer == null) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Player " + playerName + " is not online.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            return;
        }
        
        boolean success = DataManager.rollbackPlayerProgress(targetPlayer);
        if (success) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Successfully rolled back progress for " + playerName).formatted(Formatting.GREEN)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        } else {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("No backup file found for " + playerName + ".").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }
    
    private static void spawnPokemon(ServerPlayerEntity player, String parameter) {
        // Implementation would go here - spawn pokemon logic
        List<Text> lines = new ArrayList<>(List.of(Text.literal("Pokemon spawning not yet implemented in processor.").formatted(Formatting.YELLOW)));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
    }
    
    private static void sendHelp(ServerPlayerEntity player) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("--- Poke Notifier Admin Help ---").formatted(Formatting.GOLD));
        lines.add(Text.literal("Use the GUI to manage all mod settings.").formatted(Formatting.WHITE));
        lines.add(Text.literal("Server Control: Toggle debug/test modes, reload configs").formatted(Formatting.AQUA));
        lines.add(Text.literal("Event Management: Control bounty system and swarms").formatted(Formatting.AQUA));
        lines.add(Text.literal("Player Data: Manage player progress and backups").formatted(Formatting.AQUA));
        lines.add(Text.literal("Testing: Spawn Pokémon for testing purposes").formatted(Formatting.AQUA));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
    }
    
    private static void sendVersion(ServerPlayerEntity player) {
        String modVersion = FabricLoader.getInstance()
                .getModContainer("poke-notifier")
                .map(ModContainer::getMetadata)
                .map(meta -> meta.getVersion().getFriendlyString())
                .orElse("Unknown");
        List<Text> lines = new ArrayList<>(List.of(Text.literal("Poke Notifier ver. " + modVersion).formatted(Formatting.AQUA)));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
    }
    
    private static void sendStatus(ServerPlayerEntity player) {
        ConfigServer config = ConfigManager.getServerConfig();
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("--- Poke Notifier Server Status ---").formatted(Formatting.GOLD));
        lines.add(createServerStatusLine("Debug Mode", config.debug_mode_enabled));
        lines.add(createServerStatusLine("Bounty System", config.bounty_system_enabled));
        lines.add(createServerStatusLine("Test Mode", config.enable_test_mode));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
    }
    
    private static MutableText createServerStatusLine(String label, boolean isEnabled) {
        MutableText message = Text.literal(label + " = ").formatted(Formatting.WHITE);
        if (isEnabled) {
            message.append(Text.literal("ON").formatted(Formatting.GREEN));
        } else {
            message.append(Text.literal("OFF").formatted(Formatting.RED));
        }
        return message;
    }
    
    private static String formatGenName(String genName) {
        if (genName == null || !genName.toLowerCase().startsWith("gen")) return genName;
        return "Gen" + genName.substring(3);
    }
}