/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.commands.handlers;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigServer;
import com.zehro_mc.pokenotifier.globalhunt.GlobalHuntManager;
import com.zehro_mc.pokenotifier.networking.AdminStatusPayload;
import com.zehro_mc.pokenotifier.networking.OpenGuiPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles user-accessible commands (available to all players).
 */
public class UserCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserCommandHandler.class);
    
    /**
     * Registers all user commands to the main command node.
     * 
     * @param mainNode The main pokenotifier command node
     */
    public static void registerUserCommands(LiteralArgumentBuilder<ServerCommandSource> mainNode) {
        // GUI command - accessible to all players
        var guiCommand = CommandManager.literal("gui")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player != null) {
                        ConfigServer config = ConfigManager.getServerConfig();
                        GlobalHuntManager huntManager = GlobalHuntManager.getInstance();
                        boolean hasActiveEvent = huntManager.hasActiveEvent();
                        String activePokemon = hasActiveEvent ? 
                            (huntManager.getCurrentEvent().isShiny() ? "Shiny " : "") + huntManager.getCurrentEvent().getPokemonName() : "";
                        
                        boolean globalHuntEnabled = config.global_hunt_system_enabled;
                        LOGGER.info("[SERVER] Sending admin status via GUI command to {} - Global Hunt System: {}", player.getName().getString(), globalHuntEnabled);
                        
                        ServerPlayNetworking.send(player, new AdminStatusPayload(
                                player.hasPermissionLevel(2),
                                config.debug_mode_enabled,
                                config.enable_test_mode,
                                config.bounty_system_enabled,
                                globalHuntEnabled,
                                hasActiveEvent,
                                activePokemon));
                        ServerPlayNetworking.send(player, new OpenGuiPayload());
                    }
                    return 1;
                });
        
        mainNode.then(guiCommand);
    }
}