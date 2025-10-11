package com.zehro_mc.pokenotifier.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload;
import com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ClientCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // --- Comando Principal del Cliente ---
        LiteralArgumentBuilder<FabricClientCommandSource> pncCommand = ClientCommandManager.literal("pnc");

        // --- Comandos de Alertas ---
        pncCommand
                .then(buildCommandToggle("alert_sound", "Alert Sounds", (config, enabled) -> config.alert_sounds_enabled = enabled))
                .then(buildCommandToggle("alert_toast", "HUD (Toast) alerts", (config, enabled) -> config.alert_toast_enabled = enabled))
                .then(buildCommandToggle("alert_chat", "Chat alerts", (config, enabled) -> config.alert_chat_enabled = enabled));

        // --- NUEVO: Comando Silent ---
        pncCommand.then(ClientCommandManager.literal("silent")
                .then(ClientCommandManager.literal("ON").executes(context -> {
                    ConfigClient config = ConfigManager.getClientConfig();
                    config.alert_sounds_enabled = false;
                    config.alert_toast_enabled = false;
                    config.alert_chat_enabled = false;
                    config.silent_mode_enabled = true;
                    config.searching_enabled = false; // Detiene la búsqueda
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
                    config.searching_enabled = true; // Reanuda la búsqueda
                    ConfigManager.saveClientConfigToFile();
                    context.getSource().sendFeedback(Text.literal("Silent mode is now OFF. Notifications are enabled.").formatted(Formatting.GREEN));
                    return 1;
                })));

        // --- Comando Custom List ---
        var customListCommand = ClientCommandManager.literal("customlist")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("pokemon", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String pokemonName = StringArgumentType.getString(context, "pokemon");
                                    ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.ADD, pokemonName));
                                    context.getSource().sendFeedback(Text.literal("Request sent to add ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" to your custom list.").formatted(Formatting.YELLOW));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("pokemon", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String pokemonName = StringArgumentType.getString(context, "pokemon");
                                    ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.REMOVE, pokemonName));
                                    context.getSource().sendFeedback(Text.literal("Request sent to remove ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" from your custom list.").formatted(Formatting.YELLOW));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("list")
                        .executes(context -> {
                            ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.LIST, ""));
                            context.getSource().sendFeedback(Text.literal("Requesting your custom list from the server...").formatted(Formatting.YELLOW));
                            return 1;
                        }))
                .then(ClientCommandManager.literal("clear")
                        .executes(context -> {
                            ClientPlayNetworking.send(new CustomListUpdatePayload(CustomListUpdatePayload.Action.CLEAR, ""));
                            context.getSource().sendFeedback(Text.literal("Request sent to clear your custom list.").formatted(Formatting.YELLOW));
                            return 1;
                        }));

        pncCommand.then(customListCommand);

        // --- NUEVO: Comando Catch 'em All ---
        var catchemallCommand = ClientCommandManager.literal("catchemall")
                .then(ClientCommandManager.literal("enable")
                        .then(ClientCommandManager.argument("generation", StringArgumentType.string())
                                .executes(context -> {
                                    String genName = StringArgumentType.getString(context, "generation");
                                    ClientPlayNetworking.send(new CatchemallUpdatePayload(CatchemallUpdatePayload.Action.ENABLE, genName));
                                    context.getSource().sendFeedback(Text.literal("Requesting to enable Catch 'em All mode for " + genName + "...").formatted(Formatting.YELLOW));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("disable")
                        .then(ClientCommandManager.argument("generation", StringArgumentType.string())
                                .executes(context -> {
                                    String genName = StringArgumentType.getString(context, "generation");
                                    ClientPlayNetworking.send(new CatchemallUpdatePayload(CatchemallUpdatePayload.Action.DISABLE, genName));
                                    context.getSource().sendFeedback(Text.literal("Requesting to disable Catch 'em All mode for " + genName + "...").formatted(Formatting.YELLOW));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("list")
                        .executes(context -> {
                            ClientPlayNetworking.send(new CatchemallUpdatePayload(CatchemallUpdatePayload.Action.LIST, ""));
                            context.getSource().sendFeedback(Text.literal("Requesting your active Catch 'em All modes...").formatted(Formatting.YELLOW));
                            return 1;
                        }));

        pncCommand.then(catchemallCommand);

        // --- Comando de Versión (Cliente) ---
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

        // --- Comandos de Test Mode (Placeholder - Cliente) ---
        pncCommand.then(ClientCommandManager.literal("test_mode")
                .then(ClientCommandManager.literal("ON").executes(context -> {
                    context.getSource().sendFeedback(Text.literal("This command will be implemented in a future update.").formatted(Formatting.GRAY));
                    return 1;
                }))
                .then(ClientCommandManager.literal("OFF").executes(context -> {
                    context.getSource().sendFeedback(Text.literal("This command will be implemented in a future update.").formatted(Formatting.GRAY));
                    return 1;
                })));

        // --- NUEVO: Comando de Estado ---
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
                    //sendStatusLine(context.getSource(), "Test Mode", config.enable_test_mode); // Descomentar cuando se implemente
                    return 1;
                }));


        // --- Redirección de Comandos de Servidor ---
        var serverCommandNode = dispatcher.getRoot().getChild("pokenotifier");
        if (serverCommandNode != null) {
            pncCommand.then(ClientCommandManager.literal("reloadconfig").redirect(serverCommandNode));
        }

        dispatcher.register(pncCommand);
    }

    // --- Método de Ayuda para crear comandos ON/OFF ---
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

    // --- NUEVO: Método de Ayuda para el comando status ---
    private static void sendStatusLine(FabricClientCommandSource source, String label, boolean isEnabled) {
        MutableText message = Text.literal(label + " = ").formatted(Formatting.WHITE);
        if (isEnabled) {
            message.append(Text.literal("ON").formatted(Formatting.GREEN));
        } else {
            message.append(Text.literal("OFF").formatted(Formatting.RED));
        }
        source.sendFeedback(message);
    }

    // Interfaz funcional para hacer el código más limpio
    @FunctionalInterface
    private interface ToggleAction {
        void apply(ConfigClient config, boolean enabled);
    }
}