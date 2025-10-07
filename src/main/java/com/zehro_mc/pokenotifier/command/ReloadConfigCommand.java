package com.zehro_mc.pokenotifier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zehro_mc.pokenotifier.ConfigManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public class ReloadConfigCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("pokenotifier")
                .then(CommandManager.literal("reloadconfig")
                        .requires(source -> source.hasPermissionLevel(2)) // Only for OPs
                        .executes(ReloadConfigCommand::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context) {
        ConfigManager.loadConfig();
        context.getSource().sendFeedback(() -> Text.literal("Poke-Notifier configuration reloaded!").formatted(Formatting.GREEN), true);
        return 1;
    }
}