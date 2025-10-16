/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import com.zehro_mc.pokenotifier.model.BountyRewardsConfig;
import com.zehro_mc.pokenotifier.model.CatchemallRewardsConfig;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
import com.zehro_mc.pokenotifier.util.DataSecurityUtil;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages loading, saving, and caching all configuration files for the mod.
 * This includes server settings, client preferences, player data, and Pokémon lists.
 */
public class ConfigManager {

    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(PokeNotifier.MOD_ID).toFile();
    private static final File PLAYER_DATA_DIR = new File(CONFIG_DIR, "player_data");
    public static final File CATCH_PROGRESS_DIR = new File(CONFIG_DIR, "catch_progress");
    private static final File EVENTS_DIR = new File(CONFIG_DIR, "events"); // New directory for event configs
    private static final File CONFIG_CLIENT_FILE = new File(CONFIG_DIR, "config-client.json");
    private static final File CONFIG_SERVER_FILE = new File(CONFIG_DIR, "config-server.json");
    private static final File CATCHEMALL_REWARDS_FILE = new File(EVENTS_DIR, "catchemall_rewards.json"); // Moved
    private static final File BOUNTY_REWARDS_FILE = new File(EVENTS_DIR, "bounty_rewards.json"); // New

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigPokemon configPokemon;
    private static ConfigClient configClient;
    private static CatchemallRewardsConfig catchemallRewardsConfig;
    private static BountyRewardsConfig bountyRewardsConfig;
    private static ConfigServer configServer;

    // Caches player-specific data to avoid constant file I/O.
    private static final Map<UUID, CustomListConfig> playerConfigs = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerCatchProgress> playerCatchProgress = new ConcurrentHashMap<>();

    // Caches generation data loaded from the mod's resources.
    private static final Map<String, GenerationData> generationDataCache = new ConcurrentHashMap<>();


    public static class ConfigReadException extends Exception {
        public ConfigReadException(String message, Throwable cause) {
            super(message, cause);
        }
        public ConfigReadException(String message) {
            super(message);
        }
    }

    public static void loadConfig() throws ConfigReadException {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        if (!PLAYER_DATA_DIR.exists()) {
            PLAYER_DATA_DIR.mkdirs();
        }
        if (!CATCH_PROGRESS_DIR.exists()) {
            CATCH_PROGRESS_DIR.mkdirs();
        }
        if (!EVENTS_DIR.exists()) { // Create the new events directory
            EVENTS_DIR.mkdirs();
        }

        EnvType env = FabricLoader.getInstance().getEnvironmentType();

        // Load environment-specific configuration files.
        if (env == EnvType.CLIENT) {
            loadConfigClient();
        } else { // Server-side environment
            loadConfigServer();
            loadCatchemallRewardsConfig();
            loadBountyRewardsConfig();
        }
    }

    public static void saveConfig() {
        saveClientConfigToFile();
        saveServerConfigToFile();
        saveCatchemallRewardsConfig();
        saveBountyRewardsConfig();
    }

    public static void resetToDefault() {
        EnvType env = FabricLoader.getInstance().getEnvironmentType();

        configPokemon = new ConfigPokemon();
        catchemallRewardsConfig = new CatchemallRewardsConfig();
        bountyRewardsConfig = new BountyRewardsConfig();

        if (env == EnvType.CLIENT) {
            configClient = new ConfigClient();
        }
        // Siempre creamos una instancia de la config del servidor para que esté disponible
        configServer = new ConfigServer();

        PokeNotifier.LOGGER.info("Generated new default Poke Notifier configurations.");
        saveConfig();
    }

