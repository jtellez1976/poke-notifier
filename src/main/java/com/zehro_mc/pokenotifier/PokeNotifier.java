/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.Cobblemon;
import com.zehro_mc.pokenotifier.block.ModBlocks;
import com.zehro_mc.pokenotifier.block.entity.ModBlockEntities;
import com.zehro_mc.pokenotifier.component.ModDataComponents;
import com.zehro_mc.pokenotifier.api.PokeNotifierApi;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.event.EvolutionListener;
import com.zehro_mc.pokenotifier.item.ModItems;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.event.CaptureListener;
import com.zehro_mc.pokenotifier.networking.*;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zehro_mc.pokenotifier.networking.ServerDebugStatusPayload;
import com.zehro_mc.pokenotifier.util.PokeNotifierServerUtils;
import com.zehro_mc.pokenotifier.util.PlayerRankManager;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import kotlin.Unit;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import net.minecraft.command.argument.GameProfileArgumentType;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.concurrent.ConcurrentHashMap;

public class PokeNotifier implements ModInitializer {
    public static final String MOD_ID = "poke-notifier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier WAYPOINT_CHANNEL_ID = Identifier.of(MOD_ID, "waypoint_payload");
    public static final Identifier STATUS_UPDATE_CHANNEL_ID = Identifier.of(MOD_ID, "status_update_payload");

    public static final Map<PokemonEntity, RarityUtil.RarityCategory> TRACKED_POKEMON = new ConcurrentHashMap<>();

    private static final List<Runnable> PENDING_TASKS = new ArrayList<>();
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        // --- Initialization Banner ---
        String modVersion = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(ModContainer::getMetadata)
                .map(meta -> meta.getVersion().getFriendlyString())
                .orElse("Unknown");

        LOGGER.info("+---------------------------------------------------+");
        LOGGER.info(createBannerLine(""));
        LOGGER.info(createBannerLine("Initializing Poke Notifier"));
        LOGGER.info(createBannerLine("v" + modVersion));
        LOGGER.info(createBannerLine(""));
        LOGGER.info("+---------------------------------------------------+");

        // Register C2S payload types.
        PayloadTypeRegistry.playC2S().register(CustomListUpdatePayload.ID, CustomListUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CatchemallUpdatePayload.ID, CatchemallUpdatePayload.CODEC);

        // Register S2C payload types.
        LOGGER.info("| Phase 1/5: Registering Network Payloads...      |");
        PayloadTypeRegistry.playS2C().register(WaypointPayload.ID, WaypointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatusUpdatePayload.ID, StatusUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDebugStatusPayload.ID, ServerDebugStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CurrentGenPayload.ID, CurrentGenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CatchProgressPayload.ID, CatchProgressPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GlobalAnnouncementPayload.ID, GlobalAnnouncementPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModeStatusPayload.ID, ModeStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RankSyncPayload.ID, RankSyncPayload.CODEC);

        LOGGER.info("| Phase 2/5: Loading Configurations...              |");
        try {
            ConfigManager.loadConfig();
        } catch (ConfigManager.ConfigReadException e) {
            LOGGER.error("Failed to load Poke Notifier configuration on startup. Using default values.", e);
        }

        LOGGER.info("| Phase 3/5: Registering Components & Items...      |");
        ModDataComponents.registerModDataComponents();
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();

