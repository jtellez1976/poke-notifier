/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking.handlers;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.data.ConfigSyncHandler;
import com.zehro_mc.pokenotifier.data.DataManager;
import com.zehro_mc.pokenotifier.data.PlayerDataHandler;
import com.zehro_mc.pokenotifier.data.BackupManager;
import com.zehro_mc.pokenotifier.event.EventManager;
// import com.zehro_mc.pokenotifier.globalhunt.GlobalHuntManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.networking.*;
import com.zehro_mc.pokenotifier.util.UpdateChecker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all server-side packet processing.
 */
public class ServerPacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPacketHandler.class);
    
    /**
     * Registers all server packet receivers.
     */
    public static void register() {
        registerCustomListHandlers();
        registerAdminCommandHandlers();
        registerCatchEmAllHandlers();
        registerUpdateSourceHandlers();
        
        LOGGER.info("[Networking] Server packet handlers registered");
    }
    
    /**
     * Registers custom list update packet handlers.
     */
    private static void registerCustomListHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(CustomListUpdatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String pokemonName = payload.pokemonName().toLowerCase().trim();

            context.server().execute(() -> {
                switch (payload.action()) {
                    case ADD -> {
                        try {
                            PokemonProperties.Companion.parse(pokemonName);
                            PlayerDataHandler.addToCustomList(player, pokemonName);
                        } catch (Exception e) {
                            List<Text> errorLines = new ArrayList<>(List.of(Text.literal("Error: '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' is not a valid Pokémon name.").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(errorLines));
                        }
                    }
                    case REMOVE -> PlayerDataHandler.removeFromCustomList(player, pokemonName);
                    case LIST -> PlayerDataHandler.sendCustomHuntList(player);
                    case CLEAR -> PlayerDataHandler.clearCustomList(player);
                }
            });
        });
    }
    
    /**
     * Registers admin command packet handlers.
     */
    private static void registerAdminCommandHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(AdminCommandPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            
            context.server().execute(() -> {
                // Check permissions for admin-only commands
                boolean requiresAdmin = switch (payload.action()) {
                    case HELP, VERSION, STATUS -> false;
                    default -> true;
                };
                
                if (requiresAdmin && !player.hasPermissionLevel(2)) {
                    List<Text> errorLines = new ArrayList<>(List.of(Text.literal("You don't have permission to use admin commands.").formatted(Formatting.RED)));
                    ServerPlayNetworking.send(player, new GuiResponsePayload(errorLines));
                    return;
                }
                
                AdminCommandProcessor.processCommand(player, payload, context.server());
            });
        });
    }
    
    /**
     * Registers Catch 'em All update packet handlers.
     */
    private static void registerCatchEmAllHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(CatchemallUpdatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String genName = payload.generationName().toLowerCase().trim();

            context.server().execute(() -> {
                CatchEmAllProcessor.processUpdate(player, payload.action(), genName);
            });
        });
    }
    
    /**
     * Registers update source packet handlers.
     */
    private static void registerUpdateSourceHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(UpdateSourcePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String source = payload.source();

            context.server().execute(() -> {
                if (player.hasPermissionLevel(2) && List.of("modrinth", "curseforge", "none").contains(source)) {
                    ConfigManager.getServerConfig().update_checker_source = source;
                    List<Text> lines = new ArrayList<>(List.of(Text.literal("Update check source set to: ").formatted(Formatting.GREEN)
                            .append(Text.literal(source).formatted(Formatting.GOLD))));
                    ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    ConfigManager.saveServerConfigToFile();
                    
                    ConfigSyncHandler.syncConfigurationChange(context.server(), ConfigSyncHandler.ConfigType.UPDATE_SOURCE);
                    UpdateChecker.checkForUpdates(player);
                }
            });
        });
    }
}