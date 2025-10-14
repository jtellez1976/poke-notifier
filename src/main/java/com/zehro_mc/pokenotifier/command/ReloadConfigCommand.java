/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Registers the /pokenotifier reloadconfig command.
 */
public class ReloadConfigCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> pokenotifierCommand = CommandManager.literal("pokenotifier")
                .requires(source -> source.hasPermissionLevel(2));

        pokenotifierCommand.then(CommandManager.literal("reloadconfig")
                .executes(context -> {
                    try {
                        ConfigManager.loadConfig();
                        context.getSource().sendFeedback(() -> Text.literal("Poke Notifier configurations reloaded successfully.").formatted(Formatting.GREEN), true);
                        return 1;
                    } catch (ConfigManager.ConfigReadException e) {
                        PokeNotifier.LOGGER.error("Failed to reload Poke Notifier configuration: {}", e.getMessage());
                        context.getSource().sendError(Text.literal("Error reloading configs: " + e.getMessage()).formatted(Formatting.RED));
                        context.getSource().sendError(Text.literal("To generate new default configs, use: /pokenotifier reloadconfig new").formatted(Formatting.YELLOW));
                        return 0;
                    }
                })
                .then(CommandManager.literal("new")
                        .executes(context -> {
                            try {
                                ConfigManager.resetToDefault();
                                context.getSource().sendFeedback(() -> Text.literal("New default poke-notifier configs have been generated.").formatted(Formatting.GREEN), true);
                                return 1;
                            } catch (Exception e) {
                                PokeNotifier.LOGGER.error("Failed to generate new default configurations.", e);
                                context.getSource().sendError(Text.literal("Failed to generate new configs. Check server logs.").formatted(Formatting.RED));
                                return 0;
                            }
                        })
                )
        );

        dispatcher.register(pokenotifierCommand);
    }
}