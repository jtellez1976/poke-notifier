/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.api.PokeNotifierApi;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload;
import com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload;
import com.zehro_mc.pokenotifier.networking.UpdateSourcePayload;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.command.CommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Registers and handles all client-side commands, which start with /pnc.
 */
public class ClientCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> pncCommand = ClientCommandManager.literal("pnc");

        // --- FIX: Create a client-side command to handle the update source selection ---
        // This command is what the clickable text will now execute.
        pncCommand.then(ClientCommandManager.literal("set_update_source")
                .then(ClientCommandManager.argument("source", StringArgumentType.string())
                        .executes(context -> {
                            String source = StringArgumentType.getString(context, "source");
                            // Send a packet to the server with the chosen source.
                            ClientPlayNetworking.send(new UpdateSourcePayload(source));
                            context.getSource().sendFeedback(Text.literal("Setting update source to: ").formatted(Formatting.YELLOW)
                                    .append(Text.literal(source).formatted(Formatting.GOLD)));
                            return 1;
                        }))
        );

        // --- Alerts Subcommand ---
        var alertsNode = ClientCommandManager.literal("alerts")
                .then(buildCommandToggle("sound", "Alert Sounds", (config, enabled) -> config.alert_sounds_enabled = enabled))
                .then(buildCommandToggle("toast", "HUD (Toast) alerts", (config, enabled) -> config.alert_toast_enabled = enabled))
                .then(buildCommandToggle("chat", "Chat alerts", (config, enabled) -> config.alert_chat_enabled = enabled));

        pncCommand.then(ClientCommandManager.literal("silent")
                .then(ClientCommandManager.literal("ON").executes(context -> {
                    ConfigClient config = ConfigManager.getClientConfig();
                    config.alert_sounds_enabled = false;
                    config.alert_toast_enabled = false;
                    config.alert_chat_enabled = false;
                    config.silent_mode_enabled = true;
                    config.searching_enabled = false;
                    ConfigManager.saveClientConfigToFile();
                    context.getSource().sendFeedback(Text.literal("Silent mode is now ON. All notifications are disabled.").formatted(Formatting.RED));
                    return 1;
                }))
                .then(ClientCommandManager.literal("OFF").executes(context -> {
                    ConfigClient config = ConfigManager.getClientConfig();
                    config.alert_sounds_enabled = true;
                    config.alert_toast_enabled = true;
                    config.alert_chat_enabled = true;
                    config.silent_mode_enabled = false;
                    config.searching_enabled = true;
                    ConfigManager.saveClientConfigToFile();
                    context.getSource().sendFeedback(Text.literal("Silent mode is now OFF. Notifications are enabled.").formatted(Formatting.GREEN));
                    return 1;
                })));

        SuggestionProvider<FabricClientCommandSource> pokemonSuggestionProvider = (context, builder) ->
                CommandSource.suggestMatching(PokeNotifierApi.getAllPokemonNames(), builder);

        // --- Custom Catch Subcommand ---
        var customcatchNode = ClientCommandManager.literal("customcatch")
                .then(ClientCommandManager.literal("add") // /pnc list add <pokemon>
                        .then(ClientCommandManager.argument("pokemon", StringArgumentType.greedyString())
                                .suggests(pokemonSuggestionProvider)
                                .executes(context -> {
                                    String pokemonName = StringArgumentType.getString(context, "pokemon");
                                    ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.ADD, pokemonName));
                                    context.getSource().sendFeedback(Text.literal("Request sent to add ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" to your custom list.").formatted(Formatting.YELLOW));
                                    return 1;
                                }))) // /pnc list remove <pokemon>
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("pokemon", StringArgumentType.greedyString())
                                .suggests(pokemonSuggestionProvider)
                                .executes(context -> {
                                    String pokemonName = StringArgumentType.getString(context, "pokemon");
                                    ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.REMOVE, pokemonName));
                                    context.getSource().sendFeedback(Text.literal("Request sent to remove ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" from your custom list.").formatted(Formatting.YELLOW));
                                    return 1;
                                }))) // /pnc list view
                .then(ClientCommandManager.literal("view")
                        .executes(context -> {
                            ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.LIST, ""));
                            context.getSource().sendFeedback(Text.literal("Requesting your custom list from the server...").formatted(Formatting.YELLOW));
                            return 1;
                        })) // /pnc list clear
                .then(ClientCommandManager.literal("clear")
                        .executes(context -> {
                            ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.CLEAR, ""));
                            context.getSource().sendFeedback(Text.literal("Request sent to clear your custom list.").formatted(Formatting.YELLOW));
                            return 1;
                        }));

        SuggestionProvider<FabricClientCommandSource> generationSuggestionProvider = (context, builder) ->
                CompletableFuture.supplyAsync(() -> {
                    String activeGen = PokeNotifierClient.currentCatchEmAllGeneration;
                    Stream.of("gen1", "gen2", "gen3", "gen4", "gen5", "gen6", "gen7", "gen8", "gen9")
                            .filter(gen -> !gen.equals(activeGen)) // --- CORRECCIÓN: No sugerir la generación ya activa ---
                            .forEach(builder::suggest); 
                    return builder.build();
                });
        
        // Create a specific suggestion provider for the 'disable' command.
        SuggestionProvider<FabricClientCommandSource> activeGenerationSuggestionProvider = (context, builder) ->
                CompletableFuture.supplyAsync(() -> {
                    if (PokeNotifierClient.currentCatchEmAllGeneration != null && !"none".equals(PokeNotifierClient.currentCatchEmAllGeneration)) {
                        builder.suggest(PokeNotifierClient.currentCatchEmAllGeneration);
                    }
                    return builder.build();
                });

        var catchemallCommand = ClientCommandManager.literal("catchemall")
                .then(ClientCommandManager.literal("enable")
                        .then(ClientCommandManager.argument("generation", StringArgumentType.string())
                                .suggests(generationSuggestionProvider)
                                .executes(context -> {
                                    String genName = StringArgumentType.getString(context, "generation");
                                    ClientPlayNetworking.send(new CatchemallUpdatePayload(CatchemallUpdatePayload.Action.ENABLE, genName));
                                    context.getSource().sendFeedback(Text.literal("Requesting to track " + formatGenName(genName) + "...").formatted(Formatting.YELLOW));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("disable")
                        .then(ClientCommandManager.argument("generation", StringArgumentType.string())
                                .suggests(activeGenerationSuggestionProvider)
                                .executes(context -> {
                                    String genName = StringArgumentType.getString(context, "generation");
                                    ClientPlayNetworking.send(new CatchemallUpdatePayload(CatchemallUpdatePayload.Action.DISABLE, genName));
                                    context.getSource().sendFeedback(Text.literal("Requesting to disable Catch 'em All mode for " + genName + "...").formatted(Formatting.YELLOW));
                                    return 1;
                                }))) // Renamed from 'list' to 'status' for clarity
                .then(ClientCommandManager.literal("status")
                        .executes(context -> {
                            ClientPlayNetworking.send(new CatchemallUpdatePayload(CatchemallUpdatePayload.Action.LIST, ""));
                            context.getSource().sendFeedback(Text.literal("Requesting your active Catch 'em All modes...").formatted(Formatting.YELLOW));
                            return 1;
                        }));

        pncCommand.then(ClientCommandManager.literal("version")
                .executes(context -> {
                    String modVersion = FabricLoader.getInstance()
                            .getModContainer(PokeNotifier.MOD_ID)
                            .map(ModContainer::getMetadata)
                            .map(meta -> meta.getVersion().getFriendlyString())
                            .orElse("Unknown");

                    context.getSource().sendFeedback(Text.literal("Poke Notifier ver. " + modVersion).formatted(Formatting.AQUA));
                    return 1;
                }));

        pncCommand.then(ClientCommandManager.literal("status")
                .executes(context -> {
                    ConfigClient config = ConfigManager.getClientConfig();
                    context.getSource().sendFeedback(Text.literal("--- Poke Notifier Status ---").formatted(Formatting.GOLD));
                    sendStatusLine(context.getSource(), "Searching", config.searching_enabled);
                    sendStatusLine(context.getSource(), "Silent Mode", config.silent_mode_enabled);
                    context.getSource().sendFeedback(Text.literal("----------------------------").formatted(Formatting.GOLD));
                    sendStatusLine(context.getSource(), "  Alert Sounds", config.alert_sounds_enabled);
                    sendStatusLine(context.getSource(), "  Chat Alerts", config.alert_chat_enabled);
                    sendStatusLine(context.getSource(), "  Toast Alerts (HUD)", config.alert_toast_enabled);
                    return 1;
                }));

        // --- Help Command ---
        pncCommand.then(ClientCommandManager.literal("help")
                .executes(context -> {
                    FabricClientCommandSource source = context.getSource();
                    source.sendFeedback(Text.literal("--- Poke Notifier Help (v" + getModVersion() + ") ---").formatted(Formatting.GOLD));
                    source.sendFeedback(Text.literal("/pnc status").formatted(Formatting.AQUA).append(Text.literal(" - Shows your current settings.").formatted(Formatting.GRAY)));
                    source.sendFeedback(Text.literal("/pnc silent <on|off>").formatted(Formatting.AQUA).append(Text.literal(" - Master switch for all alerts.").formatted(Formatting.GRAY)));
                    source.sendFeedback(Text.literal(" "));
                    source.sendFeedback(Text.literal("Alerts: /pnc alerts <chat|toast|sound> <on|off>").formatted(Formatting.YELLOW));
                    source.sendFeedback(Text.literal("Example: /pnc alerts chat on").formatted(Formatting.DARK_GRAY));
                    source.sendFeedback(Text.literal(" "));
                    source.sendFeedback(Text.literal("Custom Catch: /pnc customcatch <add|remove|view|clear> [pokemon]").formatted(Formatting.YELLOW));
                    source.sendFeedback(Text.literal("Example: /pnc customcatch add pikachu").formatted(Formatting.DARK_GRAY));
                    source.sendFeedback(Text.literal(" "));
                    source.sendFeedback(Text.literal("Catch 'em All: /pnc catchemall <enable|disable|status> [gen]").formatted(Formatting.YELLOW));
                    source.sendFeedback(Text.literal("Example: /pnc catchemall enable gen1").formatted(Formatting.DARK_GRAY));

                    return 1;
                }));

        // Register all subcommands
        pncCommand.then(alertsNode);
        pncCommand.then(customcatchNode);
        pncCommand.then(catchemallCommand);

        // Redirects server-side commands to be accessible via /pnc for convenience.
        var serverCommandNode = dispatcher.getRoot().getChild("pokenotifier");
        if (serverCommandNode != null) {
            pncCommand.then(ClientCommandManager.literal("reloadconfig").redirect(serverCommandNode));
        }

        dispatcher.register(pncCommand);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommandToggle(String commandName, String feedbackText, ToggleAction action) {
        return ClientCommandManager.literal(commandName)
                .then(ClientCommandManager.literal("ON")
                        .executes(context -> {
                            ConfigClient config = ConfigManager.getClientConfig();
                            action.apply(config, true);
                            ConfigManager.saveClientConfigToFile();
                            context.getSource().sendFeedback(Text.literal(feedbackText + " are now ON").formatted(Formatting.GREEN));
                            return 1;
                        }))
                .then(ClientCommandManager.literal("OFF")
                        .executes(context -> {
                            ConfigClient config = ConfigManager.getClientConfig();
                            action.apply(config, false);
                            ConfigManager.saveClientConfigToFile();
                            context.getSource().sendFeedback(Text.literal(feedbackText + " are now OFF").formatted(Formatting.RED));
                            return 1;
                        }));
    }

    private static void sendStatusLine(FabricClientCommandSource source, String label, boolean isEnabled) {
        MutableText message = Text.literal(label + " = ").formatted(Formatting.WHITE);
        if (isEnabled) {
            message.append(Text.literal("ON").formatted(Formatting.GREEN));
        } else {
            message.append(Text.literal("OFF").formatted(Formatting.RED));
        }
        source.sendFeedback(message);
    }

    @FunctionalInterface
    private interface ToggleAction {
        void apply(ConfigClient config, boolean enabled);
    }

    private static String formatGenName(String genName) {
        if (genName == null || !genName.toLowerCase().startsWith("gen")) return genName;
        return "Gen" + genName.substring(3);
    }

    private static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(PokeNotifier.MOD_ID)
                .map(ModContainer::getMetadata)
                .map(meta -> meta.getVersion().getFriendlyString())
                .orElse("Unknown");
    }
}