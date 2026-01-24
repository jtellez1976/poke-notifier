/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.api.PokeNotifierApi;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.client.PokeNotifierClient;
import com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload;
import com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload;
import com.zehro_mc.pokenotifier.networking.UpdateSourcePayload;
import net.minecraft.client.gui.screen.Screen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;


/**
 * Registers and handles all client-side commands, which start with /pnc.
 */
public class ClientCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        LiteralArgumentBuilder<FabricClientCommandSource> pncCommand = ClientCommandManager.literal("pnc");

        // --- ONLY VISIBLE COMMAND: GUI ---
        var guiCommand = ClientCommandManager.literal("gui")
                .executes(context -> {
                    context.getSource().getPlayer().networkHandler.sendChatCommand("pokenotifier gui");
                    return 1;
                });

        // Register the only visible command
        pncCommand.then(guiCommand);

        dispatcher.register(pncCommand);
    }

    private static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(PokeNotifier.MOD_ID)
                .map(ModContainer::getMetadata)
                .map(meta -> meta.getVersion().getFriendlyString())
                .orElse("Unknown");
    }
}