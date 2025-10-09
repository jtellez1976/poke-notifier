package com.zehro_mc.pokenotifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "poke-notifier.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config config;

    public static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, Config.class);
                if (config == null) {
                    // Handle case where file is empty or invalid
                    config = new Config();
                }
                PokeNotifier.LOGGER.info("Poke Notifier configuration loaded.");
            } catch (IOException e) {
                PokeNotifier.LOGGER.error("Failed to load Poke Notifier configuration, using defaults.", e);
                config = new Config();
            }
        } else {
            config = new Config();
            PokeNotifier.LOGGER.info("No Poke Notifier configuration file found, creating a new one.");
        }
        saveConfig(); // Save to create the file if it doesn't exist or to add new fields
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            PokeNotifier.LOGGER.error("Failed to save Poke Notifier configuration.", e);
        }
    }

    public static Config getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
}