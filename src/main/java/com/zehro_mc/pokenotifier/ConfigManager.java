package com.zehro_mc.pokenotifier;

import com.google.gson.Gson;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(PokeNotifier.MOD_ID).toFile();
    private static final File PLAYER_DATA_DIR = new File(CONFIG_DIR, "player_data");

    private static final File CONFIG_POKEMON_FILE = new File(CONFIG_DIR, "config-pokemon.json");
    private static final File CONFIG_CLIENT_FILE = new File(CONFIG_DIR, "config-client.json");
    private static final File CONFIG_SERVER_FILE = new File(CONFIG_DIR, "config-server.json"); // NUEVO
    private static final File CATCHEMALL_MODE_FILE = new File(CONFIG_DIR, "catchemall-mode.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigPokemon configPokemon;
    private static ConfigClient configClient;
    private static ConfigServer configServer; // NUEVO
    private static CatchemallModeConfig catchemallModeConfig;

    // Cache para las listas personalizadas de los jugadores para evitar leer el archivo constantemente
    private static final Map<UUID, CustomListConfig> playerConfigs = new ConcurrentHashMap<>();

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

        // Obtenemos el tipo de entorno (CLIENT o SERVER)
        EnvType env = FabricLoader.getInstance().getEnvironmentType();

        // Configuraciones que podrían ser necesarias en ambos lados (aunque su uso principal sea del servidor)
        loadCatchemallModeConfig();

        // Cargamos los archivos de configuración basados en el entorno
        if (env == EnvType.CLIENT) {
            // Un cliente solo necesita su propia configuración.
            loadConfigClient();
        } else { // EnvType.SERVER
            // Un servidor dedicado necesita las listas de Pokémon y su propia configuración.
            loadConfigPokemon();
            loadConfigServer();
        }
    }

    public static void saveConfig() {
        saveConfigPokemon();
        saveClientConfigToFile();
        saveServerConfigToFile();
        saveCatchemallModeConfig();
    }

    public static void resetToDefault() {
        EnvType env = FabricLoader.getInstance().getEnvironmentType();

        configPokemon = new ConfigPokemon();
        catchemallModeConfig = new CatchemallModeConfig();

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
        // Solo guardar si no estamos en un cliente puro.
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            saveConfigFile(CONFIG_POKEMON_FILE, configPokemon, "config-pokemon.json");
        }
    }

    private static void loadConfigClient() throws ConfigReadException {
        configClient = loadConfigFile(CONFIG_CLIENT_FILE, ConfigClient.class, "config-client.json");
    }

    public static void saveClientConfigToFile() {
        // Solo guardar si estamos en un entorno de cliente
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT && configClient != null) {
            saveConfigFile(CONFIG_CLIENT_FILE, configClient, "config-client.json");
        }
    }

    private static void loadConfigServer() throws ConfigReadException {
        configServer = loadConfigFile(CONFIG_SERVER_FILE, ConfigServer.class, "config-server.json");
    }

    public static void saveServerConfigToFile() {
        // Solo guardar si no estamos en un cliente puro.
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            saveConfigFile(CONFIG_SERVER_FILE, configServer, "config-server.json");
        }
    }

    private static void loadCatchemallModeConfig() throws ConfigReadException {
        catchemallModeConfig = loadConfigFile(CATCHEMALL_MODE_FILE, CatchemallModeConfig.class, "catchemall-mode.json");
    }

    private static void saveCatchemallModeConfig() {
        if (catchemallModeConfig != null) saveConfigFile(CATCHEMALL_MODE_FILE, catchemallModeConfig, "catchemall-mode.json");
    }

    public static ConfigPokemon getPokemonConfig() {
        // El get ahora es más simple, ya que la carga inicial se encarga de la lógica.
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

    public static CatchemallModeConfig getCatchemallModeConfig() {
        if (catchemallModeConfig == null) {
            try {
                loadCatchemallModeConfig();
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Initial catchemall-mode.json load failed. Using temporary default config.", e);
                catchemallModeConfig = new CatchemallModeConfig();
            }
        }
        return catchemallModeConfig;
    }

    public static CustomListConfig getPlayerConfig(UUID playerUuid) {
        // Devuelve desde el caché si está disponible
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
        playerConfigs.put(playerUuid, config); // Actualiza el caché
    }
}