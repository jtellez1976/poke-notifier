package com.zehro_mc.pokenotifier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ReloadConfigCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> pokenotifierCommand = CommandManager.literal("pokenotifier")
                .requires(source -> source.hasPermissionLevel(2)); // Permiso base para el comando principal

        // Subcomando: /pokenotifier reloadconfig
        pokenotifierCommand.then(CommandManager.literal("reloadconfig")
                .executes(context -> {
                    try {
                        ConfigManager.loadConfig();
                        context.getSource().sendFeedback(() -> Text.literal("Poke Notifier configuration reloaded successfully.").formatted(Formatting.GREEN), true);
                        return 1; // Éxito
                    } catch (ConfigManager.ConfigReadException e) {
                        PokeNotifier.LOGGER.error("Failed to reload Poke Notifier configuration: {}", e.getMessage());
                        context.getSource().sendError(Text.literal("Error reloading config: " + e.getMessage()).formatted(Formatting.RED));
                        context.getSource().sendError(Text.literal("To generate a new default config, use: /pokenotifier reloadconfig new").formatted(Formatting.YELLOW));
                        return 0; // Fallo
                    }
                })
                // Subcomando anidado: /pokenotifier reloadconfig new
                .then(CommandManager.literal("new")
                        .executes(context -> {
                            try {
                                ConfigManager.resetToDefault();
                                context.getSource().sendFeedback(() -> Text.literal("A new default poke-notifier.json has been generated.").formatted(Formatting.GREEN), true);
                                return 1; // Éxito
                            } catch (Exception e) {
                                PokeNotifier.LOGGER.error("Failed to generate new default configuration.", e);
                                context.getSource().sendError(Text.literal("Failed to generate new config. Check server logs.").formatted(Formatting.RED));
                                return 0; // Fallo
                            }
                        })
                )
        );

        dispatcher.register(pokenotifierCommand);
    }
}