        // Register server-side packet receivers.
        LOGGER.info("| Phase 4/5: Registering Commands & Listeners...   |");
        registerServerPacketReceivers();

        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> server = startedServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> server = null);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // --- Command Refactoring ---
            // All commands are now registered here for better organization.
            var pokenotifierNode = CommandManager.literal("pokenotifier")
                    .requires(source -> source.hasPermissionLevel(2));

            // Server status command.
            var statusCommand = CommandManager.literal("status")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        context.getSource().sendFeedback(() -> Text.literal("--- Poke Notifier Server Status ---").formatted(Formatting.GOLD), false);
                        context.getSource().sendFeedback(() -> createServerStatusLine("Debug Mode", config.debug_mode_enabled), false);
                        context.getSource().sendFeedback(() -> createServerStatusLine("Test Mode", config.enable_test_mode), false);
                        return 1;
                    }).build();

            // Help command for admins
            var helpCommand = CommandManager.literal("help")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(() -> Text.literal("--- Poke Notifier Admin Help ---").formatted(Formatting.GOLD), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier status").formatted(Formatting.AQUA).append(Text.literal(" - Shows server config status.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier config reload").formatted(Formatting.AQUA).append(Text.literal(" - Reloads all config files.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier config reset").formatted(Formatting.AQUA).append(Text.literal(" - Generates new default configs.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier test debug <enable/disable>").formatted(Formatting.AQUA).append(Text.literal(" - Toggles detailed console logs.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier test mode <enable/disable>").formatted(Formatting.AQUA).append(Text.literal(" - Toggles notifications for non-natural spawns.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier test spawn <pokemon> [shiny]").formatted(Formatting.AQUA).append(Text.literal(" - Spawns a Pokémon for testing.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier data autocomplete <player> <gen>").formatted(Formatting.AQUA).append(Text.literal(" - Autocompletes a gen for a player.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier data rollback <player>").formatted(Formatting.AQUA).append(Text.literal(" - Restores a player's progress from a backup.").formatted(Formatting.WHITE)), false);
                        return 1;
                    }).build();

            // Config subcommands
            var configNode = CommandManager.literal("config")
                    .then(CommandManager.literal("reload").executes(PokeNotifier::executeReload))
                    .then(CommandManager.literal("reset").executes(PokeNotifier::executeReset))
                    .build();

            // Test mode command.
            var testModeNode = CommandManager.literal("mode")
                    .then(CommandManager.literal("enable").executes(context -> {
                        ConfigManager.getServerConfig().enable_test_mode = true;
                        ConfigManager.saveServerConfigToFile();
                        context.getSource().sendFeedback(() -> Text.literal("Test mode enabled. Non-natural spawns will now be notified.").formatted(Formatting.GREEN), true);
                        return 1;
                    }))
                    .then(CommandManager.literal("disable").executes(context -> {
                        ConfigManager.getServerConfig().enable_test_mode = false;
                        ConfigManager.saveServerConfigToFile();
                        context.getSource().sendFeedback(() -> Text.literal("Test mode disabled. Only natural spawns will be notified.").formatted(Formatting.RED), true);
                        return 1;
                    })).build();

            // Debug mode command
            var debugNode = CommandManager.literal("debug")
                    .then(CommandManager.literal("enable").executes(context -> {
                        ConfigManager.getServerConfig().debug_mode_enabled = true;
                        ConfigManager.saveServerConfigToFile();
                        context.getSource().sendFeedback(() -> Text.literal("Debug mode enabled. Verbose logging is now ON.").formatted(Formatting.GREEN), true);
                        return 1;
                    }))
                    .then(CommandManager.literal("disable").executes(context -> {
                        ConfigManager.getServerConfig().debug_mode_enabled = false;
                        ConfigManager.saveServerConfigToFile();
                        context.getSource().sendFeedback(() -> Text.literal("Debug mode disabled. Verbose logging is now OFF.").formatted(Formatting.RED), true);
                        return 1;
                    })).build();

            // Test command to spawn Pokémon.
            SuggestionProvider<ServerCommandSource> pokemonSuggestionProvider = (context, builder) ->
                    CommandSource.suggestMatching(PokeNotifierApi.getAllPokemonNames(), builder);

            var testSpawnNode = CommandManager.literal("spawn")
                    .then(CommandManager.argument("pokemon", StringArgumentType.string())
                            .suggests(pokemonSuggestionProvider)
                            .executes(context -> {
                                return executeTestSpawn(context.getSource().getPlayer(), StringArgumentType.getString(context, "pokemon"), false);
                            }))
                    .then(CommandManager.literal("shiny")
                            .executes(context -> {
                                return executeTestSpawn(context.getSource().getPlayer(), StringArgumentType.getString(context, "pokemon"), true);
                            })).build();

            // Command to autocomplete generations for testing.
            SuggestionProvider<ServerCommandSource> autoCompleteSuggestionProvider = (context, builder) -> {
                try {
                    Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
                    if (profiles.isEmpty()) {
                        return Suggestions.empty(); // No player typed yet, no suggestions.
                    }
                    // We assume only one player is selected
                    PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(profiles.iterator().next().getId());
                    return CommandSource.suggestMatching(progress.active_generations, builder);
                } catch (CommandSyntaxException e) {
                    // This can happen while the user is typing; just return no suggestions.
                    return Suggestions.empty();
                }
            };

            // Data management commands
            var autoCompleteGenNode = CommandManager.literal("autocomplete")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .suggests((context, builder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder))
                            .then(CommandManager.argument("generation", StringArgumentType.string())
                                    .suggests(autoCompleteSuggestionProvider)
                                    .executes(context -> {
                                        GameProfile profile = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
                                        String genName = StringArgumentType.getString(context, "generation");
                                        ServerPlayerEntity targetPlayer = context.getSource().getServer().getPlayerManager().getPlayer(profile.getId());

                                        if (targetPlayer == null) {
                                            context.getSource().sendError(Text.literal("Player " + profile.getName() + " is not online."));
                                            return 0;
                                        }

                                        GenerationData genData = ConfigManager.getGenerationData(genName);
                                        if (genData == null) {
                                            context.getSource().sendError(Text.literal("Generation '" + genName + "' not found."));
                                            return 0;
                                        }

                                        // Check if the player has Catch 'em All mode active.
                                        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(targetPlayer.getUuid());
                                        if (progress.active_generations.isEmpty()) {
                                            context.getSource().sendError(Text.literal("Player " + profile.getName() + " does not have Catch 'em All mode active.").formatted(Formatting.RED));
                                            return 0;
                                        }

                                        // Check if the player is already tracking a different generation.
                                        if (!progress.active_generations.isEmpty() && !progress.active_generations.contains(genName)) {
                                            String activeGen = progress.active_generations.iterator().next();
                                            context.getSource().sendError(Text.literal("Player " + profile.getName() + " is already tracking " + formatGenName(activeGen) + ". They must disable it first.").formatted(Formatting.RED));
                                            return 0;
                                        }

                                        // --- LÓGICA DE BACKUP ---
                                        File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, targetPlayer.getUuid().toString() + ".json");
                                        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, targetPlayer.getUuid().toString() + ".json.bak");

                                        // Only create a backup if one doesn't already exist.
                                        try {
                                            if (progressFile.exists() && !backupFile.exists()) {
                                                Files.copy(progressFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                                context.getSource().sendFeedback(() -> Text.literal("Backup of original progress created.").formatted(Formatting.YELLOW), false);
                                            }
                                        } catch (IOException e) {
                                            context.getSource().sendError(Text.literal("Failed to create backup file. Aborting."));
                                            LOGGER.error("Failed to create backup for " + profile.getName(), e);
                                            return 0;
                                        }

                                        context.getSource().sendFeedback(() -> Text.literal("WARNING: This command modifies player data directly. Use with caution.").formatted(Formatting.RED), false);
                                        String missingPokemon = autocompleteGenerationForPlayer(targetPlayer, genName, genData);
                                        context.getSource().sendFeedback(() -> Text.literal("Autocompleted " + formatGenName(genName) + " for player " + targetPlayer.getName().getString()).formatted(Formatting.GREEN), true);
                                        context.getSource().sendFeedback(() -> Text.literal("To complete the list, capture ").append(Text.literal(missingPokemon).formatted(Formatting.GOLD)).append(". Use '/pokenotifier testspawn " + missingPokemon + "' to test.").formatted(Formatting.AQUA), false);
                                        return 1;
                                    }))
                    ).build();

            // Command to restore player progress from a backup.
            var rollbackNode = CommandManager.literal("rollback")
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .suggests((context, builder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder))
                            .executes(context -> {
                                GameProfile profile = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
                                ServerPlayerEntity targetPlayer = context.getSource().getServer().getPlayerManager().getPlayer(profile.getId());

                                if (targetPlayer == null) {
                                    context.getSource().sendError(Text.literal("Player " + profile.getName() + " is not online."));
                                    return 0;
                                }

                                boolean success = rollbackPlayerProgress(targetPlayer);
                                if (success) {
                                    context.getSource().sendFeedback(() -> Text.literal("Successfully rolled back progress for " + profile.getName()).formatted(Formatting.GREEN), true);
                                } else {
                                    context.getSource().sendError(Text.literal("No backup file found for " + profile.getName() + "."));
                                }
                                return success ? 1 : 0;
                            })).build();

            // Build the command tree
            pokenotifierNode.then(statusCommand);
            pokenotifierNode.then(helpCommand);
            pokenotifierNode.then(configNode);
            pokenotifierNode.then(CommandManager.literal("test")
                    .then(testModeNode)
                    .then(debugNode)
                    .then(testSpawnNode));
            pokenotifierNode.then(CommandManager.literal("data")
                    .then(autoCompleteGenNode)
                    .then(rollbackNode));

            dispatcher.register(pokenotifierNode);
        });

        LOGGER.info("| Phase 5/5: Subscribing to Game Events...         |");
        // On player join, perform initial syncs and check for rank effects.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            performInitialPcSync(player);
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);
            PlayerRankManager.onPlayerJoin(player);
        });

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            RarePokemonNotifier.onPokemonSpawn(event.getEntity());
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            TRACKED_POKEMON.keySet().removeIf(entity -> entity.getPokemon().getUuid().equals(event.getPokemon().getUuid()));
            CaptureListener.onPokemonCaptured(event);
            PokeNotifierServerUtils.sendCatchProgressUpdate(event.getPlayer());
            return Unit.INSTANCE;
        });

        // New listener for evolutions, using the correct event.
        EvolutionListener.register();

        ServerTickEvents.END_SERVER_TICK.register(currentServer -> {
            TRACKED_POKEMON.entrySet().removeIf(entry -> {
                PokemonEntity pokemonEntity = entry.getKey();
                if (!pokemonEntity.isAlive() || pokemonEntity.isRemoved()) {
                    Pokemon pokemon = pokemonEntity.getPokemon();
                    RarityUtil.RarityCategory rarity = entry.getValue();

                    StatusUpdatePayload payload = new StatusUpdatePayload(
                            pokemon.getUuid().toString(),
                            pokemon.getDisplayName().getString(),
                            rarity.name(),
                            StatusUpdatePayload.UpdateType.DESPAWNED,
                            null
                    );

                    if (server != null) {
                        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                            ServerPlayNetworking.send(player, payload);
                        }
                    }
                    return true;
                }
                return false;
            });
            // Process any scheduled tasks.
            if (!PENDING_TASKS.isEmpty()) {
                for (Runnable task : new ArrayList<>(PENDING_TASKS)) {
                    task.run();
                }
                PENDING_TASKS.clear();
            }
        });

        LOGGER.info("+---------------------------------------------------+");
        LOGGER.info("|         Poke Notifier successfully loaded!        |");
        LOGGER.info("+---------------------------------------------------+");
    }

    /**
     * Registers receivers for packets sent from the client to the server.
     */
    private static void registerServerPacketReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(CustomListUpdatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String pokemonName = payload.pokemonName().toLowerCase().trim();

            context.server().execute(() -> {
                CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());

                switch (payload.action()) {
                    case ADD:
                        // Use the property parser for robust validation (e.g., handles 'mr-mime' and 'mr_mime').
                        try {
                            PokemonProperties.Companion.parse(pokemonName);
                        } catch (Exception e) {
                            player.sendMessage(Text.literal("Error: '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' is not a valid Pokémon name.").formatted(Formatting.RED), false);
                            return;
                        }
                        if (playerConfig.tracked_pokemon.add(pokemonName)) {
                            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
                            player.sendMessage(Text.literal("Added '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' to your custom tracking list.").formatted(Formatting.GREEN), false);
                        } else {
                            player.sendMessage(Text.literal("Pokémon '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' is already on your list.").formatted(Formatting.YELLOW), false);
                        }
                        break;

                    case REMOVE:
                        if (playerConfig.tracked_pokemon.remove(pokemonName)) {
                            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
                            player.sendMessage(Text.literal("Removed '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' from your custom tracking list.").formatted(Formatting.GREEN), false);
                        } else {
                            player.sendMessage(Text.literal("Pokémon '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' was not on your list.").formatted(Formatting.YELLOW), false);
                        }
                        break;

                    case LIST:
                        if (playerConfig.tracked_pokemon.isEmpty()) {
                            player.sendMessage(Text.literal("Your custom tracking list is empty.").formatted(Formatting.YELLOW), false);
                        } else {
                            player.sendMessage(Text.literal("Your custom tracking list:").formatted(Formatting.YELLOW), false);
                            playerConfig.tracked_pokemon.forEach(name -> player.sendMessage(Text.literal("- " + name).formatted(Formatting.GOLD), false));
                        }
                        break;

                    case CLEAR:
                        if (!playerConfig.tracked_pokemon.isEmpty()) {
                            playerConfig.tracked_pokemon.clear();
                            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
                            player.sendMessage(Text.literal("Your custom tracking list has been cleared.").formatted(Formatting.GREEN), false);
                        } else {
                            player.sendMessage(Text.literal("Your custom tracking list was already empty.").formatted(Formatting.YELLOW), false);
                        }
                        break;
                }
            });
        });

        // Handle "Catch 'em All" mode updates from the client.
        ServerPlayNetworking.registerGlobalReceiver(CatchemallUpdatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String genName = payload.generationName().toLowerCase().trim();

            context.server().execute(() -> {
                var progress = ConfigManager.getPlayerCatchProgress(player.getUuid());

                switch (payload.action()) {
                    case ENABLE:
                        GenerationData genData = ConfigManager.getGenerationData(genName);
                        if (genData == null) {
                            player.sendMessage(Text.literal("Error: Generation '" + genName + "' not found.").formatted(Formatting.RED), false);
                            return;
                        }
                        // Allow only one active generation at a time.
                        if (progress.active_generations.contains(genName)) {
                            String regionName = formatRegionName(genData.region);
                            ServerPlayNetworking.send(player, new ModeStatusPayload("Already Tracking: " + regionName, true));
                            return;
                        }

                        if (!progress.active_generations.isEmpty()) {
                            String oldGen = progress.active_generations.iterator().next();
                            player.sendMessage(Text.literal("Stopped tracking " + formatGenName(oldGen) + ".").formatted(Formatting.YELLOW), false);
                        }
                        progress.active_generations.clear();
                        progress.active_generations.add(genName);
                        ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
                        String regionName = formatRegionName(genData.region);

                        ServerPlayNetworking.send(player, new ModeStatusPayload("Tracking: " + regionName, true));
                        PokeNotifierServerUtils.sendCatchProgressUpdate(player);
                        break;

                    case DISABLE:
                        if (progress.active_generations.remove(genName)) {
                            ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
                            ServerPlayNetworking.send(player, new ModeStatusPayload("Tracking Disabled", false));
                            PokeNotifierServerUtils.sendCatchProgressUpdate(player); // Update to hide the HUD
                        } else {
                            ServerPlayNetworking.send(player, new ModeStatusPayload("Was not tracking", false));
                        }
                        break;

                    case LIST:
                        if (progress.active_generations.isEmpty()) {
                            player.sendMessage(Text.literal("You are not tracking any generation for Catch 'em All mode.").formatted(Formatting.YELLOW), false);
                        } else {
                            player.sendMessage(Text.literal("You are currently tracking the following generations:").formatted(Formatting.YELLOW), false);
                            progress.active_generations.forEach(gen -> {
                                GenerationData data = ConfigManager.getGenerationData(gen);
                                String regionNameForList = data != null ? formatRegionName(data.region) : "Unknown";
                                player.sendMessage(Text.literal("- " + formatGenName(gen) + " (" + regionNameForList + ")").formatted(Formatting.GOLD), false);
                            });
                        }
                        break;
                }
            });
        });
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

    private int executeTestSpawn(ServerPlayerEntity player, String pokemonName, boolean isShiny) {
        // The test spawn command should only work if test_mode is enabled.
        if (!ConfigManager.getServerConfig().enable_test_mode) {
            player.sendMessage(Text.literal("Test Mode is disabled. Please enable it first with '/pokenotifier test_mode enable'.").formatted(Formatting.RED), false);
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        final String finalPokemonName = pokemonName.toLowerCase().trim();

        // Strict validation: the name must exist in Cobblemon's official list.
        if (PokeNotifierApi.getAllPokemonNames().noneMatch(name -> name.equals(finalPokemonName))) {
            player.sendMessage(Text.literal("Error: Pokémon '").append(Text.literal(finalPokemonName).formatted(Formatting.GOLD)).append("' is not a valid Pokémon name.").formatted(Formatting.RED), false);
            return 0;
        }

        try {
            PokemonProperties props = PokemonProperties.Companion.parse(finalPokemonName);
            if (isShiny) {
                props.setShiny(true);
            }

            PokemonEntity pokemonEntity = props.createEntity(world);
            pokemonEntity.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());

            // Add a custom NBT tag to identify this as a test spawn.
            pokemonEntity.getPokemon().getPersistentData().putBoolean("pokenotifier_test_spawn", true);

            world.spawnEntity(pokemonEntity);

            // Manually trigger our own notification event, as world.spawnEntity does not.
            RarePokemonNotifier.onPokemonSpawn(pokemonEntity);

            player.sendMessage(Text.literal("Spawned a " + (isShiny ? "Shiny " : "") + finalPokemonName + ".").formatted(Formatting.GREEN), false);
            return 1;
        } catch (Exception e) {
            player.sendMessage(Text.literal("Error: Pokémon '" + finalPokemonName + "' not found.").formatted(Formatting.RED), false);
            return 0;
        }
    }

    private static String formatGenName(String genName) {
        if (genName == null || !genName.toLowerCase().startsWith("gen")) return genName;
        return "Gen" + genName.substring(3);
    }

    private static String formatRegionName(String regionName) {
        if (regionName == null || regionName.isEmpty()) return "Unknown";
        return regionName.substring(0, 1).toUpperCase() + regionName.substring(1);
    }

    /**
     * Performs the initial sync of a player's PC and party with their "Catch 'em All" progress.
     * This operation only runs once per player.
     * @param player The player to sync.
     */
    private static void performInitialPcSync(ServerPlayerEntity player) {
        var progress = ConfigManager.getPlayerCatchProgress(player.getUuid());

        if (progress.initialPcSyncCompleted) {
            return;
        }

        LOGGER.info("Performing initial PC sync for player: " + player.getName().getString());

        PokemonStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PokemonStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);

        Stream<Pokemon> partyStream = StreamSupport.stream(party.spliterator(), false);
        Stream<Pokemon> pcStream = StreamSupport.stream(pc.spliterator(), false);
        Stream.concat(partyStream, pcStream).forEach(pokemon -> {
            if (pokemon == null) return;
            String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();

            for (int i = 1; i <= 9; i++) {
                String genId = "gen" + i;
                GenerationData genData = ConfigManager.getGenerationData(genId);
                if (genData != null && genData.pokemon.contains(pokemonName)) {
                    progress.caught_pokemon.computeIfAbsent(genId, k -> new java.util.HashSet<>()).add(pokemonName);
                }
            }
        });

        progress.initialPcSyncCompleted = true;
        ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);

        player.sendMessage(Text.literal("[Poke Notifier] Your Pokédex has been synchronized with the 'Catch 'em All' mode!").formatted(Formatting.GREEN), false);
        LOGGER.info("Initial PC sync completed for player: " + player.getName().getString());
    }

    private static String autocompleteGenerationForPlayer(ServerPlayerEntity player, String genName, GenerationData genData) {
        PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());

        List<String> pokemonList = new ArrayList<>(genData.pokemon);
        if (pokemonList.isEmpty()) {
            return "none";
        }

        String lastPokemon = pokemonList.remove(pokemonList.size() - 1);

        progress.caught_pokemon.put(genName, new HashSet<>(pokemonList));
        ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);

        player.sendMessage(Text.literal("An administrator has autocompleted your " + formatGenName(genName) + " progress for testing purposes.").formatted(Formatting.YELLOW), false);
        PokeNotifierServerUtils.sendCatchProgressUpdate(player);
        return lastPokemon;
    }

    private static boolean rollbackPlayerProgress(ServerPlayerEntity player) {
        File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json");
        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json.bak");

        if (!backupFile.exists()) {
            return false;
        }

        try {
            Files.move(backupFile.toPath(), progressFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            // Clear the player's progress from the cache. The next time their data is needed,
            // it will be re-read from the restored file and migrated correctly.
            ConfigManager.forceReloadPlayerCatchProgress(player.getUuid());
            PlayerRankManager.updateAndSyncRank(player);

            // --- FIX: Force a progress update to the client to refresh the HUD immediately ---
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);

            player.getInventory().remove(stack -> stack.getItem() instanceof com.zehro_mc.pokenotifier.item.PokedexTrophyItem, -1, player.getInventory());
            player.sendMessage(Text.literal("Your Pokédex Trophies have been removed as part of the rollback.").formatted(Formatting.YELLOW), false);

            player.sendMessage(Text.literal("Your 'Catch 'em All' progress has been restored by an administrator.").formatted(Formatting.GREEN), false);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to rollback progress for player " + player.getName().getString(), e);
            return false;
        }
    }

    public static void scheduleTask(Runnable task) {
        PENDING_TASKS.add(task);
    }

    // --- Centralized Command Logic ---

    private static int executeReload(CommandContext<ServerCommandSource> context) {
        try {
            ConfigManager.loadConfig();
            context.getSource().sendFeedback(() -> Text.literal("Poke Notifier configurations reloaded successfully.").formatted(Formatting.GREEN), true);
            return 1;
        } catch (ConfigManager.ConfigReadException e) {
            LOGGER.error("Failed to reload Poke Notifier configuration: {}", e.getMessage());
            context.getSource().sendError(Text.literal("Error reloading configs: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int executeReset(CommandContext<ServerCommandSource> context) {
        try {
            ConfigManager.resetToDefault();
            context.getSource().sendFeedback(() -> Text.literal("New default poke-notifier configs have been generated.").formatted(Formatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to generate new default configurations.", e);
            context.getSource().sendError(Text.literal("Failed to generate new configs. Check server logs.").formatted(Formatting.RED));
            return 0;
        }
    }

    /**
     * Creates a formatted line for the startup banner, centering the text.
     * @param text The text to be included in the banner line.
     * @return A formatted string ready for logging.
     */
    private static String createBannerLine(String text) {
        int bannerWidth = 49; // The inner width of the banner (51 total chars - 2 for '|')
        if (text.isEmpty()) {
            return "|" + " ".repeat(bannerWidth) + "|";
        }
        int padding = bannerWidth - text.length();
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;
        return "| " + " ".repeat(leftPadding) + text + " ".repeat(rightPadding) + " |";
    }
}