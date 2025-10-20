/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.data;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.model.CustomListConfig;
import com.zehro_mc.pokenotifier.networking.GuiResponsePayload;
import com.zehro_mc.pokenotifier.util.PokeNotifierServerUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles player-specific data operations including custom hunt lists and progress management.
 */
public class PlayerDataHandler {
    
    /**
     * Adds a Pokémon to a player's custom tracking list.
     * @param player The target player
     * @param pokemonName The Pokémon name to add
     */
    public static void addToCustomList(ServerPlayerEntity player, String pokemonName) {
        CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());
        
        if (playerConfig.tracked_pokemon.add(pokemonName)) {
            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Added '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' to your custom tracking list.").formatted(Formatting.GREEN)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);
        } else {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Pokémon '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' is already on your list.").formatted(Formatting.YELLOW)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }

    /**
     * Removes a Pokémon from a player's custom tracking list.
     * @param player The target player
     * @param pokemonName The Pokémon name to remove
     */
    public static void removeFromCustomList(ServerPlayerEntity player, String pokemonName) {
        CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());
        
        if (playerConfig.tracked_pokemon.remove(pokemonName)) {
            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);
            sendCustomHuntList(player);
        } else {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Pokémon '").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("' was not on your list.").formatted(Formatting.YELLOW)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }

    /**
     * Clears a player's entire custom tracking list.
     * @param player The target player
     */
    public static void clearCustomList(ServerPlayerEntity player) {
        CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());
        
        if (!playerConfig.tracked_pokemon.isEmpty()) {
            playerConfig.tracked_pokemon.clear();
            ConfigManager.savePlayerConfig(player.getUuid(), playerConfig);
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Your custom tracking list has been cleared.").formatted(Formatting.GREEN)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
            PokeNotifierServerUtils.sendCatchProgressUpdate(player);
        } else {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Your custom tracking list was already empty.").formatted(Formatting.YELLOW)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }

    /**
     * Sends the player's custom hunt list to them.
     * @param player The player to send the list to
     */
    public static void sendCustomHuntList(ServerPlayerEntity player) {
        CustomListConfig playerConfig = ConfigManager.getPlayerConfig(player.getUuid());
        if (playerConfig.tracked_pokemon.isEmpty()) {
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Your custom tracking list is empty.").formatted(Formatting.YELLOW)));
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        } else {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("Your custom tracking list:").formatted(Formatting.YELLOW));
            playerConfig.tracked_pokemon.stream().sorted().forEach(name -> {
                Text pokemonText = Text.literal("• " + name + " [X]").formatted(Formatting.GOLD)
                        .styled(style -> style.withClickEvent(new net.minecraft.text.ClickEvent(
                                net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND, 
                                "remove:" + name)));
                lines.add(pokemonText);
            });
            ServerPlayNetworking.send(player, new GuiResponsePayload(lines));
        }
    }
}