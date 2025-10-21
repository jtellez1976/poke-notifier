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
import com.zehro_mc.pokenotifier.client.PokeNotifierClient;
import com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload;
import com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload;
import com.zehro_mc.pokenotifier.networking.UpdateSourcePayload;
import net.minecraft.client.gui.screen.Screen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;


/**
 * Registers and handles all client-side commands, which start with /pnc.
 */
public class ClientCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        LiteralArgumentBuilder<FabricClientCommandSource> pncCommand = ClientCommandManager.literal("pnc");

        // --- FIX: Create a client-side command to handle the update source selection ---
        // This command is what the clickable text will now execute.
        // It's hidden because it's only meant to be called by the GUI.
        pncCommand.then(ClientCommandManager.literal("update")
                .then(ClientCommandManager.argument("source", StringArgumentType.string())
                        .suggests((context, builder) -> CommandSource.suggestMatching(Stream.of("modrinth", "curseforge", "none"), builder))
                        .executes(context -> {
                            String source = StringArgumentType.getString(context, "source");
                            // Send a packet to the server with the chosen source.
                            ClientPlayNetworking.send(new UpdateSourcePayload(source));
                            context.getSource().sendFeedback(Text.literal("Setting update source to: ").formatted(Formatting.YELLOW)
                                    .append(Text.literal(source).formatted(Formatting.GOLD)));
                            return 1;
                        })).requires(source -> false) // Hide from chat suggestions
        );

        // --- Alerts Subcommand ---
        // Hidden because it's managed by the GUI.
        var alertsNode = ClientCommandManager.literal("alerts")
                .requires(source -> false)
                .then(buildCommandToggle("sound", "Alert Sounds", (config, enabled) -> config.alert_sounds_enabled = enabled))
                .then(buildCommandToggle("toast", "HUD (Toast) alerts", (config, enabled) -> config.alert_toast_enabled = enabled))
                .then(buildCommandToggle("chat", "Chat alerts", (config, enabled) -> config.alert_chat_enabled = enabled));

        pncCommand.then(ClientCommandManager.literal("silent")
                .requires(source -> false) // Hide from chat
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

        // --- Custom Catch Subcommand - HIDDEN ---
        var customcatchNode = ClientCommandManager.literal("customcatch")
                .requires(source -> false) // Hide from chat suggestions
                .executes(context -> 0) // Dummy executor to make the node valid.
                .then(ClientCommandManager.literal("add")
                        .requires(source -> false) // Hide from chat suggestions
                        .then(ClientCommandManager.argument("pokemon", StringArgumentType.greedyString())
                                .suggests(pokemonSuggestionProvider)
                                .executes(context -> {
                                    String pokemonName = StringArgumentType.getString(context, "pokemon");
                                    ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.ADD, pokemonName));
                                    context.getSource().sendFeedback(Text.literal("Request sent to add ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" to your custom list.").formatted(Formatting.YELLOW));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("remove")
                        .requires(source -> false) // Hide from chat suggestions
                        .then(ClientCommandManager.argument("pokemon", StringArgumentType.greedyString())
                                .suggests(pokemonSuggestionProvider)
                                .executes(context -> {
                                    String pokemonName = StringArgumentType.getString(context, "pokemon");
                                    ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.REMOVE, pokemonName));
                                    context.getSource().sendFeedback(Text.literal("Request sent to remove ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" from your custom list.").formatted(Formatting.YELLOW));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("view")
                        .requires(source -> false) // Hide from chat suggestions
                        .executes(context -> {
                            ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.LIST, ""));
                            context.getSource().sendFeedback(Text.literal("Requesting your custom list from the server...").formatted(Formatting.YELLOW));
                            return 1;
                        }))
                .then(ClientCommandManager.literal("clear")
                        .requires(source -> false) // Hide from chat suggestions
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

        // Hidden because it's managed by the GUI.
        var catchemallCommand = ClientCommandManager.literal("catchemall")
                .requires(source -> false)
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

        // Hidden because it's managed by the GUI.
        pncCommand.then(ClientCommandManager.literal("version")
                .requires(source -> false)
                .executes(context -> {
                    String modVersion = FabricLoader.getInstance()
                            .getModContainer(PokeNotifier.MOD_ID)
                            .map(ModContainer::getMetadata)
                            .map(meta -> meta.getVersion().getFriendlyString())
                            .orElse("Unknown");

                    List<Text> lines = new ArrayList<>(List.of(Text.literal("Poke Notifier ver. " + modVersion).formatted(Formatting.AQUA))); // FIX: Ensure mutable list
                    if (MinecraftClient.getInstance().currentScreen instanceof PokeNotifierCustomScreen screen) {
                        screen.displayResponse(lines);
                    } else {
                        lines.forEach(context.getSource()::sendFeedback);
                    }
                    return 1;
                }));

        // Hidden because it's managed by the GUI.
        pncCommand.then(ClientCommandManager.literal("status")
                .requires(source -> false)
                .executes(context -> {
                    ConfigClient config = ConfigManager.getClientConfig();
                    List<Text> statusLines = new ArrayList<>();
                    statusLines.add(Text.literal("--- Poke Notifier Status ---").formatted(Formatting.GOLD));
                    statusLines.add(createStatusLine("Searching", config.searching_enabled));
                    statusLines.add(createStatusLine("Silent Mode", config.silent_mode_enabled));
                    statusLines.add(Text.literal("----------------------------").formatted(Formatting.GOLD));
                    statusLines.add(createStatusLine("  Alert Sounds", config.alert_sounds_enabled));
                    statusLines.add(createStatusLine("  Chat Alerts", config.alert_chat_enabled));
                    statusLines.add(createStatusLine("  Toast Alerts (HUD)", config.alert_toast_enabled));

                    // Instead of sending to chat, update the GUI if it's open
                    if (MinecraftClient.getInstance().currentScreen instanceof PokeNotifierCustomScreen screen) {
                        screen.displayResponse(statusLines);
                    } else {
                        statusLines.forEach(context.getSource()::sendFeedback);
                    }
                    return 1;
                }));

        // --- Help Command ---
        // Hidden because it's managed by the GUI.
        pncCommand.then(ClientCommandManager.literal("help")
                .requires(source -> false)
                .executes(context -> {
                    List<Text> helpLines = new ArrayList<>();
                    helpLines.add(Text.literal("--- Poke Notifier Help (v" + getModVersion() + ") ---").formatted(Formatting.GOLD));
                    helpLines.add(Text.literal("/pnc status").formatted(Formatting.AQUA).append(Text.literal(" - Shows your current settings.").formatted(Formatting.GRAY)));
                    helpLines.add(Text.literal("/pnc silent <on|off>").formatted(Formatting.AQUA).append(Text.literal(" - Master switch for all alerts.").formatted(Formatting.GRAY)));
                    helpLines.add(Text.literal(" "));
                    helpLines.add(Text.literal("Custom Catch: /pnc customcatch <add|remove|view|clear> [pokemon]").formatted(Formatting.YELLOW));
                    helpLines.add(Text.literal("Catch 'em All: /pnc catchemall <enable|disable|status> [gen]").formatted(Formatting.YELLOW));

                    if (MinecraftClient.getInstance().currentScreen instanceof PokeNotifierCustomScreen screen) {
                        screen.displayResponse(helpLines);
                    } else {
                        helpLines.forEach(context.getSource()::sendFeedback);
                    }

                    return 1;
                }));

        // --- FIX: Re-implement the client-side GUI command ---
        // Instead of opening the screen directly, it executes the server-side command,
        // which then sends a packet back to the client. This is the most robust pattern.
        var guiCommand = ClientCommandManager.literal("gui")
                .executes(context -> {
                    context.getSource().getPlayer().networkHandler.sendChatCommand("pokenotifier gui");
                    return 1;
                });

        // --- NEW: Internal command for the GUI to set text field values ---
        var internalCommand = ClientCommandManager.literal("internal")
                .requires(source -> false) // Hide from chat
                .then(ClientCommandManager.literal("set_gui_text")
                        .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String text = StringArgumentType.getString(context, "text");
                                    // FIX: Execute on the main client thread to avoid race conditions
                                    context.getSource().getClient().execute(() -> {
                                        if (context.getSource().getClient().currentScreen instanceof PokeNotifierCustomScreen screen) {
                                            screen.setPokemonNameField(text);
                                        }
                                    });
                                    return 1;
                                })));
        pncCommand.then(internalCommand);

        // --- NEW: Waypoint command for Xaero's integration ---
        var waypointCommand = ClientCommandManager.literal("addwaypoint")
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                        .then(ClientCommandManager.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                .then(ClientCommandManager.argument("y", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .then(ClientCommandManager.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                                .executes(context -> {
                                                    String name = StringArgumentType.getString(context, "name");
                                                    int x = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "x");
                                                    int y = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "y");
                                                    int z = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "z");
                                                    
                                                    if (com.zehro_mc.pokenotifier.client.compat.XaeroWaypointIntegration.addWaypoint(name, x, y, z)) {
                                                        context.getSource().sendFeedback(Text.literal("Waypoint '" + name + "' added successfully!").formatted(Formatting.GREEN));
                                                    } else {
                                                        context.getSource().sendFeedback(Text.literal("Failed to add waypoint. Xaero's minimap not found.").formatted(Formatting.RED));
                                                    }
                                                    return 1;
                                                })))));

        // Register all subcommands
        pncCommand.then(alertsNode);
        pncCommand.then(customcatchNode);
        pncCommand.then(catchemallCommand);
        pncCommand.then(waypointCommand);

        // Redirects server-side commands to be accessible via /pnc for convenience.
        var serverCommandNode = dispatcher.getRoot().getChild("pokenotifier");
        if (serverCommandNode != null) {
            pncCommand.then(ClientCommandManager.literal("reloadconfig").redirect(serverCommandNode));
        }
        pncCommand.then(guiCommand);

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

    private static MutableText createStatusLine(String label, boolean isEnabled) {
        MutableText message = Text.literal(label + " = ").formatted(Formatting.WHITE);
        if (isEnabled) {
            message.append(Text.literal("ON").formatted(Formatting.GREEN));
        } else {
            message.append(Text.literal("OFF").formatted(Formatting.RED));
        }
        return message;
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