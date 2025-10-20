/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.commands.handlers;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Handles admin-only commands (OP level 2+ required).
 */
public class AdminCommandHandler {
    
    /**
     * Registers all admin commands to the main command node.
     * 
     * @param mainNode The main pokenotifier command node
     */
    public static void registerAdminCommands(LiteralArgumentBuilder<ServerCommandSource> mainNode) {
        // TODO: Move admin commands from PokeNotifier.java here
        // This will be implemented in the next step
    }
}