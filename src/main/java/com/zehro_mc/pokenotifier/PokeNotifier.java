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
import com.zehro_mc.pokenotifier.util.PokeNotifierServerUtils;
import com.zehro_mc.pokenotifier.util.PlayerRankManager;
import com.zehro_mc.pokenotifier.util.UpdateChecker;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import com.zehro_mc.pokenotifier.globalhunt.GlobalHuntManager;
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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.world.Heightmap;
import java.util.stream.StreamSupport;
import java.util.concurrent.ConcurrentHashMap;

public class PokeNotifier implements ModInitializer {
    public static final String MOD_ID = "poke-notifier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Map<PokemonEntity, RarityUtil.RarityCategory> TRACKED_POKEMON = new ConcurrentHashMap<>();

    // --- Bounty System Scheduler ---
    private static int bountyTickCounter = 0;
    private static final Random BOUNTY_RANDOM = new Random();
    private static int bountyReminderTickCounter = 0;
    private static long bountyStartTime = 0L;

    // --- Swarm System Scheduler ---
    private static int swarmTickCounter = 0;
    private static final Random SWARM_RANDOM = new Random();

    // --- Rival System Cooldowns ---
    public static final Map<UUID, Long> RIVAL_NOTIFICATION_COOLDOWNS = new ConcurrentHashMap<>();

    // --- Reset Confirmation ---
    private static final Map<UUID, String> RESET_CONFIRMATION_TOKENS = new ConcurrentHashMap<>();

    // --- Update Checker ---
    public static UpdateChecker.UpdateInfo LATEST_VERSION_INFO = null;
    public static boolean UPDATE_CHECK_COMPLETED = false;
    private static final Set<UUID> NOTIFIED_UP_TO_DATE_ADMINS = new HashSet<>();

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

        LOGGER.info("| Phase 2/5: Loading Configurations...              |");
        try {
            ConfigManager.loadConfig();
        } catch (ConfigManager.ConfigReadException e) {
            LOGGER.error("Failed to load Poke Notifier configuration on startup. Using default values.", e);
        }
        
        // Force creation of server config if it doesn't exist
        ConfigManager.getServerConfig();
        ConfigManager.saveConfig();

        // Register networking payloads. This is called on both client and server.
        PokeNotifierPayloads.register();

        // Asynchronously check for updates. The checker itself will handle logging.
        UpdateChecker.checkForUpdates(null).thenRun(() -> {
            UPDATE_CHECK_COMPLETED = true;
        });

        LOGGER.info("| Phase 3/5: Registering Components & Items...      |");
        ModDataComponents.registerModDataComponents();
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();

        // Register server-side packet receivers.
        LOGGER.info("| Phase 4/5: Registering Commands & Listeners...   |");
        registerServerPacketReceivers();

        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
            server = startedServer;
            // Initialize Global Hunt Manager
            GlobalHuntManager.getInstance().initialize(startedServer);
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            // Clear the notified admin list on each server start.
            NOTIFIED_UP_TO_DATE_ADMINS.clear();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
            // Shutdown Global Hunt Manager
            GlobalHuntManager.getInstance().shutdown();
            server = null;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // --- Command Refactoring ---
            // All commands are now registered here for better organization.
            var pokenotifierNode = CommandManager.literal("pokenotifier");

