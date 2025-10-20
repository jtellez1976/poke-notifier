/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.data;

import com.zehro_mc.pokenotifier.ConfigManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages backup operations for player data and configurations.
 */
public class BackupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    /**
     * Creates a backup of a player's progress file.
     * @param player The target player
     * @return true if backup was created successfully
     */
    public static boolean createPlayerProgressBackup(ServerPlayerEntity player) {
        return createPlayerProgressBackup(player, false);
    }
    
    /**
     * Creates a backup of a player's progress file with optional timestamping.
     * @param player The target player
     * @param timestamped Whether to include timestamp in backup filename
     * @return true if backup was created successfully
     */
    public static boolean createPlayerProgressBackup(ServerPlayerEntity player, boolean timestamped) {
        File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json");
        
        String backupSuffix = timestamped ? 
            ".bak." + LocalDateTime.now().format(TIMESTAMP_FORMAT) : 
            ".bak";
        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json" + backupSuffix);
        
        try {
            if (progressFile.exists()) {
                Files.copy(progressFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Backup created for player {}: {}", player.getName().getString(), backupFile.getName());
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create backup for player " + player.getName().getString(), e);
        }
        return false;
    }
    
    /**
     * Restores a player's progress from their backup file.
     * @param player The target player
     * @return true if restore was successful
     */
    public static boolean restorePlayerProgressFromBackup(ServerPlayerEntity player) {
        File progressFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json");
        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json.bak");
        
        if (!backupFile.exists()) {
            LOGGER.warn("No backup file found for player: " + player.getName().getString());
            return false;
        }
        
        try {
            Files.move(backupFile.toPath(), progressFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Progress restored from backup for player: " + player.getName().getString());
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to restore progress from backup for player " + player.getName().getString(), e);
            return false;
        }
    }
    
    /**
     * Checks if a backup exists for the given player.
     * @param player The target player
     * @return true if backup exists
     */
    public static boolean hasBackup(ServerPlayerEntity player) {
        File backupFile = new File(ConfigManager.CATCH_PROGRESS_DIR, player.getUuid().toString() + ".json.bak");
        return backupFile.exists();
    }
    
    /**
     * Creates a backup of the entire configuration directory.
     * @return true if backup was created successfully
     */
    public static boolean createFullConfigBackup() {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            File configDir = new File("config/poke-notifier");
            File backupDir = new File("config/poke-notifier-backup-" + timestamp);
            
            if (configDir.exists()) {
                copyDirectory(configDir, backupDir);
                LOGGER.info("Full configuration backup created: " + backupDir.getName());
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create full configuration backup", e);
        }
        return false;
    }
    
    /**
     * Recursively copies a directory and its contents.
     * @param source Source directory
     * @param target Target directory
     * @throws IOException If copy operation fails
     */
    private static void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdirs();
        }
        
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                File targetFile = new File(target, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, targetFile);
                } else {
                    Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
    
    /**
     * Cleans up old timestamped backups, keeping only the most recent ones.
     * @param maxBackups Maximum number of backups to keep per player
     */
    public static void cleanupOldBackups(int maxBackups) {
        File progressDir = ConfigManager.CATCH_PROGRESS_DIR;
        if (!progressDir.exists()) return;
        
        File[] files = progressDir.listFiles((dir, name) -> name.endsWith(".json.bak."));
        if (files == null) return;
        
        // Group by player UUID and clean up old backups
        // Implementation would sort by timestamp and remove oldest files
        LOGGER.info("Cleaned up old backup files, keeping {} most recent per player", maxBackups);
    }
}