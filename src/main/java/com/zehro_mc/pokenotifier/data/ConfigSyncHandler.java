/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.data;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigServer;
import com.zehro_mc.pokenotifier.events.SwarmConfig;
// import com.zehro_mc.pokenotifier.globalhunt.GlobalHuntManager;
import com.zehro_mc.pokenotifier.networking.AdminStatusPayload;
// import com.zehro_mc.pokenotifier.networking.UpdateSourceSyncPayload;
import com.zehro_mc.pokenotifier.util.PokeNotifierServerUtils;
import com.zehro_mc.pokenotifier.util.PlayerRankManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles configuration synchronization between server and clients.
 */
public class ConfigSyncHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSyncHandler.class);
    
    /**
     * Performs initial synchronization when a player joins the server.
     * @param player The joining player
     */
    public static void performInitialSync(ServerPlayerEntity player) {
        // Perform initial PC sync
        DataManager.performInitialPcSync(player);
        
        // Send catch progress update
        PokeNotifierServerUtils.sendCatchProgressUpdate(player);

        // Sync admin status with the client
        ConfigServer config = ConfigManager.getServerConfig();
        // GlobalHuntManager huntManager = GlobalHuntManager.getInstance();
        // boolean hasActiveEvent = huntManager.hasActiveEvent();
        // String activePokemon = hasActiveEvent ? 
        //     (huntManager.getCurrentEvent().isShiny() ? "Shiny " : "") + huntManager.getCurrentEvent().getPokemonName() : "";
        
        boolean globalHuntEnabled = config.global_hunt_system_enabled;
        LOGGER.info("[SERVER] Sending admin status to {} - Global Hunt System: {}", player.getName().getString(), globalHuntEnabled);
        
        SwarmConfig swarmConfig = SwarmConfig.load();
        ServerPlayNetworking.send(player, new AdminStatusPayload(
                player.hasPermissionLevel(2),
                config.debug_mode_enabled,
                config.enable_test_mode,
                config.bounty_system_enabled,
                globalHuntEnabled,
                swarmConfig.system_enabled,
                false, // hasActiveEvent,
                "", // activePokemon
                false, // hasActiveSwarm
                "")); // activeSwarmPokemon
        
        // Sync update source with client
        // ServerPlayNetworking.send(player, new UpdateSourceSyncPayload(config.update_checker_source));

        // Update player rank
        PlayerRankManager.onPlayerJoin(player);
    }

    /**
     * Syncs configuration changes with all online players.
     * @param server The minecraft server instance
     * @param configType The type of configuration that changed
     */
    public static void syncConfigurationChange(net.minecraft.server.MinecraftServer server, ConfigType configType) {
        ConfigServer config = ConfigManager.getServerConfig();
        
        switch (configType) {
            case UPDATE_SOURCE -> {
                // for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                //     ServerPlayNetworking.send(player, new UpdateSourceSyncPayload(config.update_checker_source));
                // }
            }
            case ADMIN_STATUS -> {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    // GlobalHuntManager huntManager = GlobalHuntManager.getInstance();
                    // boolean hasActiveEvent = huntManager.hasActiveEvent();
                    // String activePokemon = hasActiveEvent ? 
                    //     (huntManager.getCurrentEvent().isShiny() ? "Shiny " : "") + huntManager.getCurrentEvent().getPokemonName() : "";
                    
                    boolean globalHuntEnabled = config.global_hunt_system_enabled;
                    LOGGER.info("[SERVER] Syncing admin status to {} - Global Hunt System: {}", player.getName().getString(), globalHuntEnabled);
                    
                    SwarmConfig swarmConfig = SwarmConfig.load();
                    ServerPlayNetworking.send(player, new AdminStatusPayload(
                            player.hasPermissionLevel(2),
                            config.debug_mode_enabled,
                            config.enable_test_mode,
                            config.bounty_system_enabled,
                            globalHuntEnabled,
                            swarmConfig.system_enabled,
                            false, // hasActiveEvent,
                            "", // activePokemon
                            false, // hasActiveSwarm
                            "")); // activeSwarmPokemon
                }
            }
        }
    }

    /**
     * Handles bounty notifications for joining players.
     * @param player The joining player
     * @param activeBounty The current active bounty, if any
     */
    public static void handleBountyNotification(ServerPlayerEntity player, String activeBounty) {
        if (activeBounty != null) {
            Text message = Text.literal("Psst! There is currently an active bounty for a ").formatted(Formatting.GRAY)
                    .append(Text.literal(activeBounty).formatted(Formatting.GOLD))
                    .append(Text.literal("!").formatted(Formatting.GRAY));
            player.sendMessage(message, false);
        }
    }

    /**
     * Handles update source configuration prompts for admins.
     * @param player The admin player
     * @param updateSourceConfigured Whether update source is configured
     * @param latestVersionInfo Latest version information, if available
     * @param updateCheckCompleted Whether update check has completed
     */
    public static void handleUpdateSourcePrompt(ServerPlayerEntity player, boolean updateSourceConfigured, 
                                              Object latestVersionInfo, boolean updateCheckCompleted) {
        if (!player.hasPermissionLevel(2)) return;
        
        if (!updateSourceConfigured) {
            Text prompt = Text.literal("Please configure the update source from the GUI panel:").formatted(Formatting.YELLOW);
            Text guiButton = Text.literal("[Open GUI]").formatted(Formatting.GREEN)
                    .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent(
                            net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/pnc gui")));

            player.sendMessage(prompt, false);
            player.sendMessage(Text.literal("Go to User Tools > Info & Help to configure update source: ").append(guiButton), false);
        }
    }

    public enum ConfigType {
        UPDATE_SOURCE,
        ADMIN_STATUS,
        BOUNTY_SYSTEM,
        GLOBAL_HUNT
    }
}