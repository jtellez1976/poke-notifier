package com.zehro_mc.pokenotifier;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import com.zehro_mc.pokenotifier.model.CatchemallRewardsConfig;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
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
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(PokeNotifier.MOD_ID).toFile();
    private static final File PLAYER_DATA_DIR = new File(CONFIG_DIR, "player_data");
    public static final File CATCH_PROGRESS_DIR = new File(CONFIG_DIR, "catch_progress"); // NUEVO Y PÚBLICO

    private static final File CONFIG_POKEMON_FILE = new File(CONFIG_DIR, "config-pokemon.json");
    private static final File CONFIG_CLIENT_FILE = new File(CONFIG_DIR, "config-client.json");
    private static final File CONFIG_SERVER_FILE = new File(CONFIG_DIR, "config-server.json"); // NUEVO
    private static final File CATCHEMALL_REWARDS_FILE = new File(CONFIG_DIR, "catchemall_rewards.json"); // NUEVO
    private static final File CATCHEMALL_MODE_FILE = new File(CONFIG_DIR, "catchemall-mode.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigPokemon configPokemon;
    private static ConfigClient configClient;
    private static CatchemallRewardsConfig catchemallRewardsConfig; // NUEVO
    private static ConfigServer configServer; // NUEVO
    private static CatchemallModeConfig catchemallModeConfig;

    // Cache para las listas personalizadas de los jugadores para evitar leer el archivo constantemente
    private static final Map<UUID, CustomListConfig> playerConfigs = new ConcurrentHashMap<>();
    // Cache para el progreso "Catch 'em All" de los jugadores
    private static final Map<UUID, PlayerCatchProgress> playerCatchProgress = new ConcurrentHashMap<>(); // NUEVO

    // Cache para las listas de generaciones cargadas desde los recursos del mod
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
        if (!CATCH_PROGRESS_DIR.exists()) { // NUEVO
            CATCH_PROGRESS_DIR.mkdirs();
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
            loadCatchemallRewardsConfig(); // NUEVO
        }
    }

    public static void saveConfig() {
        saveConfigPokemon();
        saveClientConfigToFile();
        saveServerConfigToFile();
        saveCatchemallRewardsConfig(); // NUEVO
        saveCatchemallModeConfig();
    }

    public static void resetToDefault() {
        EnvType env = FabricLoader.getInstance().getEnvironmentType();

        configPokemon = new ConfigPokemon();
        catchemallRewardsConfig = new CatchemallRewardsConfig(); // NUEVO
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
        // Solo guardar si estamos en un servidor dedicado, o si la configuración del servidor
        // ha sido inicializada (lo que ocurre en single-player).
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER || configServer != null) {
            saveConfigFile(CONFIG_SERVER_FILE, configServer, "config-server.json");
        }
    }

    private static void loadCatchemallRewardsConfig() throws ConfigReadException { // NUEVO
        catchemallRewardsConfig = loadConfigFile(CATCHEMALL_REWARDS_FILE, CatchemallRewardsConfig.class, "catchemall_rewards.json");
    }

    private static void saveCatchemallRewardsConfig() { // NUEVO
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            saveConfigFile(CATCHEMALL_REWARDS_FILE, catchemallRewardsConfig, "catchemall_rewards.json");
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

    public static CatchemallRewardsConfig getCatchemallRewardsConfig() { // NUEVO
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

    // --- NUEVOS MÉTODOS PARA CATCH 'EM ALL ---

    public static PlayerCatchProgress getPlayerCatchProgress(UUID playerUuid) {
        return playerCatchProgress.computeIfAbsent(playerUuid, uuid -> {
            File progressFile = new File(CATCH_PROGRESS_DIR, uuid.toString() + ".json");
            try {
                return loadConfigFile(progressFile, PlayerCatchProgress.class, "catch progress for " + uuid);
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Could not load catch progress for player " + uuid + ". Creating a new one.", e);
                return new PlayerCatchProgress();
            }
        });
    }

    public static void savePlayerCatchProgress(UUID playerUuid, PlayerCatchProgress progress) {
        File progressFile = new File(CATCH_PROGRESS_DIR, playerUuid.toString() + ".json");
        saveConfigFile(progressFile, progress, "catch progress for " + playerUuid);
        playerCatchProgress.put(playerUuid, progress); // Actualiza el caché
    }

    /**
     * Elimina el progreso de un jugador del caché, forzando a que se recargue desde el archivo en la próxima petición.
     * @param playerUuid El UUID del jugador.
     */
    public static void forceReloadPlayerCatchProgress(UUID playerUuid) {
        playerCatchProgress.remove(playerUuid);
    }

    /**
     * Carga los datos de una generación (región y lista de Pokémon) desde los recursos del mod.
     * Los resultados se guardan en caché para mejorar el rendimiento.
     * @param genName El nombre del archivo de generación (ej: "gen1")
     * @return Un objeto GenerationData o null si no se encuentra.
     */
    public static GenerationData getGenerationData(String genName) {
        return generationDataCache.computeIfAbsent(genName, name -> {
            String path = "/data/" + PokeNotifier.MOD_ID + "/generations/" + name + ".json";
            try (InputStream is = ConfigManager.class.getResourceAsStream(path)) {
                if (is == null) {
                    PokeNotifier.LOGGER.warn("Generation file not found: {}", path);
                    return null; // Devuelve null si el archivo no existe
                }
                try (InputStreamReader reader = new InputStreamReader(is)) {
                    return GSON.fromJson(reader, GenerationData.class);
                }
            } catch (IOException | JsonSyntaxException e) {
                PokeNotifier.LOGGER.error("Failed to load or parse generation file: {}", path, e);
                return null; // Devuelve null si hay un error de lectura/parseo
            }
        });
    }
}