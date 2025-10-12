package com.zehro_mc.pokenotifier.util;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigPokemon;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public class RarityUtil {

    public enum RarityCategory {
        CUSTOM(Formatting.LIGHT_PURPLE),
        HUNT(Formatting.GREEN), // NUEVA CATEGORÍA PARA CATCH 'EM ALL
        SHINY(Formatting.YELLOW), // NUEVA CATEGORÍA PARA SHINIES
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
        // Usamos el path del Identifier para tener el nombre limpio y consistente (ej: nidoran_f, mr_mime)
        String name = pokemon.getForm().getSpecies().getResourceIdentifier().getPath();

        // --- HERRAMIENTA DE DIAGNÓSTICO ---
        if (ConfigManager.getServerConfig().debug_mode_enabled) {
            PokeNotifier.LOGGER.info("[DIAGNOSTIC] RarityUtil checking Pokémon. Name obtained: '{}'", name);
        }

        // Prioridad 1: Lista personalizada del jugador
        if (player != null) {
            CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());
            if (playerConfig.tracked_pokemon.contains(name)) {
                return RarityCategory.CUSTOM;
            }
        }

        // Prioridad 1.5: Pokémon Shiny
        if (pokemon.getShiny()) {
            return RarityCategory.SHINY;
        }

        // Prioridad 2: Listas globales del servidor
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