            // Server status command.
            var statusCommand = CommandManager.literal("status")
                    .requires(source -> false) // Hide from chat
                    .executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();                        
                        List<Text> lines = new ArrayList<>();
                        lines.add(Text.literal("--- Poke Notifier Server Status ---").formatted(Formatting.GOLD));
                        lines.add(createServerStatusLine("Debug Mode", config.debug_mode_enabled));
                        lines.add(createServerStatusLine("Bounty System", config.bounty_system_enabled));
                        if (config.bounty_system_enabled) {
                            String currentBounty = getActiveBounty();
                            MutableText bountyStatus = Text.literal("  Current Bounty = ").formatted(Formatting.WHITE);
                            bountyStatus.append(currentBounty == null ? Text.literal("None").formatted(Formatting.GRAY) : Text.literal(currentBounty).formatted(Formatting.GOLD));
                            lines.add(bountyStatus);
                        }
                        lines.add(createServerStatusLine("Test Mode", config.enable_test_mode));
                        ServerPlayNetworking.send(context.getSource().getPlayer(), new GuiResponsePayload(lines));
                        return 1;
                    }).build();

            // Help command for admins
            var helpCommand = CommandManager.literal("help")
                    .requires(source -> false) // Hide from chat
                    .executes(context -> {
                        // FIX: Send response to GUI
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(() -> Text.literal("--- Poke Notifier Admin Help ---").formatted(Formatting.GOLD), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier status").formatted(Formatting.AQUA).append(Text.literal(" - Shows server config status.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier config reload").formatted(Formatting.AQUA).append(Text.literal(" - Reloads all config files.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier config reset").formatted(Formatting.AQUA).append(Text.literal(" - Generates new default configs.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier test debug <enable/disable>").formatted(Formatting.AQUA).append(Text.literal(" - Toggles detailed console logs.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier test mode <enable/disable>").formatted(Formatting.AQUA).append(Text.literal(" - Toggles notifications for non-natural spawns.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier test spawn <pokemon> [shiny]").formatted(Formatting.AQUA).append(Text.literal(" - Spawns a Pokémon for testing.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier data autocomplete <player>").formatted(Formatting.AQUA).append(Text.literal(" - Autocompletes a gen for a player.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier bounty system <enable/disable>").formatted(Formatting.AQUA).append(Text.literal(" - Toggles the automatic bounty system.").formatted(Formatting.WHITE)), false);
                        source.sendFeedback(() -> Text.literal("/pokenotifier data rollback <player>").formatted(Formatting.AQUA).append(Text.literal(" - Restores a player's progress from a backup.").formatted(Formatting.WHITE)), false);
                        return 1;
                    }).build();

            // Config subcommands
            var configNode = CommandManager.literal("config")
                    .requires(source -> false) // Hide from chat
                    .then(CommandManager.literal("reload").executes(PokeNotifier::executeReload));

            // --- MEJORA: Comando de reseteo con confirmación ---
            configNode.then(CommandManager.literal("reset").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                if (player == null) return 0;

                String token = UUID.randomUUID().toString().substring(0, 8);
                RESET_CONFIRMATION_TOKENS.put(player.getUuid(), token);

                Text confirmText = Text.literal("[CONFIRM]").formatted(Formatting.GREEN, Formatting.BOLD)
                        .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/pokenotifier internal confirm_reset " + token)));
                Text cancelText = Text.literal("[CANCEL]").formatted(Formatting.RED, Formatting.BOLD)
                        .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/pokenotifier internal cancel_reset")));

                context.getSource().sendFeedback(() -> Text.literal("WARNING: This will reset all Poke Notifier configurations to their defaults. This action cannot be undone.").formatted(Formatting.RED), false);
                context.getSource().sendFeedback(() -> Text.literal("Click to proceed: ").append(confirmText).append(" ").append(cancelText), false);
                return 1;
            }));

            // Test mode command.
            var testModeNode = CommandManager.literal("mode")
                    .requires(source -> false) // Hide from chat
                    .then(CommandManager.literal("enable").executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.enable_test_mode = true;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Test mode enabled. Non-natural spawns will now be notified.").formatted(Formatting.GREEN)));
                        ServerPlayNetworking.send(context.getSource().getPlayer(), new GuiResponsePayload(lines));
                        return 1;
                    }))
                    .then(CommandManager.literal("disable").executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.enable_test_mode = false;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Test mode disabled. Only natural spawns will be notified.").formatted(Formatting.RED)));
                        ServerPlayNetworking.send(context.getSource().getPlayer(), new GuiResponsePayload(lines));
                        return 1;
                    })).build();

            // Debug mode command
            var debugNode = CommandManager.literal("debug")
                    .requires(source -> false) // Hide from chat
                    .then(CommandManager.literal("enable").executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.debug_mode_enabled = true;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Debug mode enabled. Verbose logging is now ON.").formatted(Formatting.GREEN)));
                        ServerPlayNetworking.send(context.getSource().getPlayer(), new GuiResponsePayload(lines));
                        return 1;
                    }))
                    .then(CommandManager.literal("disable").executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.debug_mode_enabled = false;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Debug mode disabled. Verbose logging is now OFF.").formatted(Formatting.RED)));
                        ServerPlayNetworking.send(context.getSource().getPlayer(), new GuiResponsePayload(lines));
                        return 1;
                    })).build();

            // Bounty system command
            var bountySystemNode = CommandManager.literal("system")
                    .requires(source -> false) // Hide from chat
                    .then(CommandManager.literal("enable").executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.bounty_system_enabled = true;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Automatic Bounty System enabled.").formatted(Formatting.GREEN)));
                        ServerPlayNetworking.send(context.getSource().getPlayer(), new GuiResponsePayload(lines));
                        return 1;
                    }))
                    .then(CommandManager.literal("disable").executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.bounty_system_enabled = false;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Automatic Bounty System disabled.").formatted(Formatting.RED)));
                        ServerPlayNetworking.send(context.getSource().getPlayer(), new GuiResponsePayload(lines));
                        clearActiveBounty(false); // Clear any active bounty without announcement
                        return 1;
                    })).build();

            // Test command to spawn Pokémon.
            SuggestionProvider<ServerCommandSource> pokemonSuggestionProvider = (context, builder) ->
                    CommandSource.suggestMatching(PokeNotifierApi.getAllPokemonNames(), builder);

            var testSpawnNode = CommandManager.literal("spawn")
                    .requires(source -> false) // Hide from chat
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
            var autoCompleteGenNode = CommandManager.literal("autocomplete")
                    .requires(source -> false) // Hide from chat
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .suggests((context, builder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder))
                            .executes(context -> {
                                GameProfile profile = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
                                ServerPlayerEntity targetPlayer = context.getSource().getServer().getPlayerManager().getPlayer(profile.getId());
                                ServerPlayerEntity adminPlayer = context.getSource().getPlayer();

                                if (targetPlayer == null) {
                                    if (adminPlayer != null) ServerPlayNetworking.send(adminPlayer, new GuiResponsePayload(List.of(Text.literal("Player " + profile.getName() + " is not online.").formatted(Formatting.RED))));
                                    return 0;
                                }

                                PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(targetPlayer.getUuid());
                                if (progress.active_generations.isEmpty()) {
                                    if (adminPlayer != null) ServerPlayNetworking.send(adminPlayer, new GuiResponsePayload(List.of(Text.literal("Player " + profile.getName() + " does not have Catch 'em All mode active.").formatted(Formatting.RED))));
                                    return 0;
                                }
                                String genName = progress.active_generations.iterator().next();
                                GenerationData genData = ConfigManager.getGenerationData(genName);
                                if (genData == null) {
                                    if (adminPlayer != null) ServerPlayNetworking.send(adminPlayer, new GuiResponsePayload(List.of(Text.literal("Internal error: Could not find data for generation '" + genName + "'.").formatted(Formatting.RED))));
                                    return 0;
                                }

                                // --- LÓGICA DE BACKUP ---
                                File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, targetPlayer.getUuid().toString() + ".json");
                                File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, targetPlayer.getUuid().toString() + ".json.bak");
                                try {
                                    if (progressFile.exists() && !backupFile.exists()) {
                                        Files.copy(progressFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);                                        if (adminPlayer != null) ServerPlayNetworking.send(adminPlayer, new GuiResponsePayload(List.of(Text.literal("Backup of original progress created.").formatted(Formatting.YELLOW))));
                                    }
                                } catch (IOException e) {
                                    LOGGER.error("Failed to create backup for " + profile.getName(), e);
                                }

                                String missingPokemon = autocompleteGenerationForPlayer(targetPlayer, genName, genData);
                                List<Text> response = new ArrayList<>();
                                response.add(Text.literal("Autocompleted " + formatGenName(genName) + " for player " + targetPlayer.getName().getString()).formatted(Formatting.GREEN));
                                response.add(Text.literal("To complete the list, capture: ").append(Text.literal(missingPokemon).formatted(Formatting.GOLD)).append(". Use '/pokenotifier test spawn " + missingPokemon + "' to test.").formatted(Formatting.AQUA));
                                if (adminPlayer != null) ServerPlayNetworking.send(adminPlayer, new GuiResponsePayload(response));
                                return 1;
                            })
                    ).build();

            // Command to restore player progress from a backup.
            var rollbackNode = CommandManager.literal("rollback")
                    .requires(source -> false) // Hide from chat
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .suggests((context, builder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder))
                            .executes(context -> {
                                GameProfile profile = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
                                ServerPlayerEntity targetPlayer = context.getSource().getServer().getPlayerManager().getPlayer(profile.getId());                                ServerPlayerEntity adminPlayer = context.getSource().getPlayer();

                                if (targetPlayer == null) {
                                    if (adminPlayer != null) ServerPlayNetworking.send(adminPlayer, new GuiResponsePayload(List.of(Text.literal("Player " + profile.getName() + " is not online.").formatted(Formatting.RED))));
                                    return 0;
                                }

                                boolean success = rollbackPlayerProgress(targetPlayer);
                                if (success) {
                                    if (adminPlayer != null) ServerPlayNetworking.send(adminPlayer, new GuiResponsePayload(List.of(Text.literal("Successfully rolled back progress for " + profile.getName()).formatted(Formatting.GREEN))));
                                } else {
                                    if (adminPlayer != null) ServerPlayNetworking.send(adminPlayer, new GuiResponsePayload(List.of(Text.literal("No backup file found for " + profile.getName() + ".").formatted(Formatting.RED))));
                                }
                                return success ? 1 : 0;
                            })).build();

            // Build the command tree
            pokenotifierNode.then(statusCommand);
            pokenotifierNode.then(helpCommand);
            pokenotifierNode.then(configNode);
            pokenotifierNode.then(CommandManager.literal("test") // Main "test" command
                    .requires(source -> false) // Hide from chat
                    .then(testModeNode)
                    .then(debugNode)
                    .then(testSpawnNode));
            pokenotifierNode.then(CommandManager.literal("data")
                    .requires(source -> false) // Hide from chat
                    .then(autoCompleteGenNode)
                    .then(rollbackNode));
            pokenotifierNode.then(CommandManager.literal("bounty") // Main "bounty" command
                    .requires(source -> false) // Hide from chat
                    .then(bountySystemNode));

            // --- MEJORA: Comando para iniciar enjambres (swarms) ---
            var swarmNode = CommandManager.literal("swarm")
                    .requires(source -> false) // Hide from chat
                    .then(CommandManager.literal("start")
                            .then(CommandManager.argument("pokemon", StringArgumentType.string())
                                    .suggests(pokemonSuggestionProvider)
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player == null) return 0;
                                        String pokemonName = StringArgumentType.getString(context, "pokemon").trim();
                                        boolean success = executeSwarmStart(player, pokemonName);
                                        if (success) {
                                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Attempting to start a swarm of ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("...").formatted(Formatting.GREEN)));
                                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                                        }
                                        return success ? 1 : 0;
                                    })
                            )
                    );
            pokenotifierNode.then(swarmNode);

            // --- FIX: GUI command is now a top-level child of the main node ---
            var guiCommand = CommandManager.literal("gui")
                    .requires(source -> source.hasPermissionLevel(0)) // Allow execution from client command
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            ConfigServer config = ConfigManager.getServerConfig();
                            ServerPlayNetworking.send(player, new AdminStatusPayload(
                                    player.hasPermissionLevel(2),
                                    config.debug_mode_enabled,
                                    config.enable_test_mode,
                                    config.bounty_system_enabled,
                                    GlobalHuntManager.getInstance().getConfig().isEnabled()));
                            ServerPlayNetworking.send(player, new OpenGuiPayload());
                        }
                        return 1;
                    });

            // --- FIX: Register internal commands as a hidden subcommand ---
            var internalNode = CommandManager.literal("internal").requires(source -> false)
                    .then(CommandManager.literal("confirm_reset").then(CommandManager.argument("token", StringArgumentType.string()).executes(PokeNotifier::executeConfirmReset)))
                    .then(CommandManager.literal("cancel_reset").executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Configuration reset cancelled.").formatted(Formatting.YELLOW), false);
                        return 1;
                    }));
            pokenotifierNode.then(internalNode);
            pokenotifierNode.then(guiCommand); // Add gui command here

            dispatcher.register(pokenotifierNode);
        });

        LOGGER.info("| Phase 5/5: Subscribing to Game Events...         |");
        // On player join, perform initial syncs and check for rank effects.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            performInitialPcSync(player);
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);

            // --- MEJORA: Notify player on join if a bounty is active ---
            String currentBounty = getActiveBounty();
            if (currentBounty != null) {
                Text message = Text.literal("Psst! There is currently an active bounty for a ").formatted(Formatting.GRAY)
                        .append(Text.literal(currentBounty).formatted(Formatting.GOLD))
                        .append(Text.literal("!").formatted(Formatting.GRAY));
                player.sendMessage(message, false);
            }

            // --- MEJORA: Check for update source configuration ---
            if (player.hasPermissionLevel(2)) {
                if ("unknown".equalsIgnoreCase(ConfigManager.getServerConfig().update_checker_source)) {
                    Text prompt = Text.literal("Please configure the update source from the GUI panel:").formatted(Formatting.YELLOW);
                    Text guiButton = Text.literal("[Open GUI]").formatted(Formatting.GREEN).styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/pnc gui")));

                    player.sendMessage(prompt, false);
                    player.sendMessage(Text.literal("Go to User Tools > Info & Help to configure update source: ").append(guiButton), false);
                } else if (LATEST_VERSION_INFO != null) {
                    // --- MEJORA: Notify admin if a new version is available ---
                    Text updateMessage = Text.literal("A new version of Poke Notifier is available: ").formatted(Formatting.GREEN)
                            .append(Text.literal(LATEST_VERSION_INFO.version()).formatted(Formatting.GOLD));
                    Text downloadLink = Text.literal("[Click here to download]").formatted(Formatting.AQUA, Formatting.UNDERLINE)
                            .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, LATEST_VERSION_INFO.url())));

                    player.sendMessage(updateMessage, false);
                    player.sendMessage(downloadLink, false);
                } else if (UPDATE_CHECK_COMPLETED && !NOTIFIED_UP_TO_DATE_ADMINS.contains(player.getUuid())) {
                    // --- MEJORA: Notify admin that the mod is up to date ---
                    player.sendMessage(Text.literal("Poke Notifier is up to date!").formatted(Formatting.GREEN), false);
                    NOTIFIED_UP_TO_DATE_ADMINS.add(player.getUuid());
                }
            }

            // --- NEW: Sync admin status with the client ---
            ConfigServer config = ConfigManager.getServerConfig();
            ServerPlayNetworking.send(player, new AdminStatusPayload(
                    player.hasPermissionLevel(2),
                    config.debug_mode_enabled,
                    config.enable_test_mode,
                    config.bounty_system_enabled,
                    GlobalHuntManager.getInstance().getConfig().isEnabled()));

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
            
            // Check if this capture is part of a Global Hunt
            if (GlobalHuntManager.getInstance().hasActiveEvent()) {
                var activeEvent = GlobalHuntManager.getInstance().getCurrentEvent();
                if (activeEvent.getSpawnedPokemon() != null && 
                    activeEvent.getSpawnedPokemon().getPokemon().getUuid().equals(event.getPokemon().getUuid())) {
                    activeEvent.onPokemonCaptured(event.getPlayer().getName().getString());
                }
            }
            
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
                            null // Player name is null for despawns
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

            // Tick the bounty system scheduler.
            tickBountySystem(currentServer);

            // Tick the swarm system scheduler.
            tickSwarmSystem(currentServer);
        });

        // --- MEJORA: Print the final success banner synchronously ---
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
                            // FIX: Send error response to GUI
                            List<Text> errorLines = new ArrayList<>(List.of(Text.literal("Error: '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' is not a valid Pokémon name.").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(errorLines));
                            return;
                        }
                        if (playerConfig.tracked_pokemon.add(pokemonName)) {
                            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Added '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' to your custom tracking list.").formatted(Formatting.GREEN)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            PokeNotifierServerUtils.sendCatchProgressUpdate(player); // FIX: Update client state
                        } else {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Pokémon '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' is already on your list.").formatted(Formatting.YELLOW)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                        break;

                    case REMOVE:
                        if (playerConfig.tracked_pokemon.remove(pokemonName)) {
                            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
                            PokeNotifierServerUtils.sendCatchProgressUpdate(player);
                            sendCustomHuntList(player); // FIX: Send the updated list directly.
                        } else {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Pokémon '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' was not on your list.").formatted(Formatting.YELLOW)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                        break;

                    case LIST:
                        sendCustomHuntList(player);
                        break;

                    case CLEAR:
                        if (!playerConfig.tracked_pokemon.isEmpty()) {
                            playerConfig.tracked_pokemon.clear();
                            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Your custom tracking list has been cleared.").formatted(Formatting.GREEN)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            PokeNotifierServerUtils.sendCatchProgressUpdate(player); // FIX: Update client state
                        } else {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Your custom tracking list was already empty.").formatted(Formatting.YELLOW)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                        break;
                }
            });
        });

        // Handle admin commands from the client.
        ServerPlayNetworking.registerGlobalReceiver(AdminCommandPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            
            context.server().execute(() -> {
                // Check if command requires admin permissions
                boolean requiresAdmin = switch (payload.action()) {
                    case HELP, VERSION, STATUS -> false; // These are informational and don't require admin
                    default -> true; // All other commands require admin
                };
                
                // Verify admin permissions for admin-only commands
                if (requiresAdmin && !player.hasPermissionLevel(2)) {
                    List<Text> errorLines = new ArrayList<>(List.of(Text.literal("You don't have permission to use admin commands.").formatted(Formatting.RED)));
                    ServerPlayNetworking.send(player, new GuiResponsePayload(errorLines));
                    return;
                }
                
                switch (payload.action()) {
                    case TOGGLE_DEBUG_MODE -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.debug_mode_enabled = !config.debug_mode_enabled;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Debug mode ").append(config.debug_mode_enabled ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED))));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
                    case TOGGLE_TEST_MODE -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.enable_test_mode = !config.enable_test_mode;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Test mode ").append(config.enable_test_mode ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED))));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
                    case TOGGLE_BOUNTY_SYSTEM -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        config.bounty_system_enabled = !config.bounty_system_enabled;
                        ConfigManager.saveServerConfigToFile();
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Bounty system ").append(config.bounty_system_enabled ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED))));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        if (!config.bounty_system_enabled) {
                            clearActiveBounty(false);
                        }
                    }
                    case SERVER_STATUS -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        List<Text> lines = new ArrayList<>();
                        lines.add(Text.literal("--- Poke Notifier Server Status ---").formatted(Formatting.GOLD));
                        lines.add(createServerStatusLine("Debug Mode", config.debug_mode_enabled));
                        lines.add(createServerStatusLine("Bounty System", config.bounty_system_enabled));
                        if (config.bounty_system_enabled) {
                            String currentBounty = getActiveBounty();
                            MutableText bountyStatus = Text.literal("  Current Bounty = ").formatted(Formatting.WHITE);
                            bountyStatus.append(currentBounty == null ? Text.literal("None").formatted(Formatting.GRAY) : Text.literal(currentBounty).formatted(Formatting.GOLD));
                            lines.add(bountyStatus);
                        }
                        lines.add(createServerStatusLine("Test Mode", config.enable_test_mode));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
                    case RELOAD_CONFIG -> {
                        try {
                            ConfigManager.loadConfig();
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Poke Notifier configurations reloaded successfully.").formatted(Formatting.GREEN)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        } catch (ConfigManager.ConfigReadException e) {
                            LOGGER.error("Failed to reload Poke Notifier configuration: {}", e.getMessage());
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Error reloading configs: " + e.getMessage()).formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                    }
                    case RESET_CONFIG -> {
                        try {
                            ConfigManager.resetToDefault();
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("All Poke Notifier configurations have been reset to default.").formatted(Formatting.GREEN)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        } catch (Exception e) {
                            LOGGER.error("Failed to generate new default configurations.", e);
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Failed to generate new configs. Check server logs.").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                    }
                    case START_SWARM -> {
                        String pokemonName = payload.parameter().trim();
                        if (!pokemonName.isEmpty()) {
                            boolean success = executeSwarmStart(player, pokemonName);
                            if (success) {
                                List<Text> lines = new ArrayList<>(List.of(Text.literal("Attempting to start a swarm of ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("...").formatted(Formatting.GREEN)));
                                ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            }
                        }
                    }
                    case START_GLOBAL_HUNT -> {
                        String[] parts = payload.parameter().trim().split(" ");
                        if (parts.length >= 1) {
                            String pokemonName = parts[0];
                            boolean isShiny = parts.length > 1 && "shiny".equals(parts[1]);
                            
                            if (GlobalHuntManager.getInstance().hasActiveEvent()) {
                                List<Text> lines = new ArrayList<>(List.of(Text.literal("A Global Hunt is already active!").formatted(Formatting.RED)));
                                ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                                return;
                            }
                            
                            // Use the new method that generates challenging coordinates automatically
                            ServerWorld world = player.getServerWorld();
                            
                            GlobalHuntManager.getInstance().startManualEvent(world, pokemonName, isShiny);
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Started Global Hunt for ").append(Text.literal((isShiny ? "Shiny " : "") + pokemonName).formatted(Formatting.GOLD)).formatted(Formatting.GREEN)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                    }
                    case CANCEL_GLOBAL_HUNT -> {
                        if (GlobalHuntManager.getInstance().hasActiveEvent()) {
                            GlobalHuntManager.getInstance().getCurrentEvent().cancel();
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Global Hunt cancelled by admin").formatted(Formatting.YELLOW)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            
                            // Notify all players
                            Text announcement = Text.literal("The Global Hunt has been cancelled by an administrator.").formatted(Formatting.YELLOW);
                            context.server().getPlayerManager().broadcast(announcement, false);
                        } else {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("No active Global Hunt to cancel").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                    }
                    case TOGGLE_GLOBAL_HUNT_SYSTEM -> {
                        boolean enabled = GlobalHuntManager.getInstance().getConfig().isEnabled();
                        GlobalHuntManager.getInstance().getConfig().setEnabled(!enabled);
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Global Hunt system ").append((!enabled) ? Text.literal("enabled").formatted(Formatting.GREEN) : Text.literal("disabled").formatted(Formatting.RED))));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
                    case GLOBAL_HUNT_STATUS -> {
                        List<Text> lines = new ArrayList<>();
                        lines.add(Text.literal("--- Global Hunt Status ---").formatted(Formatting.GOLD));
                        lines.add(Text.literal("System: ").append(GlobalHuntManager.getInstance().getConfig().isEnabled() ? Text.literal("Enabled").formatted(Formatting.GREEN) : Text.literal("Disabled").formatted(Formatting.RED)));
                        
                        if (GlobalHuntManager.getInstance().hasActiveEvent()) {
                            var event = GlobalHuntManager.getInstance().getCurrentEvent();
                            lines.add(Text.literal("Active Event: ").append(Text.literal((event.isShiny() ? "Shiny " : "") + event.getPokemonName()).formatted(Formatting.GOLD)));
                            lines.add(Text.literal("Location: ").append(Text.literal(event.getCoordinates().getX() + ", " + event.getCoordinates().getY() + ", " + event.getCoordinates().getZ()).formatted(Formatting.AQUA)));
                            lines.add(Text.literal("World: ").append(Text.literal(event.getWorld().getRegistryKey().getValue().toString()).formatted(Formatting.AQUA)));
                        } else {
                            lines.add(Text.literal("Active Event: None").formatted(Formatting.GRAY));
                        }
                        
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
                    case AUTOCOMPLETE_PLAYER -> {
                        String playerName = payload.parameter().trim();
                        ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(playerName);
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
                        
                        // Create backup
                        File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, targetPlayer.getUuid().toString() + ".json");
                        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, targetPlayer.getUuid().toString() + ".json.bak");
                        try {
                            if (progressFile.exists() && !backupFile.exists()) {
                                Files.copy(progressFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            LOGGER.error("Failed to create backup for " + playerName, e);
                        }
                        
                        String missingPokemon = autocompleteGenerationForPlayer(targetPlayer, genName, genData);
                        List<Text> response = new ArrayList<>();
                        response.add(Text.literal("Autocompleted " + formatGenName(genName) + " for player " + targetPlayer.getName().getString()).formatted(Formatting.GREEN));
                        response.add(Text.literal("To complete the list, capture: ").append(Text.literal(missingPokemon).formatted(Formatting.GOLD)).formatted(Formatting.AQUA));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(response));
                    }
                    case ROLLBACK_PLAYER -> {
                        String playerName = payload.parameter().trim();
                        ServerPlayerEntity targetPlayer = context.server().getPlayerManager().getPlayer(playerName);
                        if (targetPlayer == null) {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Player " + playerName + " is not online.").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            return;
                        }
                        
                        boolean success = rollbackPlayerProgress(targetPlayer);
                        if (success) {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Successfully rolled back progress for " + playerName).formatted(Formatting.GREEN)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        } else {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("No backup file found for " + playerName + ".").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                    }
                    case SPAWN_POKEMON -> {
                        String[] parts = payload.parameter().trim().split(" ");
                        String pokemonName = parts[0];
                        boolean isShiny = parts.length > 1 && "shiny".equals(parts[1]);
                        
                        if (!ConfigManager.getServerConfig().enable_test_mode) {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Test Mode is disabled. Enable it in Server Control.").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            return;
                        }
                        
                        // Execute test spawn logic inline since method is not static
                        ServerWorld world = player.getServerWorld();
                        
                        // Strict validation: the name must exist in Cobblemon's official list.
                        if (PokeNotifierApi.getAllPokemonNames().noneMatch(name -> name.equals(pokemonName.toLowerCase()))) {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Error: Pokémon '" + pokemonName + "' is not a valid Pokémon name.").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            return;
                        }
                        
                        try {
                            PokemonProperties props = PokemonProperties.Companion.parse(pokemonName.toLowerCase());
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
                            
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Spawned a " + (isShiny ? "Shiny " : "") + pokemonName + ".").formatted(Formatting.GREEN)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        } catch (Exception e) {
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Error: Pokémon '" + pokemonName + "' not found.").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        }
                    }
                    case HELP -> {
                        List<Text> lines = new ArrayList<>();
                        lines.add(Text.literal("--- Poke Notifier Admin Help ---").formatted(Formatting.GOLD));
                        lines.add(Text.literal("Use the GUI to manage all mod settings.").formatted(Formatting.WHITE));
                        lines.add(Text.literal("Server Control: Toggle debug/test modes, reload configs").formatted(Formatting.AQUA));
                        lines.add(Text.literal("Event Management: Control bounty system and swarms").formatted(Formatting.AQUA));
                        lines.add(Text.literal("Player Data: Manage player progress and backups").formatted(Formatting.AQUA));
                        lines.add(Text.literal("Testing: Spawn Pokémon for testing purposes").formatted(Formatting.AQUA));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
                    case VERSION -> {
                        String modVersion = FabricLoader.getInstance()
                                .getModContainer(MOD_ID)
                                .map(ModContainer::getMetadata)
                                .map(meta -> meta.getVersion().getFriendlyString())
                                .orElse("Unknown");
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Poke Notifier ver. " + modVersion).formatted(Formatting.AQUA)));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
                    case STATUS -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        List<Text> lines = new ArrayList<>();
                        lines.add(Text.literal("--- Poke Notifier Server Status ---").formatted(Formatting.GOLD));
                        lines.add(createServerStatusLine("Debug Mode", config.debug_mode_enabled));
                        lines.add(createServerStatusLine("Bounty System", config.bounty_system_enabled));
                        lines.add(createServerStatusLine("Test Mode", config.enable_test_mode));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    }
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
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Error: Generation '" + genName + "' not found.").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            return;
                        }
                        // Allow only one active generation at a time.
                        if (progress.active_generations.contains(genName)) {
                            String regionName = formatRegionName(genData.region);
                            List<Text> lines = new ArrayList<>(List.of(Text.literal("Already tracking " + regionName + ".").formatted(Formatting.YELLOW)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                            return;
                        }

                        if (!progress.active_generations.isEmpty()) {
                            String oldGen = progress.active_generations.iterator().next();
                            // This message can stay in chat as it's a side-effect of a new action
                            player.sendMessage(Text.literal("Stopped tracking " + formatGenName(oldGen) + ".").formatted(Formatting.YELLOW));
                        }
                        progress.active_generations.clear();
                        progress.active_generations.add(genName);
                        ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
                        String regionName = formatRegionName(genData.region);

                        ServerPlayNetworking.send(player, new ModeStatusPayload("Tracking: " + regionName, true));
                        List<Text> lines = new ArrayList<>(List.of(Text.literal("Now tracking: " + regionName).formatted(Formatting.GREEN)));
                        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                        PokeNotifierServerUtils.sendCatchProgressUpdate(player);
                        break;

                    case DISABLE:
                        if (progress.active_generations.remove(genName)) {
                            ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
                            List<Text> disableLines = new ArrayList<>(List.of(Text.literal("Tracking disabled for " + formatGenName(genName)).formatted(Formatting.YELLOW)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(disableLines));
                            PokeNotifierServerUtils.sendCatchProgressUpdate(player); // Update to hide the HUD
                        } else {
                            List<Text> notTrackingLines = new ArrayList<>(List.of(Text.literal("You were not tracking " + formatGenName(genName) + ".").formatted(Formatting.RED)));
                            ServerPlayNetworking.send(player, new GuiResponsePayload(notTrackingLines));
                        }
                        break;

                    case LIST:
                        List<Text> catchemallLines; // Use a unique name to avoid scope conflicts
                        if (progress.active_generations.isEmpty()) {
                            catchemallLines = new ArrayList<>(List.of(Text.literal("You are not tracking any generation for Catch 'em All mode.").formatted(Formatting.YELLOW)));
                        } else {
                            catchemallLines = new ArrayList<>();
                            catchemallLines.add(Text.literal("You are currently tracking the following generations:").formatted(Formatting.YELLOW));
                            progress.active_generations.forEach(gen -> {
                                GenerationData data = ConfigManager.getGenerationData(gen);
                                String regionNameForList = data != null ? formatRegionName(data.region) : "Unknown";
                                catchemallLines.add(Text.literal("- " + formatGenName(gen) + " (" + regionNameForList + ")").formatted(Formatting.GOLD));
                            });
                        }
                        ServerPlayNetworking.send(player, new GuiResponsePayload(catchemallLines));
                        break;
                }
            });
        });

        // --- FIX: Handle update source selection via packet ---
        ServerPlayNetworking.registerGlobalReceiver(UpdateSourcePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String source = payload.source();

            context.server().execute(() -> {
                if (player.hasPermissionLevel(2) && List.of("modrinth", "curseforge", "none").contains(source)) {
                    ConfigManager.getServerConfig().update_checker_source = source;
                    // FIX: Send response to GUI
                    List<Text> lines = new ArrayList<>(List.of(Text.literal("Update check source set to: ").formatted(Formatting.GREEN)
                            .append(Text.literal(source).formatted(Formatting.GOLD))));
                    ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                    ConfigManager.saveServerConfigToFile();
                    // FIX: Restore the immediate update check after changing the source.
                    UpdateChecker.checkForUpdates(player);
                }
            });
        });
    }

    /**
     * Generates and sends the player's custom hunt list to them.
     * @param player The player to send the list to.
     */
    private static void sendCustomHuntList(ServerPlayerEntity player) {
        CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());
        if (playerConfig.tracked_pokemon.isEmpty()) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Your custom tracking list is empty.").formatted(Formatting.YELLOW)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        } else {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("Your custom tracking list:").formatted(Formatting.YELLOW));
            playerConfig.tracked_pokemon.stream().sorted().forEach(name -> {
                Text pokemonText = Text.literal("• " + name).formatted(Formatting.GOLD);
                Text removeButton = Text.literal(" [X]").formatted(Formatting.RED, Formatting.BOLD)
                        .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/pnc customcatch remove " + name)));
                lines.add(pokemonText.copy().append(removeButton));
            });
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
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
            // FIX: Send error to GUI
            ServerPlayNetworking.send(player, new GuiResponsePayload(List.of(Text.literal("Test Mode is disabled. Enable it in Server Control.").formatted(Formatting.RED))));
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        final String finalPokemonName = pokemonName.toLowerCase().trim();

        // Strict validation: the name must exist in Cobblemon's official list.
        if (PokeNotifierApi.getAllPokemonNames().noneMatch(name -> name.equals(finalPokemonName))) {
            ServerPlayNetworking.send(player, new GuiResponsePayload(List.of(Text.literal("Error: Pokémon '").append(Text.literal(finalPokemonName).formatted(Formatting.GOLD)).append("' is not a valid Pokémon name.").formatted(Formatting.RED))));
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

            ServerPlayNetworking.send(player, new GuiResponsePayload(List.of(Text.literal("Spawned a " + (isShiny ? "Shiny " : "") + finalPokemonName + ".").formatted(Formatting.GREEN))));
            return 1;
        } catch (Exception e) {
            ServerPlayNetworking.send(player, new GuiResponsePayload(List.of(Text.literal("Error: Pokémon '" + finalPokemonName + "' not found.").formatted(Formatting.RED))));
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

        // --- FIX: Correctly identify the missing Pokémon ---
        Set<String> allPokemonForGen = new HashSet<>(genData.pokemon);
        if (allPokemonForGen.isEmpty()) {
            return "none";
        }

        // Find the difference between all Pokémon and the ones the player has caught.
        allPokemonForGen.removeAll(progress.caught_pokemon.getOrDefault(genName, new HashSet<>()));

        // The remaining Pokémon is the one to leave out.
        String lastPokemon = allPokemonForGen.stream().findFirst().orElse("mew"); // Fallback

        Set<String> completedList = new HashSet<>(genData.pokemon);
        completedList.remove(lastPokemon);
        progress.caught_pokemon.put(genName, completedList);
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

    // --- Bounty System Logic ---

    public static String getActiveBounty() { // Now reads directly from config
        return ConfigManager.getServerConfig().active_bounty;
    }

    public static void clearActiveBounty(boolean announce) {
        String currentBounty = getActiveBounty();
        if (currentBounty != null && announce) {
            Text message = Text.literal("The bounty for ").formatted(Formatting.YELLOW)
                    .append(Text.literal(currentBounty).formatted(Formatting.GOLD))
                    .append(Text.literal(" has been claimed!").formatted(Formatting.YELLOW));
            server.getPlayerManager().broadcast(message, false);
        }
        ConfigManager.getServerConfig().active_bounty = null;
        ConfigManager.saveServerConfigToFile(); // Persist the change
    }

    private static void tickBountySystem(MinecraftServer server) {
        ConfigServer config = ConfigManager.getServerConfig();
        if (!config.bounty_system_enabled) {
            return;
        }

        bountyTickCounter++;

        // --- Lógica de Expiración ---
        String activeBounty = getActiveBounty();
        if (activeBounty != null && bountyStartTime > 0) {
            long elapsedTime = System.currentTimeMillis() - bountyStartTime;
            if (elapsedTime >= (long)config.bounty_duration_minutes * 60 * 1000) {
                LOGGER.info("[Bounty System] Bounty for {} has expired.", activeBounty);
                server.getPlayerManager().broadcast(Text.literal("The bounty for ").append(Text.literal(activeBounty).formatted(Formatting.GOLD)).append(" has expired! The Pokémon got away...").formatted(Formatting.YELLOW), false);
                clearActiveBounty(false);
                return; // Stop further processing for this tick
            }

            // --- MEJORA: Periodic Reminder Logic ---
            if (config.bounty_reminder_interval_minutes > 0) {
                bountyReminderTickCounter++;
                if (bountyReminderTickCounter >= config.bounty_reminder_interval_minutes * 60 * 20) {
                    bountyReminderTickCounter = 0;
                    long remainingMinutes = config.bounty_duration_minutes - (elapsedTime / (60 * 1000));
                    Text reminder = Text.literal("Reminder: The bounty for ").formatted(Formatting.YELLOW)
                            .append(Text.literal(activeBounty).formatted(Formatting.GOLD))
                            .append(Text.literal(" is still active! Time remaining: ~" + remainingMinutes + " minutes.").formatted(Formatting.YELLOW));
                    server.getPlayerManager().broadcast(reminder, false);
                }
            }
        }


        if (bountyTickCounter >= config.bounty_check_interval_seconds * 20) {
            bountyTickCounter = 0;

            // Only start a new bounty if there isn't one active.
            if (activeBounty == null) {
                if (BOUNTY_RANDOM.nextInt(100) < config.bounty_start_chance_percent) {
                    startNewBounty(server);
                }
            }
        }
    }

    private static void startNewBounty(MinecraftServer server) {
        ConfigPokemon pokemonConfig = ConfigManager.getPokemonConfig();
        List<String> bountyPool = new ArrayList<>();
        bountyPool.addAll(pokemonConfig.RARE);
        bountyPool.addAll(pokemonConfig.ULTRA_RARE);

        if (bountyPool.isEmpty()) {
            LOGGER.warn("[Bounty System] No Pokémon available in RARE or ULTRA_RARE lists to create a bounty.");
            return;
        }

        String newBounty = bountyPool.get(BOUNTY_RANDOM.nextInt(bountyPool.size()));
        ConfigManager.getServerConfig().active_bounty = newBounty;
        ConfigManager.saveServerConfigToFile(); // Persist the new bounty immediately
        bountyReminderTickCounter = 0; // Reset reminder timer
        bountyStartTime = System.currentTimeMillis(); // Start the timer


        String capitalizedBounty = newBounty.substring(0, 1).toUpperCase() + newBounty.substring(1);
        Text message = Text.literal("🎯 New Bounty Available! ").formatted(Formatting.GREEN)
                .append(Text.literal("The first trainer to capture a ").formatted(Formatting.YELLOW))
                .append(Text.literal(capitalizedBounty).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" will receive a special reward!").formatted(Formatting.YELLOW));

        // --- MEJORA: Usamos ModeStatusPayload para el toast y el sonido de campana ---
        ModeStatusPayload payload = new ModeStatusPayload("New Bounty!", true);
        server.getPlayerManager().broadcast(message, false);
        server.getPlayerManager().getPlayerList().forEach(player -> {
            player.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE, SoundCategory.NEUTRAL, 1.0F, 1.2F);
            ServerPlayNetworking.send(player, payload);
        });

        LOGGER.info("[Bounty System] New bounty started for: {}", newBounty);
    }

    // --- Centralized Command Logic ---

    private static int executeReload(CommandContext<ServerCommandSource> context) {
        try {
            ConfigManager.loadConfig();
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Poke Notifier configurations reloaded successfully.").formatted(Formatting.GREEN)));
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player != null) ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            return 1;
        } catch (ConfigManager.ConfigReadException e) {
            LOGGER.error("Failed to reload Poke Notifier configuration: {}", e.getMessage());
            context.getSource().sendError(Text.literal("Error reloading configs: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }

    private static int executeConfirmReset(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0; // Should not happen from a click event

        String providedToken = StringArgumentType.getString(context, "token");
        String expectedToken = RESET_CONFIRMATION_TOKENS.get(player.getUuid());

        if (expectedToken != null && expectedToken.equals(providedToken)) {
            RESET_CONFIRMATION_TOKENS.remove(player.getUuid()); // Invalidate token
            try {
                ConfigManager.resetToDefault();
                List<Text> lines = new ArrayList<>(List.of(Text.literal("All Poke Notifier configurations have been reset to default.").formatted(Formatting.GREEN)));
                ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
                return 1;
            } catch (Exception e) {
                LOGGER.error("Failed to generate new default configurations.", e);
                context.getSource().sendError(Text.literal("Failed to generate new configs. Check server logs.").formatted(Formatting.RED));
                return 0;
            }
        } else {
            context.getSource().sendError(Text.literal("Invalid or expired confirmation token. Please run '/pokenotifier config reset' again.").formatted(Formatting.RED));
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

    private static boolean executeSwarmStart(ServerPlayerEntity player, String pokemonName) {
        // --- FIX: The manual command now triggers the same logic as the automatic system ---
        startRandomSwarm(player.getServer(), pokemonName);
        return true;
    }

    private static void tickSwarmSystem(MinecraftServer server) {
        ConfigServer config = ConfigManager.getServerConfig();
        if (!config.swarm_system_enabled) {
            return;
        }

        swarmTickCounter++;

        if (swarmTickCounter >= config.swarm_check_interval_minutes * 60 * 20) {
            swarmTickCounter = 0;

            if (SWARM_RANDOM.nextInt(100) < config.swarm_start_chance_percent) {
                startRandomSwarm(server, null);
            }
        }
    }

    private static BlockPos generateRandomCoordinates(ServerWorld world) {
        Random random = new Random();
        int maxDistance = 1000; // For automatic events
        
        for (int attempts = 0; attempts < 20; attempts++) {
            int x = random.nextInt(maxDistance * 2) - maxDistance;
            int z = random.nextInt(maxDistance * 2) - maxDistance;
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
            
            BlockPos pos = new BlockPos(x, y, z);
            
            if (y >= world.getBottomY() + 10 && y <= world.getTopY() - 10) {
                return pos;
            }
        }
        
        // Fallback: spawn near world spawn
        BlockPos spawn = world.getSpawnPos();
        int x = spawn.getX() + random.nextInt(200) - 100;
        int z = spawn.getZ() + random.nextInt(200) - 100;
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        return new BlockPos(x, y, z);
    }
    
    private static BlockPos generateRandomCoordinatesNearPlayer(ServerWorld world, ServerPlayerEntity player) {
        Random random = new Random();
        int maxDistance = 500; // Closer for manual events
        
        for (int attempts = 0; attempts < 15; attempts++) {
            int x = (int) player.getX() + random.nextInt(maxDistance * 2) - maxDistance;
            int z = (int) player.getZ() + random.nextInt(maxDistance * 2) - maxDistance;
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
            
            BlockPos pos = new BlockPos(x, y, z);
            
            if (y >= world.getBottomY() + 10 && y <= world.getTopY() - 10) {
                return pos;
            }
        }
        
        // Fallback: spawn near player
        int x = (int) player.getX() + random.nextInt(200) - 100;
        int z = (int) player.getZ() + random.nextInt(200) - 100;
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        return new BlockPos(x, y, z);
    }

    private static void startRandomSwarm(MinecraftServer server, String forcedPokemonName) {
        String pokemonName;
        if (forcedPokemonName != null) {
            pokemonName = forcedPokemonName;
        } else {
            // 1. Create the pool of eligible Pokémon.
            ConfigPokemon pokemonConfig = ConfigManager.getPokemonConfig();
            List<String> swarmPool = new ArrayList<>();
            swarmPool.addAll(pokemonConfig.RARE);
            swarmPool.addAll(pokemonConfig.ULTRA_RARE);

            if (swarmPool.isEmpty()) {
                LOGGER.warn("[Swarm System] No Pokémon available in RARE or ULTRA_RARE lists to create a swarm.");
                return;
            }
            // 2. Select a random Pokémon.
            pokemonName = swarmPool.get(SWARM_RANDOM.nextInt(swarmPool.size()));
        }

        // 3. Find a random, valid location.
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList().stream()
                .filter(p -> p.getServerWorld().getRegistryKey() == ServerWorld.OVERWORLD).toList();

        if (players.isEmpty()) {
            LOGGER.info("[Swarm System] No players in Overworld, skipping swarm.");
            return;
        }

        ServerPlayerEntity referencePlayer = players.get(SWARM_RANDOM.nextInt(players.size()));
        ServerWorld world = referencePlayer.getServerWorld();

        double angle = SWARM_RANDOM.nextDouble() * 2 * Math.PI;
        // --- MEJORA: Use a closer, more engaging distance for swarms ---
        double distance = 30 + (SWARM_RANDOM.nextDouble() * 20); // 30-50 blocks away

        int x = (int) (referencePlayer.getX() + Math.cos(angle) * distance);
        int z = (int) (referencePlayer.getZ() + Math.sin(angle) * distance);
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);

        BlockPos swarmPos = new BlockPos(x, y, z);

        // 4. Execute the outbreak at the found location.
        try {
            PokemonProperties props = PokemonProperties.Companion.parse(pokemonName);
            Pokemon pokemon = props.create(); // Create a sample to get the name
            String capitalizedName = pokemon.getSpecies().getName();
            String biomeName = world.getBiome(swarmPos).getKey().map(key -> key.getValue().getPath()).orElse("unknown").replace("_", " ");

            Text message = Text.literal("🌊 Swarm Alert! ").formatted(Formatting.AQUA)
                    .append(Text.literal("A large concentration of ").formatted(Formatting.YELLOW))
                    .append(Text.literal(capitalizedName).formatted(Formatting.GOLD, Formatting.BOLD))
                    .append(Text.literal(" has been detected in " + biomeName + " biomes").formatted(Formatting.YELLOW))
                    .append(Text.literal(" around X: " + swarmPos.getX() + ", Z: " + swarmPos.getZ()).formatted(Formatting.AQUA)); // Add coordinates

            server.getPlayerManager().broadcast(message, false);
            server.getPlayerManager().getPlayerList().forEach(p -> p.playSoundToPlayer(SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.NEUTRAL, 1.0F, 1.0F));

            // --- MEJORA: Spawn a random amount of Pokémon between 10 and 15 ---
            int swarmSize = 10 + SWARM_RANDOM.nextInt(6); // Generates a number from 0-5, so the result is 10-15.
            for (int i = 0; i < swarmSize; i++) {
                PokemonEntity entity = props.createEntity(world);
                entity.teleport(swarmPos.getX() + (Math.random() - 0.5) * 20, swarmPos.getY(), swarmPos.getZ() + (Math.random() - 0.5) * 20, true);
                world.spawnEntity(entity);
            }
            LOGGER.info("[Swarm System] Started a swarm of {} at {}", pokemonName, swarmPos);
        } catch (Exception e) {
            LOGGER.error("Failed to start automatic swarm for " + pokemonName, e);
        }
    }
}