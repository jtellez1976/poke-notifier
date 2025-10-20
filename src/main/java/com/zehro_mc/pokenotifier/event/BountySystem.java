/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.event;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigPokemon;
import com.zehro_mc.pokenotifier.ConfigServer;
import com.zehro_mc.pokenotifier.networking.ModeStatusPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages the automatic bounty system for rare Pokémon.
 */
public class BountySystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(BountySystem.class);
    private static final Random BOUNTY_RANDOM = new Random();
    
    private int bountyTickCounter = 0;
    private int bountyReminderTickCounter = 0;
    private long bountyStartTime = 0L;
    
    /**
     * Ticks the bounty system, handling timing and automatic bounty creation.
     * @param server The minecraft server instance
     */
    public void tick(MinecraftServer server) {
        ConfigServer config = ConfigManager.getServerConfig();
        if (!config.bounty_system_enabled) {
            return;
        }

        bountyTickCounter++;

        // Handle bounty expiration and reminders
        String activeBounty = getActiveBounty();
        if (activeBounty != null && bountyStartTime > 0) {
            long elapsedTime = System.currentTimeMillis() - bountyStartTime;
            if (elapsedTime >= (long)config.bounty_duration_minutes * 60 * 1000) {
                LOGGER.info("[Bounty System] Bounty for {} has expired.", activeBounty);
                server.getPlayerManager().broadcast(Text.literal("The bounty for ").append(Text.literal(activeBounty).formatted(Formatting.GOLD)).append(" has expired! The Pokémon got away...").formatted(Formatting.YELLOW), false);
                clearActiveBounty(false);
                return;
            }

            // Periodic reminder logic
            if (config.bounty_reminder_interval_minutes > 0) {
                bountyReminderTickCounter++;
                if (bountyReminderTickCounter >= config.bounty_reminder_interval_minutes * 60 * 20) {
                    bountyReminderTickCounter = 0;
                    long remainingMinutes = config.bounty_duration_minutes - (elapsedTime / (60 * 1000));
                    Text reminder = Text.literal("Reminder: The bounty for ").formatted(Formatting.YELLOW)
                            .append(Text.literal(activeBounty).formatted(Formatting.GOLD))
                            .append(Text.literal(" is still active! Time remaining: ~" + remainingMinutes + " minutes.").formatted(Formatting.YELLOW));
                    server.getPlayerManager().broadcast(reminder, false);
                }
            }
        }

        // Check for new bounty creation
        if (bountyTickCounter >= config.bounty_check_interval_seconds * 20) {
            bountyTickCounter = 0;

            if (activeBounty == null) {
                if (BOUNTY_RANDOM.nextInt(100) < config.bounty_start_chance_percent) {
                    startNewBounty(server);
                }
            }
        }
    }
    
    /**
     * Gets the currently active bounty.
     * @return The active bounty Pokémon name, or null if none
     */
    public String getActiveBounty() {
        return ConfigManager.getServerConfig().active_bounty;
    }
    
    /**
     * Clears the active bounty.
     * @param announce Whether to announce the bounty completion
     */
    public void clearActiveBounty(boolean announce) {
        String currentBounty = getActiveBounty();
        if (currentBounty != null && announce) {
            Text message = Text.literal("The bounty for ").formatted(Formatting.YELLOW)
                    .append(Text.literal(currentBounty).formatted(Formatting.GOLD))
                    .append(Text.literal(" has been claimed!").formatted(Formatting.YELLOW));
            // Note: server broadcast would need to be handled by caller
        }
        ConfigManager.getServerConfig().active_bounty = null;
        ConfigManager.saveServerConfigToFile();
    }
    
    /**
     * Starts a new bounty with a random rare Pokémon.
     * @param server The minecraft server instance
     */
    private void startNewBounty(MinecraftServer server) {
        ConfigPokemon pokemonConfig = ConfigManager.getPokemonConfig();
        List<String> bountyPool = new ArrayList<>();
        bountyPool.addAll(pokemonConfig.RARE);
        bountyPool.addAll(pokemonConfig.ULTRA_RARE);

        if (bountyPool.isEmpty()) {
            LOGGER.warn("[Bounty System] No Pokémon available in RARE or ULTRA_RARE lists to create a bounty.");
            return;
        }

        String newBounty = bountyPool.get(BOUNTY_RANDOM.nextInt(bountyPool.size()));
        ConfigManager.getServerConfig().active_bounty = newBounty;
        ConfigManager.saveServerConfigToFile();
        bountyReminderTickCounter = 0;
        bountyStartTime = System.currentTimeMillis();

        String capitalizedBounty = newBounty.substring(0, 1).toUpperCase() + newBounty.substring(1);
        Text message = Text.literal("🎯 New Bounty Available! ").formatted(Formatting.GREEN)
                .append(Text.literal("The first trainer to capture a ").formatted(Formatting.YELLOW))
                .append(Text.literal(capitalizedBounty).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" will receive a special reward!").formatted(Formatting.YELLOW));

        ModeStatusPayload payload = new ModeStatusPayload("New Bounty!", true);
        server.getPlayerManager().broadcast(message, false);
        server.getPlayerManager().getPlayerList().forEach(player -> {
            player.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE, SoundCategory.NEUTRAL, 1.0F, 1.2F);
            ServerPlayNetworking.send(player, payload);
        });

        LOGGER.info("[Bounty System] New bounty started for: {}", newBounty);
    }
}