package com.zehro_mc.pokenotifier.util;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.Config;
import com.zehro_mc.pokenotifier.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class RarityUtil {

    public enum RarityCategory {
        LEGENDARY("LEGENDARY", 0xFFD700, NamedTextColor.GOLD),
        MYTHICAL("MYTHICAL", 0xFF69B4, NamedTextColor.LIGHT_PURPLE),
        ULTRA_BEAST("ULTRA_BEAST", 0x00FFFF, NamedTextColor.AQUA),
        PARADOX("PARADOX", 0x9400D3, TextColor.color(0x9400D3)), // Using a custom color for Paradox
        PSEUDO_LEGENDARY("PSEUDO_LEGENDARY", 0x8A2BE2, TextColor.color(0x8A2BE2)),
        RARE("RARE", 0x00BFFF, NamedTextColor.AQUA),
        UNCOMMON("UNCOMMON", 0x32CD32, NamedTextColor.GREEN),
        SHINY("SHINY", 0xFFFF00, NamedTextColor.YELLOW),
        NONE("NONE", 0xFFFFFF, NamedTextColor.WHITE);

        private final String key;
        private final int waypointColor;
        private final TextColor chatColor;

        RarityCategory(String key, int waypointColor, TextColor chatColor) {
            this.key = key;
            this.waypointColor = waypointColor;
            this.chatColor = chatColor;
        }

        public int getWaypointColor() {
            return this.waypointColor;
        }

        public TextColor getChatColor() {
            return this.chatColor;
        }

        public Component getRarityName() {
            return Component.translatable("poke-notifier.rarity." + this.key);
        }
    }

    public static RarityCategory getRarity(Pokemon pokemon) {
        Config config = ConfigManager.getConfig();
        String pokemonName = pokemon.getSpecies().getName().toLowerCase().replace(" ", "").replace("-", ""); // Normalise name

        if (pokemon.getAspects().contains("shiny")) {
            return RarityCategory.SHINY;
        }

        // Check from most to least rare, based on the new config lists
        if (config.MYTHICAL.contains(pokemonName)) {
            return RarityCategory.MYTHICAL;
        }
        if (config.LEGENDARY.contains(pokemonName)) {
            return RarityCategory.LEGENDARY;
        }
        if (config.ULTRA_BEAST.contains(pokemonName)) {
            return RarityCategory.ULTRA_BEAST;
        }
        if (config.PARADOX.contains(pokemonName)) {
            return RarityCategory.PARADOX;
        }
        if (config.PSEUDO_LEGENDARY.contains(pokemonName)) {
            return RarityCategory.PSEUDO_LEGENDARY;
        }
        if (config.RARE.contains(pokemonName)) {
            return RarityCategory.RARE;
        }
        if (config.UNCOMMON.contains(pokemonName)) {
            return RarityCategory.UNCOMMON;
        }

        return RarityCategory.NONE;
    }

    public static RarityCategory getRarityFromColor(int color) {
        for (RarityCategory category : RarityCategory.values()) {
            if (category.getWaypointColor() == color) {
                return category;
            }
        }
        return RarityCategory.NONE;
    }
}