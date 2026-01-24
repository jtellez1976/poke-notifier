package com.zehro_mc.pokenotifier.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNotifierData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerData> PLAYER_DATA = new HashMap<>();
    private static File dataFile;

    public static class PlayerData {
        public boolean receivedStarterNotifier = false;
        public long firstJoinDate = 0;
    }

    public static void initialize(File serverDir) {
        // Use Fabric's config directory
        File configDir = FabricLoader.getInstance().getConfigDir().resolve("poke-notifier/player_data").toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        dataFile = new File(configDir, "notifier-devices.json");
        load();
    }

    public static boolean hasReceivedNotifier(UUID playerUuid) {
        return PLAYER_DATA.getOrDefault(playerUuid, new PlayerData()).receivedStarterNotifier;
    }

    public static void markAsReceived(UUID playerUuid) {
        PlayerData data = PLAYER_DATA.computeIfAbsent(playerUuid, k -> new PlayerData());
        data.receivedStarterNotifier = true;
        if (data.firstJoinDate == 0) {
            data.firstJoinDate = System.currentTimeMillis();
        }
        save();
    }

    private static void load() {
        if (!dataFile.exists()) {
            PokeNotifier.LOGGER.info("Player notifier data file doesn't exist, will be created on first save");
            return;
        }
        try (FileReader reader = new FileReader(dataFile)) {
            Map<String, PlayerData> loaded = GSON.fromJson(reader, 
                new com.google.gson.reflect.TypeToken<Map<String, PlayerData>>(){}.getType());
            if (loaded != null) {
                loaded.forEach((key, value) -> PLAYER_DATA.put(UUID.fromString(key), value));
                PokeNotifier.LOGGER.info("Loaded {} player notifier records", PLAYER_DATA.size());
            }
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to load player notifier data", e);
        }
    }

    private static void save() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(dataFile)) {
                Map<String, PlayerData> toSave = new HashMap<>();
                PLAYER_DATA.forEach((uuid, data) -> toSave.put(uuid.toString(), data));
                GSON.toJson(toSave, writer);
            }
        } catch (Exception e) {
            PokeNotifier.LOGGER.error("Failed to save player notifier data", e);
        }
    }
}
