/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Registers the /pokenotifier help command for administrators.
 */
public class HelpCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var helpCommand = CommandManager.literal("help")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    source.sendFeedback(() -> Text.literal("--- Poke Notifier Admin Help ---").formatted(Formatting.GOLD), false);
                    source.sendFeedback(() -> Text.literal("/pokenotifier status").formatted(Formatting.AQUA).append(Text.literal(" - Shows server config status.").formatted(Formatting.WHITE)), false);
                    source.sendFeedback(() -> Text.literal("/pokenotifier reloadconfig").formatted(Formatting.AQUA).append(Text.literal(" - Reloads all config files.").formatted(Formatting.WHITE)), false);
                    source.sendFeedback(() -> Text.literal("/pokenotifier reloadconfig new").formatted(Formatting.AQUA).append(Text.literal(" - Generates new default configs.").formatted(Formatting.WHITE)), false);
                    source.sendFeedback(() -> Text.literal("/pokenotifier debug_mode <enable/disable>").formatted(Formatting.AQUA).append(Text.literal(" - Toggles detailed console logs.").formatted(Formatting.WHITE)), false);
                    source.sendFeedback(() -> Text.literal("/pokenotifier test_mode <enable/disable>").formatted(Formatting.AQUA).append(Text.literal(" - Toggles notifications for non-natural spawns.").formatted(Formatting.WHITE)), false);
                    source.sendFeedback(() -> Text.literal("/pokenotifier testspawn <pokemon> [shiny]").formatted(Formatting.AQUA).append(Text.literal(" - Spawns a Pokémon for testing.").formatted(Formatting.WHITE)), false);
                    source.sendFeedback(() -> Text.literal("/pokenotifier autocompletegen <player> <gen>").formatted(Formatting.AQUA).append(Text.literal(" - Autocompletes a gen for a player.").formatted(Formatting.WHITE)), false);
                    source.sendFeedback(() -> Text.literal("/pokenotifier rollback <player>").formatted(Formatting.AQUA).append(Text.literal(" - Restores a player's progress from a backup.").formatted(Formatting.WHITE)), false);
                    return 1;
                }).build();

        dispatcher.getRoot().getChild("pokenotifier").addChild(helpCommand);
    }
}