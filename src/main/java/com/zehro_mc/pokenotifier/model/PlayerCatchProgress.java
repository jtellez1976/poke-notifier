package com.zehro_mc.pokenotifier.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerCatchProgress {

    // --- NUEVA BANDERA ---
    // Esta bandera asegura que la sincronización masiva del PC solo ocurra una vez.
    public boolean initialPcSyncCompleted = false;

    public Set<String> active_generations = new HashSet<>();
    public Map<String, Set<String>> caught_pokemon = new HashMap<>();

}