/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Defines the properties for each prestige rank, including its display text,
 * color, and the associated icon texture.
 */
public enum Rank {
    TRAINEE("[Trainee]", Formatting.GRAY, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup0.png")),
    GREAT("[Great]", Formatting.GOLD, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup1.png")),
    EXPERT("[Expert]", Formatting.AQUA, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup2.png")),
    VETERAN("[Veteran]", Formatting.YELLOW, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup3.png")),
    MASTER("[Master]", Formatting.LIGHT_PURPLE, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup4_master.png"));

    public final Text displayText;
    public final Identifier icon;

    Rank(String text, Formatting color, Identifier icon) {
        this.displayText = Text.literal(text).formatted(color);
        this.icon = icon;
    }
}