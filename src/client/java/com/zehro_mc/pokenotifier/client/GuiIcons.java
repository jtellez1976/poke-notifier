/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Utility class for our custom GUI icons
 */
public class GuiIcons {
    
    // User Tools icons
    public static final Identifier NOTIFICATIONS = Identifier.of("poke-notifier", "textures/gui/icons/notifications.png");
    public static final Identifier CUSTOM_HUNT = Identifier.of("poke-notifier", "textures/gui/icons/custom_hunt.png");
    public static final Identifier CATCH_EM_ALL = Identifier.of("poke-notifier", "textures/gui/icons/catch_em_all.png");
    public static final Identifier MAP_SETTINGS = Identifier.of("poke-notifier", "textures/gui/icons/map_settings.png");
    public static final Identifier INFO_HELP = Identifier.of("poke-notifier", "textures/gui/icons/info_help.png");
    
    // Toggle states
    public static final Identifier ON = Identifier.of("poke-notifier", "textures/gui/icons/on.png");
    public static final Identifier OFF = Identifier.of("poke-notifier", "textures/gui/icons/off.png");
    
    // Admin Tools icons
    public static final Identifier SYSTEM_STATUS = Identifier.of("poke-notifier", "textures/gui/icons/system_status.png");
    public static final Identifier SERVER_CONTROL = Identifier.of("poke-notifier", "textures/gui/icons/server_control.png");
    public static final Identifier PLAYER_DATA = Identifier.of("poke-notifier", "textures/gui/icons/player_data.png");
    public static final Identifier TESTING = Identifier.of("poke-notifier", "textures/gui/icons/testing.png");
    
    // Events icons
    public static final Identifier GLOBAL_HUNT = Identifier.of("poke-notifier", "textures/gui/icons/global_hunt.png");
    public static final Identifier BOUNTY_SYSTEM = Identifier.of("poke-notifier", "textures/gui/icons/bounty_system.png");
    public static final Identifier SWARM_EVENTS = Identifier.of("poke-notifier", "textures/gui/icons/swarm_events.png");
    public static final Identifier RIVAL_BATTLES = Identifier.of("poke-notifier", "textures/gui/icons/rival_battles.png");
    
    // Action icons
    public static final Identifier ADD = Identifier.of("poke-notifier", "textures/gui/icons/add.png");
    public static final Identifier CLEAR = Identifier.of("poke-notifier", "textures/gui/icons/clear.png");
    public static final Identifier VIEW_LIST = Identifier.of("poke-notifier", "textures/gui/icons/view_list.png");
    public static final Identifier TRACK_GEN = Identifier.of("poke-notifier", "textures/gui/icons/track_gen.png");
    public static final Identifier START_EVENT = Identifier.of("poke-notifier", "textures/gui/icons/start_event.png");
    public static final Identifier SHINY = Identifier.of("poke-notifier", "textures/gui/icons/shiny.png");
    public static final Identifier REFRESH = Identifier.of("poke-notifier", "textures/gui/icons/refresh.png");
    
    // Main tab icons
    public static final Identifier USER_TOOLS = Identifier.of("poke-notifier", "textures/gui/icons/user_tools.png");
    public static final Identifier EVENTS = Identifier.of("poke-notifier", "textures/gui/icons/events.png");
    public static final Identifier ADMIN = Identifier.of("poke-notifier", "textures/gui/icons/admin.png");
    
    // Legacy aliases
    public static final Identifier SETTINGS = MAP_SETTINGS;
    public static final Identifier INFO = INFO_HELP;
    public static final Identifier SUCCESS = CUSTOM_HUNT;
    public static final Identifier WARNING = CATCH_EM_ALL;
    
    /**
     * Render an icon at the specified position
     */
    public static void renderIcon(DrawContext context, Identifier iconId, int x, int y, int size) {
        context.drawTexture(iconId, x, y, 0, 0, size, size, size, size);
    }
    
    /**
     * Render an icon with default 16x16 size
     */
    public static void renderIcon(DrawContext context, Identifier iconId, int x, int y) {
        renderIcon(context, iconId, x, y, 16);
    }
}