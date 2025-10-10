package com.zehro_mc.pokenotifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(PokeNotifier.MOD_ID).toFile();

    private static final File CONFIG_POKEMON_FILE = new File(CONFIG_DIR, "config-pokemon.json");
    private static final File CONFIG_CLIENT_FILE = new File(CONFIG_DIR, "config-client.json");
    private static final File CATCHEMALL_MODE_FILE = new File(CONFIG_DIR, "Catchemall-mode.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigPokemon configPokemon;
    private static ConfigClient configClient;
    private static CatchemallModeConfig catchemallModeConfig;

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
        loadConfigPokemon();
        loadConfigClient();
        loadCatchemallModeConfig();
    }

    public static void saveConfig() {
        saveConfigPokemon();
        saveClientConfigToFile();
        saveCatchemallModeConfig();
    }

    public static void resetToDefault() {
        configPokemon = new ConfigPokemon();
        configClient = new ConfigClient();
        catchemallModeConfig = new CatchemallModeConfig();
        PokeNotifier.LOGGER.info("Generated new default Poke Notifier configurations.");
        saveConfig();
    }

    private static <T> T loadConfigFile(File file, Class<T> configClass, String configName) throws ConfigReadException {
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                T configObject = GSON.fromJson(reader, configClass);
                if (configObject == null) {
                    throw new ConfigReadException("The " + configName + " file is empty or invalid.");
                }
                PokeNotifier.LOGGER.info("Poke Notifier " + configName + " loaded.");
                return configObject;
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

    private static void loadConfigPokemon() throws ConfigReadException {
        configPokemon = loadConfigFile(CONFIG_POKEMON_FILE, ConfigPokemon.class, "config-pokemon.json");
    }

    private static void saveConfigPokemon() {
        if (configPokemon != null) saveConfigFile(CONFIG_POKEMON_FILE, configPokemon, "config-pokemon.json");
    }

    private static void loadConfigClient() throws ConfigReadException {
        configClient = loadConfigFile(CONFIG_CLIENT_FILE, ConfigClient.class, "config-client.json");
    }

    public static void saveClientConfigToFile() {
        if (configClient != null) saveConfigFile(CONFIG_CLIENT_FILE, configClient, "config-client.json");
    }

    private static void loadCatchemallModeConfig() throws ConfigReadException {
        catchemallModeConfig = loadConfigFile(CATCHEMALL_MODE_FILE, CatchemallModeConfig.class, "Catchemall-mode.json");
    }

    private static void saveCatchemallModeConfig() {
        if (catchemallModeConfig != null) saveConfigFile(CATCHEMALL_MODE_FILE, catchemallModeConfig, "Catchemall-mode.json");
    }

    public static ConfigPokemon getPokemonConfig() {
        if (configPokemon == null) {
            try {
                loadConfigPokemon();
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Initial config-pokemon.json load failed. Using temporary default config.", e);
                configPokemon = new ConfigPokemon();
            }
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

    public static CatchemallModeConfig getCatchemallModeConfig() {
        if (catchemallModeConfig == null) {
            try {
                loadCatchemallModeConfig();
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Initial Catchemall-mode.json load failed. Using temporary default config.", e);
                catchemallModeConfig = new CatchemallModeConfig();
            }
        }
        return catchemallModeConfig;
    }
}