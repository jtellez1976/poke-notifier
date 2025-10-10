package com.zehro_mc.pokenotifier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zehro_mc.pokenotifier.ConfigServer;
import com.zehro_mc.pokenotifier.ConfigManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DebugModeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> debugCommand = CommandManager.literal("debug_mode")
                .requires(source -> source.hasPermissionLevel(2)) // Solo OPs
                .then(CommandManager.literal("enable")
                        .executes(context -> {
                            ConfigServer config = ConfigManager.getServerConfig();
                            config.debug_mode_enabled = true;
                            ConfigManager.saveServerConfigToFile();
                            context.getSource().sendFeedback(() -> Text.literal("Debug mode enabled. Verbose logging is now ON.").formatted(Formatting.GREEN), true);
                            return 1;
                        }))
                .then(CommandManager.literal("disable")
                        .executes(context -> {
                            ConfigServer config = ConfigManager.getServerConfig();
                            config.debug_mode_enabled = false;
                            ConfigManager.saveServerConfigToFile();
                            context.getSource().sendFeedback(() -> Text.literal("Debug mode disabled. Verbose logging is now OFF.").formatted(Formatting.RED), true);
                            return 1;
                        }));

        dispatcher.getRoot().getChild("pokenotifier").addChild(debugCommand.build());
    }
}