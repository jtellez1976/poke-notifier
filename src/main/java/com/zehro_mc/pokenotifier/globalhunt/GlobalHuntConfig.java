/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.globalhunt;

import net.minecraft.util.Identifier;
import com.zehro_mc.pokenotifier.PokeNotifier;

import java.util.*;

public class GlobalHuntConfig {
    // Default configuration values
    private boolean enabled = true;
    private int minIntervalHours = 2;
    private int maxIntervalHours = 6;
    private int eventDurationMinutes = 30;
    private int maxSpawnDistance = 10000;
    private double shinyChance = 0.1; // 10% chance for shiny
    
    // World configuration
    private Set<String> enabledWorlds = new HashSet<>(Arrays.asList(
        "minecraft:overworld",
        "minecraft:the_nether", 
        "minecraft:the_end"
    ));
    
    // Pokemon configuration with weights
    private Map<String, Integer> pokemonPool = new HashMap<>();
    private Random random = new Random();
    
    public GlobalHuntConfig() {
        initializeDefaultPokemonPool();
    }
    
    private void initializeDefaultPokemonPool() {
        // Legendary Pokemon with different rarities (weight = rarity)
        pokemonPool.put("mewtwo", 1);        // Ultra rare
        pokemonPool.put("mew", 1);           // Ultra rare
        pokemonPool.put("lugia", 2);         // Very rare
        pokemonPool.put("ho-oh", 2);         // Very rare
        pokemonPool.put("rayquaza", 2);      // Very rare
        pokemonPool.put("dialga", 3);        // Rare
        pokemonPool.put("palkia", 3);        // Rare
        pokemonPool.put("giratina", 3);      // Rare
        pokemonPool.put("arceus", 1);        // Ultra rare
        pokemonPool.put("reshiram", 3);      // Rare
        pokemonPool.put("zekrom", 3);        // Rare
        pokemonPool.put("kyurem", 3);        // Rare
        
        // Mythical Pokemon
        pokemonPool.put("celebi", 2);        // Very rare
        pokemonPool.put("jirachi", 2);       // Very rare
        pokemonPool.put("deoxys", 2);        // Very rare
        pokemonPool.put("darkrai", 2);       // Very rare
        pokemonPool.put("shaymin", 3);       // Rare
        
        // Pseudo-legendaries (more common in Global Hunts)
        pokemonPool.put("dragonite", 5);     // Uncommon
        pokemonPool.put("tyranitar", 5);     // Uncommon
        pokemonPool.put("salamence", 5);     // Uncommon
        pokemonPool.put("metagross", 5);     // Uncommon
        pokemonPool.put("garchomp", 5);      // Uncommon
        pokemonPool.put("hydreigon", 5);     // Uncommon
    }
    
    public String getRandomPokemon() {
        if (pokemonPool.isEmpty()) {
            PokeNotifier.LOGGER.warn("Pokemon pool is empty, using default Mewtwo");
            return "mewtwo";
        }
        
        // Calculate total weight
        int totalWeight = pokemonPool.values().stream().mapToInt(Integer::intValue).sum();
        
        // Generate random number
        int randomValue = random.nextInt(totalWeight);
        
        // Select Pokemon based on weight
        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : pokemonPool.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                return entry.getKey();
            }
        }
        
        // Fallback (should never reach here)
        return pokemonPool.keySet().iterator().next();
    }
    
    public boolean isWorldEnabled(Identifier worldId) {
        return enabledWorlds.contains(worldId.toString());
    }
    
    public void setWorldEnabled(String worldId, boolean enabled) {
        if (enabled) {
            enabledWorlds.add(worldId);
        } else {
            enabledWorlds.remove(worldId);
        }
    }
    
    public void addPokemon(String pokemon, int weight) {
        pokemonPool.put(pokemon.toLowerCase(), Math.max(1, weight));
    }
    
    public void removePokemon(String pokemon) {
        pokemonPool.remove(pokemon.toLowerCase());
    }
    
    public boolean hasPokemon(String pokemon) {
        return pokemonPool.containsKey(pokemon.toLowerCase());
    }
    
    public int getPokemonWeight(String pokemon) {
        return pokemonPool.getOrDefault(pokemon.toLowerCase(), 0);
    }
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getMinIntervalHours() { return minIntervalHours; }
    public void setMinIntervalHours(int hours) { this.minIntervalHours = Math.max(1, hours); }
    
    public int getMaxIntervalHours() { return maxIntervalHours; }
    public void setMaxIntervalHours(int hours) { 
        this.maxIntervalHours = Math.max(minIntervalHours, hours); 
    }
    
    public int getEventDurationMinutes() { return eventDurationMinutes; }
    public void setEventDurationMinutes(int minutes) { this.eventDurationMinutes = Math.max(5, minutes); }
    
    public int getMaxSpawnDistance() { return maxSpawnDistance; }
    public void setMaxSpawnDistance(int distance) { this.maxSpawnDistance = Math.max(1000, distance); }
    
    public double getShinyChance() { return shinyChance; }
    public void setShinyChance(double chance) { this.shinyChance = Math.max(0.0, Math.min(1.0, chance)); }
    
    public Set<String> getEnabledWorlds() { return new HashSet<>(enabledWorlds); }
    public Map<String, Integer> getPokemonPool() { return new HashMap<>(pokemonPool); }
    
    // Utility methods for admin interface
    public List<String> getAvailablePokemon() {
        return new ArrayList<>(pokemonPool.keySet());
    }
    
    public void resetToDefaults() {
        enabled = true;
        minIntervalHours = 2;
        maxIntervalHours = 6;
        eventDurationMinutes = 30;
        maxSpawnDistance = 10000;
        shinyChance = 0.1;
        
        enabledWorlds.clear();
        enabledWorlds.addAll(Arrays.asList(
            "minecraft:overworld",
            "minecraft:the_nether", 
            "minecraft:the_end"
        ));
        
        pokemonPool.clear();
        initializeDefaultPokemonPool();
    }
}