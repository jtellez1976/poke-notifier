package com.zehro_mc.pokenotifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    // We use Gson to convert Java objects to JSON and vice versa. We make it "pretty" to be human-readable.
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // The path to the configuration file. FabricLoader gives us the path to the /config folder.
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), PokeNotifier.MOD_ID + ".json");

    // A static instance of our configuration that will be accessible from the entire mod.
    private static Config config;

    public static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            // If the file exists, we read it.
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, Config.class);
                if (config == null) { // Handle case where file is empty or invalid
                    throw new IOException("Config file is empty or malformed.");
                }
                PokeNotifier.LOGGER.info("Poke-Notifier configuration loaded successfully.");
            } catch (Exception e) {
                PokeNotifier.LOGGER.error("Could not read or parse configuration file. Creating a new one with default values.", e);
                config = new Config();
                saveConfig();
            }
        } else {
            // If the file doesn't exist, we create a new one with the default values.
            PokeNotifier.LOGGER.info("Poke-Notifier configuration not found. Creating a default one.");
            config = new Config();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            PokeNotifier.LOGGER.info("Poke-Notifier configuration saved to: " + CONFIG_FILE.getAbsolutePath());
        } catch (IOException e) {
            PokeNotifier.LOGGER.error("Could not save configuration file.", e);
        }
    }

    // A "getter" method so other classes can access the loaded configuration.
    public static Config getConfig() {
        if (config == null) {
            // This is a safeguard. If something tries to access the config before it's loaded, we load it.
            loadConfig();
        }
        return config;
    }
}