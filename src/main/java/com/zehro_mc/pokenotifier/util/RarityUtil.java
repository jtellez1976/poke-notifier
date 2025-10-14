/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigPokemon;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

/**
 * Determines the rarity of a Pokémon based on a prioritized list of criteria.
 */
public class RarityUtil {

    public enum RarityCategory {
        CUSTOM(Formatting.LIGHT_PURPLE), // For a player's personal hunt list.
        HUNT(Formatting.GREEN),          // For the "Catch 'em All" mode.
        SHINY(Formatting.YELLOW),
        LEGENDARY(Formatting.GOLD),
        MYTHICAL(Formatting.AQUA),
        ULTRA_BEAST(Formatting.BLUE),
        PARADOX(Formatting.LIGHT_PURPLE),
        ULTRA_RARE(Formatting.RED),
        RARE(Formatting.YELLOW),
        COMMON(Formatting.WHITE);

        private final Formatting chatColor;

        RarityCategory(Formatting chatColor) {
            this.chatColor = chatColor;
        }

        public Formatting getChatColor() {
            return chatColor;
        }

        public int getWaypointColor() {
            return chatColor.getColorValue() != null ? chatColor.getColorValue() : 0xFFFFFF;
        }
    }

    public static RarityCategory getRarity(Pokemon pokemon, ServerPlayerEntity player) {
        // Use the identifier path for a clean, consistent name (e.g., 'nidoran_f', 'mr_mime').
        String name = pokemon.getForm().getSpecies().getResourceIdentifier().getPath();

        if (player != null) {
            CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());
            if (playerConfig.tracked_pokemon.contains(name)) {
                return RarityCategory.CUSTOM;
            }
        }

        if (pokemon.getShiny()) {
            return RarityCategory.SHINY;
        }

        ConfigPokemon configPokemon = ConfigManager.getPokemonConfig();

        if (configPokemon.LEGENDARY.contains(name)) {
            return RarityCategory.LEGENDARY;
        }
        if (configPokemon.MYTHICAL.contains(name)) {
            return RarityCategory.MYTHICAL;
        }
        if (configPokemon.ULTRA_BEAST.contains(name)) {
            return RarityCategory.ULTRA_BEAST;
        }
        if (configPokemon.PARADOX.contains(name)) {
            return RarityCategory.PARADOX;
        }
        if (configPokemon.ULTRA_RARE.contains(name)) {
            return RarityCategory.ULTRA_RARE;
        }
        if (configPokemon.RARE.contains(name)) {
            return RarityCategory.RARE;
        }

        return RarityCategory.COMMON;
    }
}