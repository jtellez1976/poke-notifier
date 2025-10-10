package com.zehro_mc.pokenotifier.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
                    sendStatusLine(context.getSource(), "Alert Sounds", config.alert_sounds_enabled);
                    sendStatusLine(context.getSource(), "Chat Alerts", config.alert_chat_enabled);
                    sendStatusLine(context.getSource(), "Toast Alerts (HUD)", config.alert_toast_enabled);
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