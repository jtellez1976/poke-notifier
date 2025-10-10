package com.zehro_mc.pokenotifier.util;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigPokemon;
import net.minecraft.util.Formatting;

public class RarityUtil {

    public enum RarityCategory {
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

    public static RarityCategory getRarity(Pokemon pokemon) {
        String name = pokemon.getSpecies().getName().toLowerCase();
        // --- LA CORRECCIÓN CLAVE ---
        // Siempre obtenemos la configuración más reciente del ConfigManager.
        // NO creamos una nueva instancia.
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