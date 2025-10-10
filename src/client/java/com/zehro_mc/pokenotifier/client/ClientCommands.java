package com.zehro_mc.pokenotifier.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ClientCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Main command /pokenotifier
        var pokenotifierNode = ClientCommandManager.literal("pokenotifier").build();

        // Subcommand /pokenotifier alertsounds <true|false>
        var alertsoundsNode = ClientCommandManager.literal("alertsounds")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            ConfigClient config = ConfigManager.getClientConfig();
                            config.alert_sounds_enabled = enabled;
                            ConfigManager.saveClientConfigToFile();

                            String status = enabled ? "ON" : "OFF";
                            context.getSource().sendFeedback(Text.literal("Alert sounds are now " + status).formatted(Formatting.GREEN));
                            return 1;
                        })
                ).build();

        // Subcommand /pokenotifier alerttoast <true|false>
        var alerttoastNode = ClientCommandManager.literal("alerttoast")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            ConfigClient config = ConfigManager.getClientConfig();
                            config.alert_toast_enabled = enabled;
                            ConfigManager.saveClientConfigToFile();

                            String status = enabled ? "ON" : "OFF";
                            context.getSource().sendFeedback(Text.literal("HUD (Toast) alerts are now " + status).formatted(Formatting.GREEN));
                            return 1;
                        })
                ).build();

        // Subcommand /pokenotifier alertchat <true|false>
        var alertchatNode = ClientCommandManager.literal("alertchat")
                .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            ConfigClient config = ConfigManager.getClientConfig();
                            config.alert_chat_enabled = enabled;
                            ConfigManager.saveClientConfigToFile();

                            String status = enabled ? "ON" : "OFF";
                            context.getSource().sendFeedback(Text.literal("Chat alerts are now " + status).formatted(Formatting.GREEN));
                            return 1;
                        })
                ).build();

        // Link everything together
        dispatcher.getRoot().addChild(pokenotifierNode);
        pokenotifierNode.addChild(alertsoundsNode);
        pokenotifierNode.addChild(alerttoastNode);
        pokenotifierNode.addChild(alertchatNode);
    }
}