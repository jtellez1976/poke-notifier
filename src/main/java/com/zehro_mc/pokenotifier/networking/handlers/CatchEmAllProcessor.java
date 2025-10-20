/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking.handlers;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload;
import com.zehro_mc.pokenotifier.networking.GuiResponsePayload;
import com.zehro_mc.pokenotifier.networking.ModeStatusPayload;
import com.zehro_mc.pokenotifier.util.PokeNotifierServerUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes Catch 'em All update payloads from clients.
 */
public class CatchEmAllProcessor {
    
    /**
     * Processes a Catch 'em All update.
     * @param player The player who sent the update
     * @param action The action to perform
     * @param genName The generation name
     */
    public static void processUpdate(ServerPlayerEntity player, CatchemallUpdatePayload.Action action, String genName) {
        var progress = ConfigManager.getPlayerCatchProgress(player.getUuid());

        switch (action) {
            case ENABLE -> enableGeneration(player, progress, genName);
            case DISABLE -> disableGeneration(player, progress, genName);
            case LIST -> listActiveGenerations(player, progress);
        }
    }
    
    private static void enableGeneration(ServerPlayerEntity player, com.zehro_mc.pokenotifier.model.PlayerCatchProgress progress, String genName) {
        GenerationData genData = ConfigManager.getGenerationData(genName);
        if (genData == null) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Error: Generation '" + genName + "' not found.").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            return;
        }
        
        // Allow only one active generation at a time
        if (progress.active_generations.contains(genName)) {
            String regionName = formatRegionName(genData.region);
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Already tracking " + regionName + ".").formatted(Formatting.YELLOW)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            return;
        }

        if (!progress.active_generations.isEmpty()) {
            String oldGen = progress.active_generations.iterator().next();
            player.sendMessage(Text.literal("Stopped tracking " + formatGenName(oldGen) + ".").formatted(Formatting.YELLOW));
        }
        
        progress.active_generations.clear();
        progress.active_generations.add(genName);
        ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
        String regionName = formatRegionName(genData.region);

        ServerPlayNetworking.send(player, new ModeStatusPayload("Tracking: " + regionName, true));
        List<Text> lines = new ArrayList<>(List.of(Text.literal("Now tracking: " + regionName).formatted(Formatting.GREEN)));
        ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        PokeNotifierServerUtils.sendCatchProgressUpdate(player);
    }
    
    private static void disableGeneration(ServerPlayerEntity player, com.zehro_mc.pokenotifier.model.PlayerCatchProgress progress, String genName) {
        if (progress.active_generations.remove(genName)) {
            ConfigManager.savePlayerCatchProgress(player.getUuid(), progress);
            List<Text> disableLines = new ArrayList<>(List.of(Text.literal("Tracking disabled for " + formatGenName(genName)).formatted(Formatting.YELLOW)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(disableLines));
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);
        } else {
            List<Text> notTrackingLines = new ArrayList<>(List.of(Text.literal("You were not tracking " + formatGenName(genName) + ".").formatted(Formatting.RED)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(notTrackingLines));
        }
    }
    
    private static void listActiveGenerations(ServerPlayerEntity player, com.zehro_mc.pokenotifier.model.PlayerCatchProgress progress) {
        List<Text> catchemallLines;
        if (progress.active_generations.isEmpty()) {
            catchemallLines = new ArrayList<>(List.of(Text.literal("You are not tracking any generation for Catch 'em All mode.").formatted(Formatting.YELLOW)));
        } else {
            catchemallLines = new ArrayList<>();
            catchemallLines.add(Text.literal("You are currently tracking the following generations:").formatted(Formatting.YELLOW));
            progress.active_generations.forEach(gen -> {
                GenerationData data = ConfigManager.getGenerationData(gen);
                String regionNameForList = data != null ? formatRegionName(data.region) : "Unknown";
                catchemallLines.add(Text.literal("- " + formatGenName(gen) + " (" + regionNameForList + ")").formatted(Formatting.GOLD));
            });
        }
        ServerPlayNetworking.send(player, new GuiResponsePayload(catchemallLines));
    }
    
    private static String formatGenName(String genName) {
        if (genName == null || !genName.toLowerCase().startsWith("gen")) return genName;
        return "Gen" + genName.substring(3);
    }

    private static String formatRegionName(String regionName) {
        if (regionName == null || regionName.isEmpty()) return "Unknown";
        return regionName.substring(0, 1).toUpperCase() + regionName.substring(1);
    }
}