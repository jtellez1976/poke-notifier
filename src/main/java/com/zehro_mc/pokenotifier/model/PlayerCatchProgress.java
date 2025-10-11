package com.zehro_mc.pokenotifier.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerCatchProgress {

    // Generaciones que el jugador está siguiendo activamente (ej: "gen1", "gen2")
    public Set<String> active_generations = new HashSet<>();

    // Mapa para rastrear los Pokémon capturados por generación
    public Map<String, Set<String>> caught_pokemon = new HashMap<>();
}