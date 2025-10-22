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
    
    // Custom icons
    public static final Identifier NOTIFICATIONS = Identifier.of("poke-notifier", "textures/gui/icons/notifications.png");
    
    // Fallback to pokeball for others until more icons are created
    public static final Identifier POKEBALL = Identifier.of("poke-notifier", "textures/gui/pokeball-icon.png");
    public static final Identifier SETTINGS = POKEBALL;
    public static final Identifier REFRESH = POKEBALL;
    public static final Identifier INFO = POKEBALL;
    public static final Identifier WARNING = POKEBALL;
    public static final Identifier SUCCESS = POKEBALL;
    public static final Identifier ERROR = POKEBALL;
    
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