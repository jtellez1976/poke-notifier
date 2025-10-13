package com.zehro_mc.pokenotifier;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.Cobblemon;
import com.zehro_mc.pokenotifier.component.ModDataComponents;
import com.zehro_mc.pokenotifier.command.DebugModeCommand;
import com.zehro_mc.pokenotifier.api.PokeNotifierApi;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.item.ModItems;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.event.CaptureListener;
import com.zehro_mc.pokenotifier.networking.*;
import com.zehro_mc.pokenotifier.networking.ServerDebugStatusPayload;
import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.util.PokeNotifierServerUtils;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import kotlin.Unit;
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
import com.zehro_mc.pokenotifier.command.ReloadConfigCommand;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.concurrent.ConcurrentHashMap;

public class PokeNotifier implements ModInitializer {
    public static final String MOD_ID = "poke-notifier";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier WAYPOINT_CHANNEL_ID = Identifier.of(MOD_ID, "waypoint_payload");
    public static final Identifier STATUS_UPDATE_CHANNEL_ID = Identifier.of(MOD_ID, "status_update_payload");

    public static final Map<PokemonEntity, RarityUtil.RarityCategory> TRACKED_POKEMON = new ConcurrentHashMap<>();

    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Poke Notifier...");

        // Registramos los TIPOS de paquetes que el servidor puede recibir (C2S).
        PayloadTypeRegistry.playC2S().register(CustomListUpdatePayload.ID, CustomListUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CatchemallUpdatePayload.ID, CatchemallUpdatePayload.CODEC);

        // Y también los TIPOS de paquetes que el servidor puede enviar (S2C).
        PayloadTypeRegistry.playS2C().register(WaypointPayload.ID, WaypointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatusUpdatePayload.ID, StatusUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDebugStatusPayload.ID, ServerDebugStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CurrentGenPayload.ID, CurrentGenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CatchProgressPayload.ID, CatchProgressPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GlobalAnnouncementPayload.ID, GlobalAnnouncementPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModeStatusPayload.ID, ModeStatusPayload.CODEC);

        // Cargamos la configuración desde los archivos .json al iniciar el mod.
        // Esta es la corrección clave.
        try {
            ConfigManager.loadConfig();
        } catch (ConfigManager.ConfigReadException e) {
            LOGGER.error("Failed to load Poke Notifier configuration on startup. Using default values.", e);
        }

        // Registramos nuestros componentes de datos
        ModDataComponents.registerModDataComponents();

        // Registramos nuestros objetos personalizados
        ModItems.registerModItems();

