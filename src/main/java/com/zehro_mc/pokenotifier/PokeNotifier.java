package com.zehro_mc.pokenotifier;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.command.DebugModeCommand;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import com.zehro_mc.pokenotifier.event.CaptureListener;
import com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload;
import com.zehro_mc.pokenotifier.networking.PokeNotifierPackets;
import com.zehro_mc.pokenotifier.networking.ServerDebugStatusPayload;
import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import kotlin.Unit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zehro_mc.pokenotifier.command.ReloadConfigCommand;
import java.util.Map;
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

        // --- LA CORRECCIÓN CLAVE ---
        // Registramos los paquetes aquí para que tanto el servidor como el cliente los conozcan.
        PokeNotifierPackets.registerS2CPackets();
        // Registramos los paquetes que van del cliente al servidor
        PokeNotifierPackets.registerC2SPackets();

        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> server = startedServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> server = null);

        try {
            ConfigManager.loadConfig();
        } catch (ConfigManager.ConfigReadException e) {
            LOGGER.error("Failed to load Poke Notifier configuration on startup. Using default values.", e);
        }
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
            var testModeCommand = CommandManager.literal("test_mode") // Placeholder
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Test mode command is under development.").formatted(Formatting.GRAY), false);
                        return 1;
                    }).build();

            dispatcher.getRoot().getChild("pokenotifier").addChild(statusCommand);
            dispatcher.getRoot().getChild("pokenotifier").addChild(testModeCommand);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> server = null);

        // Enviar estado del debug mode a los OPs cuando se conectan
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player.hasPermissionLevel(2)) { // Si es OP
                boolean debugEnabled = ConfigManager.getServerConfig().debug_mode_enabled;
                ServerDebugStatusPayload payload = new ServerDebugStatusPayload(debugEnabled);
                ServerPlayNetworking.send(player, payload);
            }
        });

        // Recibir y procesar las actualizaciones de la lista personalizada
        ServerPlayNetworking.registerGlobalReceiver(CustomListUpdatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            String pokemonName = payload.pokemonName().toLowerCase().trim();

            context.server().execute(() -> {
                CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());

                switch (payload.action()) {
                    case ADD:
                        if (PokemonSpecies.INSTANCE.getByName(pokemonName) == null) {
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

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            RarePokemonNotifier.onPokemonSpawn(event);
            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            TRACKED_POKEMON.keySet().removeIf(entity -> entity.getPokemon().getUuid().equals(event.getPokemon().getUuid()));
            CaptureListener.onPokemonCaptured(event);
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

    private static MutableText createServerStatusLine(String label, boolean isEnabled) {
        MutableText message = Text.literal(label + " = ").formatted(Formatting.WHITE);
        if (isEnabled) {
            message.append(Text.literal("ON").formatted(Formatting.GREEN));
        } else {
            message.append(Text.literal("OFF").formatted(Formatting.RED));
        }
        return message;
    }
}