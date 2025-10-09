package com.zehro_mc.pokenotifier;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("poke-notifier.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config config;

    // Excepción personalizada para un manejo de errores claro en el comando
    public static class ConfigReadException extends Exception {
        public ConfigReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void loadConfig() throws ConfigReadException {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, Config.class);
                if (config == null) {
                    throw new ConfigReadException("The config file is empty or invalid.", null);
                }
                PokeNotifier.LOGGER.info("Poke Notifier configuration loaded.");
            } catch (JsonSyntaxException e) {
                // Este es el error específico para un formato JSON incorrecto (ej: coma faltante)
                throw new ConfigReadException("Failed to parse poke-notifier.json. Please check for syntax errors.", e);
            } catch (IOException e) {
                throw new ConfigReadException("Failed to read poke-notifier.json.", e);
            }
        } else {
            // Si el archivo no existe, crea uno nuevo por defecto.
            PokeNotifier.LOGGER.info("No Poke Notifier configuration file found, creating a new one.");
            resetToDefault();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            PokeNotifier.LOGGER.error("Failed to save Poke Notifier configuration.", e);
        }
    }

    public static void resetToDefault() {
        config = new Config();
        PokeNotifier.LOGGER.info("Generated new default Poke Notifier configuration.");
        saveConfig();
    }

    public static Config getConfig() {
        if (config == null) {
            try {
                loadConfig();
            } catch (ConfigReadException e) {
                PokeNotifier.LOGGER.error("Initial config load failed. Using temporary default config. Please fix poke-notifier.json or reset it.", e);
                config = new Config(); // Usa una configuración por defecto temporal si el archivo está roto en la carga inicial
            }
        }
        return config;
    }
}