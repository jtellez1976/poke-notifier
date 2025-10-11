package com.zehro_mc.pokenotifier.api;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;

import java.util.stream.Stream;

/**
 * Una clase de utilidad para exponer partes de la API de Poke Notifier o interactuar con Cobblemon.
 */
public class PokeNotifierApi {

    public static Stream<String> getAllPokemonNames() {
        // La "fuente de la verdad" es la lista completa de especies registradas en Cobblemon.
        // Cada 'Species' en esta lista, incluidas las formas, tiene su propio ResourceIdentifier.
        return PokemonSpecies.INSTANCE.getSpecies().stream().map(species -> species.getResourceIdentifier().getPath());
    }
}