    private static <T> T loadConfigFile(File file, Class<T> configClass, String configName) throws ConfigReadException {
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                // --- MEJORA: Lógica de Migración ---
                // 1. Cargar como un objeto JSON genérico para leer la versión de forma segura.
                JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                if (jsonObject == null) {
                    throw new ConfigReadException("The " + configName + " file is empty or invalid.");
                }

                // --- MEJORA: Envolvemos la lógica de reflexión en un try-catch para manejar errores ---
                try {
                    int fileVersion = jsonObject.has("config_version") ? jsonObject.get("config_version").getAsInt() : 0;
                    T newConfigInstance = configClass.getDeclaredConstructor().newInstance();
                    int currentVersion = configClass.getField("config_version").getInt(newConfigInstance);

                    if (fileVersion < currentVersion) {
                        PokeNotifier.LOGGER.info("Migrating " + configName + " from version " + fileVersion + " to " + currentVersion + "...");
                        // 2. Cargar el archivo antiguo en su clase para obtener los valores existentes.
                        T oldConfig = GSON.fromJson(jsonObject, configClass);

                        // 3. Copiar los valores antiguos al nuevo objeto de configuración.
                        for (Field field : configClass.getDeclaredFields()) {
                            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || java.lang.reflect.Modifier.isTransient(field.getModifiers())) continue;
                            Object oldValue = field.get(oldConfig);
                            if (oldValue != null) {
                                field.set(newConfigInstance, oldValue);
                            }
                        }
                        // 4. Guardar el archivo fusionado y devolverlo.
                        saveConfigFile(file, newConfigInstance, configName);
                        return newConfigInstance;
                    } else {
                        // La versión es actual, simplemente cargamos el objeto.
                        return GSON.fromJson(jsonObject, configClass);
                    }
                } catch (Exception e) {
                    throw new ConfigReadException("Failed to migrate " + configName + ". A reflection error occurred.", e);
                }
            } catch (JsonSyntaxException e) {
                throw new ConfigReadException("Failed to parse " + configName + ". Please check for syntax errors.", e);
            } catch (IOException e) {
                throw new ConfigReadException("Failed to read " + configName + ".", e);
            }
        } else {
            PokeNotifier.LOGGER.info("No Poke Notifier " + configName + " file found, creating a new one.");
            try {
                T newConfig = configClass.getDeclaredConstructor().newInstance();
                saveConfigFile(file, newConfig, configName);
                return newConfig;
            } catch (Exception e) {
                throw new ConfigReadException("Failed to create default " + configName + ".", e);
            }
        }
    }

    private static <T> void saveConfigFile(File file, T configObject, String configName) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(configObject, writer);
        } catch (IOException e) {
            PokeNotifier.LOGGER.error("Failed to save Poke Notifier " + configName + ".", e);
        }
    }

    private static void loadConfigClient() throws ConfigReadException {
        configClient = loadConfigFile(CONFIG_CLIENT_FILE, ConfigClient.class, "config-client.json");
    }

    public static void saveClientConfigToFile() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT && configClient != null) {
            saveConfigFile(CONFIG_CLIENT_FILE, configClient, "config-client.json");
        }
    }

    private static void loadConfigServer() throws ConfigReadException {
        configServer = loadConfigFile(CONFIG_SERVER_FILE, ConfigServer.class, "config-server.json");
    }

    public static void saveServerConfigToFile() {
        // Only save on a dedicated server or if the server config has been initialized (e.g., in single-player).
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER || configServer != null) {
            saveConfigFile(CONFIG_SERVER_FILE, configServer, "config-server.json");
        }
    }

    private static void loadCatchemallRewardsConfig() throws ConfigReadException {
        catchemallRewardsConfig = loadConfigFile(CATCHEMALL_REWARDS_FILE, CatchemallRewardsConfig.class, "catchemall_rewards.json");
    }

    private static void saveCatchemallRewardsConfig() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            saveConfigFile(CATCHEMALL_REWARDS_FILE, catchemallRewardsConfig, "catchemall_rewards.json");
        }
    }

    private static void loadBountyRewardsConfig() throws ConfigReadException {
        bountyRewardsConfig = loadConfigFile(BOUNTY_REWARDS_FILE, BountyRewardsConfig.class, "events/bounty_rewards.json");
    }

    private static void saveBountyRewardsConfig() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            if (bountyRewardsConfig == null) bountyRewardsConfig = new BountyRewardsConfig();
            saveConfigFile(BOUNTY_REWARDS_FILE, bountyRewardsConfig, "events/bounty_rewards.json");
        }
    }


    public static ConfigPokemon getPokemonConfig() {
        if (configPokemon == null) {
            // The Pokémon lists are now hardcoded, so we just create a new instance.
            configPokemon = new ConfigPokemon();
        }
        return configPokemon;
    }

    public static ConfigClient getClientConfig() {
        if (configClient == null) {
            try {
                loadConfigClient();
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Initial config-client.json load failed. Using temporary default config.", e);
                configClient = new ConfigClient();
            }
        }
        return configClient;
    }

    public static ConfigServer getServerConfig() {
        if (configServer == null) {
            try {                
                loadConfigServer();
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Initial config-server.json load failed. Using temporary default config.", e);
                configServer = new ConfigServer();
            }
        }
        return configServer;
    }

    public static CatchemallRewardsConfig getCatchemallRewardsConfig() {
        if (catchemallRewardsConfig == null) {
            try {
                loadCatchemallRewardsConfig();
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Initial catchemall_rewards.json load failed. Using temporary default config.", e);
                catchemallRewardsConfig = new CatchemallRewardsConfig();
            }
        }
        return catchemallRewardsConfig;
    }

    public static BountyRewardsConfig getBountyRewardsConfig() {
        if (bountyRewardsConfig == null) {
            try {
                loadBountyRewardsConfig();
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Initial bounty_rewards.json load failed. Using temporary default config.", e);
                bountyRewardsConfig = new BountyRewardsConfig();
            }
        }
        return bountyRewardsConfig;
    }

    public static CustomListConfig getPlayerConfig(UUID playerUuid) {
        return playerConfigs.computeIfAbsent(playerUuid, uuid -> {
            File playerFile = new File(PLAYER_DATA_DIR, uuid.toString() + ".json");
            try {
                return loadConfigFile(playerFile, CustomListConfig.class, uuid.toString() + ".json");
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Could not load custom list for player " + uuid + ". Creating a new one.", e);
                return new CustomListConfig();
            }
        });
    }

    public static void savePlayerConfig(UUID playerUuid, CustomListConfig config) {
        File playerFile = new File(PLAYER_DATA_DIR, playerUuid.toString() + ".json");
        saveConfigFile(playerFile, config, playerUuid.toString() + ".json");
        playerConfigs.put(playerUuid, config); // Update cache
    }

    public static PlayerCatchProgress getPlayerCatchProgress(UUID playerUuid) {
        return playerCatchProgress.computeIfAbsent(playerUuid, uuid -> {
            try {
                File progressFile = new File(CATCH_PROGRESS_DIR, uuid.toString() + ".json");
                if (!progressFile.exists()) {
                    return new PlayerCatchProgress();
                }

                // Load the raw map to check for the "data" field, which indicates the new, encrypted format.
                Map<String, Object> rawData;
                try (FileReader reader = new FileReader(progressFile)) {
                    rawData = GSON.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
                }

                if (rawData.containsKey("data")) {
                    // New format: Decrypt the data.
                    String encryptedData = (String) rawData.get("data");
                    String decryptedJson = DataSecurityUtil.decrypt(encryptedData);

                    if (decryptedJson == null) {
                        PokeNotifier.LOGGER.warn("Could not decrypt progress file for player {}. It might be tampered with or from an incompatible version. Ignoring.", uuid);
                        return new PlayerCatchProgress(); // Return fresh progress for this session.
                    }
                    return GSON.fromJson(decryptedJson, PlayerCatchProgress.class);
                } else {
                    // Old format: Trust the data, migrate it, and save it back.
                    PokeNotifier.LOGGER.info("Migrating old progress file to new secure format for player {}.", uuid);
                    PlayerCatchProgress progress = GSON.fromJson(GSON.toJson(rawData), PlayerCatchProgress.class);
                    savePlayerCatchProgress(uuid, progress); // This will save it in the new, encrypted format.
                    return progress;
                }
            } catch (Exception e) {
                PokeNotifier.LOGGER.error("Could not load or migrate catch progress for player " + uuid + ". Creating a new one.", e);
                return new PlayerCatchProgress();
            }
        });
    }

    public static void savePlayerCatchProgress(UUID playerUuid, PlayerCatchProgress progress) {
        // Encrypt the progress data before saving.
        String jsonProgress = GSON.toJson(progress);
        String encryptedData = DataSecurityUtil.encrypt(jsonProgress);

        // We save it inside a map to have a consistent JSON structure: {"data": "..."}
        Map<String, String> dataToSave = Map.of("data", encryptedData);

        File progressFile = new File(CATCH_PROGRESS_DIR, playerUuid.toString() + ".json");
        saveConfigFile(progressFile, dataToSave, "catch progress for " + playerUuid);
        playerCatchProgress.put(playerUuid, progress); // Update cache
    }

    /**
     * Removes a player's progress from the cache, forcing a reload from the file on the next request.
     * @param playerUuid The player's UUID.
     */
    public static void forceReloadPlayerCatchProgress(UUID playerUuid) {
        playerCatchProgress.remove(playerUuid);
    }

    /**
     * Loads generation data (region and Pokémon list) from the mod's resources.
     * Results are cached for performance.
     * @param genName The name of the generation file (e.g., "gen1").
     * @return A GenerationData object, or null if not found.
     */
    public static GenerationData getGenerationData(String genName) {
        return generationDataCache.computeIfAbsent(genName, name -> {
            String path = "/data/" + PokeNotifier.MOD_ID + "/generations/" + name + ".json";
            try (InputStream is = ConfigManager.class.getResourceAsStream(path)) {
                if (is == null) {
                    PokeNotifier.LOGGER.warn("Generation file not found: {}", path);
                    return null;
                }
                try (InputStreamReader reader = new InputStreamReader(is)) {
                    return GSON.fromJson(reader, GenerationData.class);
                }
            } catch (IOException | JsonSyntaxException e) {
                PokeNotifier.LOGGER.error("Failed to load or parse generation file: {}", path, e);
                return null;
            }
        });
    }
}