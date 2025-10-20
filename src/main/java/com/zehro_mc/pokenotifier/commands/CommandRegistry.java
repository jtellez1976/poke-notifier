/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zehro_mc.pokenotifier.commands.handlers.AdminCommandHandler;
import com.zehro_mc.pokenotifier.commands.handlers.UserCommandHandler;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.registry.RegistryWrapper;

/**
 * Central registry for all Poke Notifier commands.
 * Handles the registration and organization of command trees.
 */
public class CommandRegistry {
    
    /**
     * Registers all Poke Notifier commands with the command dispatcher.
     * 
     * @param dispatcher The command dispatcher
     * @param registryAccess Registry access for command registration
     * @param environment The command environment
     */
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, 
                                      RegistryWrapper.WrapperLookup registryAccess, 
                                      CommandManager.RegistrationEnvironment environment) {
        
        // Create the main pokenotifier command node
        var pokenotifierNode = CommandManager.literal("pokenotifier");
        
        // Register user commands (available to all players)
        UserCommandHandler.registerUserCommands(pokenotifierNode);
        
        // Register admin commands (OP only)
        AdminCommandHandler.registerAdminCommands(pokenotifierNode);
        
        // Register the complete command tree
        dispatcher.register(pokenotifierNode);
    }
}