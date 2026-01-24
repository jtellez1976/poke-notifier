/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Manages local client-only data storage for tracking when server doesn't have the mod.
 * Stores data in config/poke-notifier/client-only-data/
 */
public class ClientOnlyDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokeNotifier.MOD_ID + "-ClientData");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final File CLIENT_DATA_DIR = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("poke-notifier")
        .resolve("client-only-data")
        .toFile();
    
    private static final File CUSTOM_HUNT_FILE = new File(CLIENT_DATA_DIR, "custom-hunt-local.json");
    private static final File CATCH_EM_ALL_FILE = new File(CLIENT_DATA_DIR, "catch-em-all-local.json");
    
    // Data structures
    private static CustomHuntData customHuntData = new CustomHuntData();
    private static CatchEmAllData catchEmAllData = new CatchEmAllData();
    
    public static void initialize() {
        // Only initialize if we're in true client-only mode (no server mod)
        if (com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) {
            if (!CLIENT_DATA_DIR.exists()) {
                CLIENT_DATA_DIR.mkdirs();
            }
            
            loadCustomHuntData();
            loadCatchEmAllData();
            
            LOGGER.info("[CLIENT-ONLY] Local data manager initialized");
        } else {
            LOGGER.info("[FULL-SERVER] Skipping client-only data manager (server has mod)");
        }
    }
    
    // Custom Hunt Methods
    public static void addToCustomHunt(String pokemonName) {
        customHuntData.huntList.add(pokemonName.toLowerCase());
        saveCustomHuntData();
    }
    
    public static void removeFromCustomHunt(String pokemonName) {
        customHuntData.huntList.remove(pokemonName.toLowerCase());
        saveCustomHuntData();
    }
    
    public static void clearCustomHunt() {
        customHuntData.huntList.clear();
        saveCustomHuntData();
    }
    
    public static List<String> getCustomHuntList() {
        return new ArrayList<>(customHuntData.huntList);
    }
    
    public static boolean isInCustomHunt(String pokemonName) {
        return customHuntData.huntList.contains(pokemonName.toLowerCase());
    }
    
    // Catch 'em All Methods
    public static void enableCatchEmAll(String generation) {
        catchEmAllData.activeGeneration = generation;
        if (!catchEmAllData.caughtPokemon.containsKey(generation)) {
            catchEmAllData.caughtPokemon.put(generation, new HashSet<>());
        }
        saveCatchEmAllData();
    }
    
    public static void disableCatchEmAll() {
        catchEmAllData.activeGeneration = "none";
        saveCatchEmAllData();
    }
    
    public static String getActiveGeneration() {
        return catchEmAllData.activeGeneration;
    }
    
    public static void addCaughtPokemon(String generation, String pokemonName) {
        Set<String> caught = catchEmAllData.caughtPokemon.computeIfAbsent(generation, k -> new HashSet<>());
        if (caught.add(pokemonName.toLowerCase())) {
            saveCatchEmAllData();
        }
    }
    
    public static int getCaughtCount(String generation) {
        return catchEmAllData.caughtPokemon.getOrDefault(generation, new HashSet<>()).size();
    }
    
    public static Set<String> getCaughtPokemon(String generation) {
        return new HashSet<>(catchEmAllData.caughtPokemon.getOrDefault(generation, new HashSet<>()));
    }
    
    public static boolean hasCaughtSpecies(String speciesName) {
        return catchEmAllData.seenSpecies.contains(speciesName.toLowerCase());
    }
    
    public static void markSpeciesAsSeen(String speciesName) {
        if (catchEmAllData.seenSpecies.add(speciesName.toLowerCase())) {
            saveCatchEmAllData();
        }
    }
    
    public static boolean hasCaught(String generation, String pokemonName) {
        return catchEmAllData.caughtPokemon.getOrDefault(generation, new HashSet<>())
            .contains(pokemonName.toLowerCase());
    }
    
    // Save/Load Methods
    private static void loadCustomHuntData() {
        if (CUSTOM_HUNT_FILE.exists()) {
            try (FileReader reader = new FileReader(CUSTOM_HUNT_FILE)) {
                customHuntData = GSON.fromJson(reader, CustomHuntData.class);
                if (customHuntData == null) {
                    customHuntData = new CustomHuntData();
                }
                // Loaded silently
            } catch (Exception e) {
                LOGGER.error("Failed to load custom hunt data", e);
                customHuntData = new CustomHuntData();
            }
        }
    }
    
    private static void saveCustomHuntData() {
        try (FileWriter writer = new FileWriter(CUSTOM_HUNT_FILE)) {
            GSON.toJson(customHuntData, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save custom hunt data", e);
        }
    }
    
    private static void loadCatchEmAllData() {
        if (CATCH_EM_ALL_FILE.exists()) {
            try (FileReader reader = new FileReader(CATCH_EM_ALL_FILE)) {
                catchEmAllData = GSON.fromJson(reader, CatchEmAllData.class);
                if (catchEmAllData == null) {
                    catchEmAllData = new CatchEmAllData();
                }
                // Loaded silently
            } catch (Exception e) {
                LOGGER.error("Failed to load catch 'em all data", e);
                catchEmAllData = new CatchEmAllData();
            }
        }
    }
    
    private static void saveCatchEmAllData() {
        try (FileWriter writer = new FileWriter(CATCH_EM_ALL_FILE)) {
            GSON.toJson(catchEmAllData, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save catch 'em all data", e);
        }
    }
    
    // Data Classes
    private static class CustomHuntData {
        Set<String> huntList = new HashSet<>();
    }
    
    private static class CatchEmAllData {
        String activeGeneration = "none";
        Map<String, Set<String>> caughtPokemon = new HashMap<>();
        Set<String> seenSpecies = new HashSet<>(); // Track all species ever seen in party
    }
}