        // --- REGISTRO DE RECEPTORES DE PAQUETES (LADO DEL SERVIDOR) ---
        // Esto debe ocurrir DESPUÉS de que los tipos de paquetes hayan sido registrados.
        registerServerPacketReceivers();

        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> server = startedServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> server = null);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ReloadConfigCommand.register(dispatcher, environment);
            DebugModeCommand.register(dispatcher);

            // Comando de estado del servidor
            var statusCommand = CommandManager.literal("status")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        ConfigServer config = ConfigManager.getServerConfig();
                        context.getSource().sendFeedback(() -> Text.literal("--- Poke Notifier Server Status ---").formatted(Formatting.GOLD), false);
                        context.getSource().sendFeedback(() -> createServerStatusLine("Debug Mode", config.debug_mode_enabled), false);
                        context.getSource().sendFeedback(() -> createServerStatusLine("Test Mode", config.enable_test_mode), false);
                        return 1;
                    }).build();

            // Comando para test_mode
            var testModeCommand = CommandManager.literal("test_mode")
                    .requires(source -> source.hasPermissionLevel(2))
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

            // NUEVO: Comando de prueba para generar Pokémon
            SuggestionProvider<ServerCommandSource> pokemonSuggestionProvider = (context, builder) ->
                    CommandSource.suggestMatching(PokeNotifierApi.getAllPokemonNames(), builder);

            var testSpawnCommand = CommandManager.literal("testspawn")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("pokemon", StringArgumentType.string())
                            .suggests(pokemonSuggestionProvider)
                            .executes(context -> {
                                return executeTestSpawn(context.getSource().getPlayer(), StringArgumentType.getString(context, "pokemon"), false);
                            }))
                            .then(CommandManager.literal("shiny")
                                    .executes(context -> {
                                        return executeTestSpawn(context.getSource().getPlayer(), StringArgumentType.getString(context, "pokemon"), true);
                                    })).build();

            // --- NUEVO: Comando para autocompletar generaciones ---
            SuggestionProvider<ServerCommandSource> generationSuggestionProvider = (context, builder) ->
                    CommandSource.suggestMatching(Stream.of("gen1", "gen2", "gen3", "gen4", "gen5", "gen6", "gen7", "gen8", "gen9"), builder);

            var autoCompleteGenCommand = CommandManager.literal("autocompletegen")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                            .suggests((context, builder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder))
                            .then(CommandManager.argument("generation", StringArgumentType.string())
                                    .suggests(generationSuggestionProvider)
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

                                        // --- LÓGICA DE BACKUP ---
                                        File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, targetPlayer.getUuid().toString() + ".json");
                                        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, targetPlayer.getUuid().toString() + ".json.bak");

                                        if (backupFile.exists()) {
                                            context.getSource().sendError(Text.literal("A backup file already exists for this player. Please use '/pokenotifier rollback " + profile.getName() + "' first.").formatted(Formatting.RED));
                                            return 0;
                                        }

                                        try {
                                            if (progressFile.exists()) {
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

            dispatcher.getRoot().getChild("pokenotifier").addChild(statusCommand);
            dispatcher.getRoot().getChild("pokenotifier").addChild(testModeCommand);
            dispatcher.getRoot().getChild("pokenotifier").addChild(testSpawnCommand);
            dispatcher.getRoot().getChild("pokenotifier").addChild(autoCompleteGenCommand);

            // --- NUEVO: Comando para restaurar el progreso ---
            var rollbackCommand = CommandManager.literal("rollback")
                    .requires(source -> source.hasPermissionLevel(2))
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

            dispatcher.getRoot().getChild("pokenotifier").addChild(rollbackCommand);
        });

        // Enviar estado del debug mode a los OPs cuando se conectan
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player.hasPermissionLevel(2)) { // Si es OP
                boolean debugEnabled = ConfigManager.getServerConfig().debug_mode_enabled;
                ServerDebugStatusPayload payload = new ServerDebugStatusPayload(debugEnabled);
                ServerPlayNetworking.send(player, payload);

                // --- SINCRONIZACIÓN INICIAL DEL PC ---
                performInitialPcSync(player);

                // Enviamos el estado del HUD de progreso al conectar
                PokeNotifierServerUtils.sendCatchProgressUpdate(player);
            }
        });

        // La forma correcta para Cobblemon 1.6+ es usar POKEMON_ENTITY_SPAWN, que nos da un evento con la entidad.
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            RarePokemonNotifier.onPokemonSpawn(event.getEntity());
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            TRACKED_POKEMON.keySet().removeIf(entity -> entity.getPokemon().getUuid().equals(event.getPokemon().getUuid()));
            CaptureListener.onPokemonCaptured(event);

            // Actualizamos el HUD del jugador que ha capturado
            PokeNotifierServerUtils.sendCatchProgressUpdate(event.getPlayer());
            return Unit.INSTANCE;
        });

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
        });
    }

    /**
     * Registra los receptores para los paquetes que vienen del cliente al servidor.
     * Se llama después de que los tipos de paquetes han sido registrados.
     */
    private static void registerServerPacketReceivers() {
        // Recibir y procesar las actualizaciones de la lista personalizada
        ServerPlayNetworking.registerGlobalReceiver(CustomListUpdatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String pokemonName = payload.pokemonName().toLowerCase().trim();

            context.server().execute(() -> {
                CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());

                switch (payload.action()) {
                    case ADD:
                        // El parser de propiedades es más robusto y entiende tanto 'mr-mime' como 'mr_mime'.
                        // Lo usamos aquí para una validación consistente.
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

        // Recibir y procesar las actualizaciones del modo Catch 'em All
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
                        // Lógica para permitir solo una generación activa a la vez
                        if (progress.active_generations.contains(genName)) {
                            // Si ya está siguiendo esta generación, solo se lo recordamos.
                            String regionName = formatRegionName(genData.region);
                            ServerPlayNetworking.send(player, new ModeStatusPayload("Already Tracking: " + regionName, true));
                            return;
                        }
                        
                        // Si ya estaba siguiendo otra, se la notificamos.
                        if (!progress.active_generations.isEmpty()) {
                            String oldGen = progress.active_generations.iterator().next();
                            player.sendMessage(Text.literal("Stopped tracking " + formatGenName(oldGen) + ".").formatted(Formatting.YELLOW), false);
                        }
                        progress.active_generations.clear(); // Limpiamos la lista
                        progress.active_generations.add(genName); // Añadimos la nueva
                        ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
                        String regionName = formatRegionName(genData.region); // Re-declaramos aquí porque el scope anterior terminó.

                        // Enviamos feedback visual
                        ServerPlayNetworking.send(player, new ModeStatusPayload("Tracking: " + regionName, true));
                        PokeNotifierServerUtils.sendCatchProgressUpdate(player);
                        break;

                    case DISABLE:
                        // --- LÓGICA RESTAURADA ---
                        if (progress.active_generations.remove(genName)) {
                            ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
                            // Enviamos feedback visual
                            ServerPlayNetworking.send(player, new ModeStatusPayload("Tracking Disabled", false));
                            PokeNotifierServerUtils.sendCatchProgressUpdate(player); // Actualizamos para ocultar el HUD
                        } else {
                            // Si no estaba siguiendo esa generación, se lo decimos.
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
        // --- RESTRICCIÓN DE TEST MODE ---
        // El comando de spawn de prueba solo debe funcionar si el test_mode está activado.
        if (!ConfigManager.getServerConfig().enable_test_mode) {
            player.sendMessage(Text.literal("Test Mode is disabled. Please enable it first with '/pokenotifier test_mode enable'.").formatted(Formatting.RED), false);
            return 0;
        }

        ServerWorld world = player.getServerWorld();
        final String finalPokemonName = pokemonName.toLowerCase().trim();

        // Validación estricta: el nombre debe existir en la lista oficial de Cobblemon.
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
            // ¡Importante! No le asignamos un propietario (OwnerUuid)
            
            // Añadimos una etiqueta NBT personalizada para identificar este spawn como uno de prueba.
            pokemonEntity.getPokemon().getPersistentData().putBoolean("pokenotifier_test_spawn", true);

            world.spawnEntity(pokemonEntity);

            // Disparamos nuestro propio evento de notificación manualmente, ya que world.spawnEntity no lo hace.
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
     * Realiza la sincronización inicial del PC y la party de un jugador con su progreso de "Catch 'em All".
     * Esta operación solo se ejecuta una vez por jugador.
     * @param player El jugador a sincronizar.
     */
    private static void performInitialPcSync(ServerPlayerEntity player) {
        var progress = ConfigManager.getPlayerCatchProgress(player.getUuid());

        // Si la sincronización ya se completó, no hacemos nada.
        if (progress.initialPcSyncCompleted) {
            return;
        }

        LOGGER.info("Performing initial PC sync for player: " + player.getName().getString());

        PokemonStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        PokemonStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);

        // Combinamos la party y el PC en un solo stream de Pokémon
        Stream<Pokemon> partyStream = StreamSupport.stream(party.spliterator(), false);
        Stream<Pokemon> pcStream = StreamSupport.stream(pc.spliterator(), false);
        Stream.concat(partyStream, pcStream).forEach(pokemon -> {
            if (pokemon == null) return;
            String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();

            // Iteramos sobre todas las generaciones conocidas para ver si el Pokémon pertenece a alguna.
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

        // Creamos una lista mutable para poder manipularla.
        List<String> pokemonList = new ArrayList<>(genData.pokemon);
        if (pokemonList.isEmpty()) {
            return "none"; // No hay Pokémon en esta generación.
        }

        // Seleccionamos el último Pokémon para dejarlo sin capturar.
        String lastPokemon = pokemonList.remove(pokemonList.size() - 1);

        // Sobrescribimos la lista de capturados para esa generación.
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
            // Forzamos la recarga del progreso desde el archivo restaurado
            ConfigManager.forceReloadPlayerCatchProgress(player.getUuid());
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);

            // --- CORRECCIÓN: Eliminamos cualquier trofeo del inventario ---
            player.getInventory().remove(stack -> stack.getItem() instanceof com.zehro_mc.pokenotifier.item.PokedexTrophyItem, -1, player.getInventory());
            player.sendMessage(Text.literal("Your Pokédex Trophies have been removed as part of the rollback.").formatted(Formatting.YELLOW), false);

            player.sendMessage(Text.literal("Your 'Catch 'em All' progress has been restored by an administrator.").formatted(Formatting.GREEN), false);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to rollback progress for player " + player.getName().getString(), e);
            return false;
        }
    }
}