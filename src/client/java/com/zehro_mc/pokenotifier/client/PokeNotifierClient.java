/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.block.entity.ModBlockEntities;
import com.zehro_mc.pokenotifier.client.compat.AdvancementPlaquesCompat;
import com.zehro_mc.pokenotifier.client.renderer.TrophyDisplayBlockEntityRenderer;
import com.zehro_mc.pokenotifier.networking.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PokeNotifierClient implements ClientModInitializer {

    public static final Map<String, BlockPos> ACTIVE_WAYPOINTS = new ConcurrentHashMap<>();
    public static final Logger LOGGER = LoggerFactory.getLogger(PokeNotifier.MOD_ID + "-Client");

    // HUD progress variables.
    public static String currentCatchEmAllGeneration = "none";
    public static int catchCaughtCount = 0;
    public static int catchTotalCount = 0;
    public static int customHuntListSize = 0; // NEW: Track the size of the hunt list

    @Override
    public void onInitializeClient() {
        // Load client-specific configurations at the very beginning.
        ConfigManager.loadClientConfig();

        HudRenderCallback.EVENT.register(NotificationHUD::render);
        HudRenderCallback.EVENT.register(CatchEmAllHUD::render);
        HudRenderCallback.EVENT.register(ActivationFeedbackHUD::render);

        // Now that payloads are registered, we can safely register their receivers.
        registerClientPacketReceivers();

        BlockEntityRendererFactories.register(ModBlockEntities.TROPHY_DISPLAY_BLOCK_ENTITY, TrophyDisplayBlockEntityRenderer::new);
        ClientCommandRegistrationCallback.EVENT.register(ClientCommands::register);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ConfigClient config = ConfigManager.getClientConfig();
            if (config.silent_mode_enabled) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Poke Notifier is in Silent Mode. Use '/pnc silent OFF' to re-enable notifications.").formatted(Formatting.YELLOW), false);
                }
            }
        });
    }

    /**
     * Registers receivers for packets sent from the server to the client.
     */
    private void registerClientPacketReceivers() {
        // --- FIX: Open the GUI via a server-sent packet ---
        ClientPlayNetworking.registerGlobalReceiver(OpenGuiPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Pass null as the parent to avoid lifecycle conflicts with the chat screen
                context.client().setScreen(new PokeNotifierCustomScreen(null));});
        });

        // --- NEW: Receive text responses for the GUI ---
        ClientPlayNetworking.registerGlobalReceiver(GuiResponsePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof PokeNotifierCustomScreen screen) {
                    screen.displayResponse(payload.lines());
                }
            });
        });

        // Receive debug mode status from the server.
        ClientPlayNetworking.registerGlobalReceiver(ServerDebugStatusPayload.ID, (payload, context) -> {
            if (payload.debugModeEnabled()) {
                context.client().execute(() -> {
                    context.client().player.sendMessage(Text.literal("Poke Notifier Debug Mode is active on the server.").formatted(Formatting.AQUA), false);
                });
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(WaypointPayload.ID, (payload, context) -> {
            if (!ConfigManager.getClientConfig().searching_enabled) return;

            MinecraftClient client = context.client();
            client.execute(() -> {
                String formattedCategory = formatCategoryName(payload.rarityCategoryName());

                if (ConfigManager.getClientConfig().alert_toast_enabled) {
                    MutableText title;
                    if ("HUNT".equals(payload.rarityCategoryName())) {
                        title = Text.literal("Hunting Target: ").append(Text.literal(payload.name()).formatted(Formatting.GREEN));
                    } else {
                        MutableText pokemonText;
                        if ("SHINY".equals(payload.rarityCategoryName())) {
                            pokemonText = createRainbowText(formattedCategory + " " + payload.name());
                        } else {
                            pokemonText = Text.literal(formattedCategory + " " + payload.name())
                                    .styled(style -> style.withColor(payload.color()));
                        }
                        title = Text.literal("A ").append(pokemonText).append(Text.literal(" has appeared"));
                    }
                    Text description = Text.empty();
                    NotificationHUD.show(title, description, payload.spriteIdentifier());
                }

                if (ConfigManager.getClientConfig().alert_chat_enabled) {
                    MutableText prefix = Text.literal("[").formatted(Formatting.GREEN)
                            .append(Text.literal("Poke Notifier").formatted(Formatting.GOLD))
                            .append(Text.literal("] ").formatted(Formatting.GREEN));

                    MutableText chatMessage = prefix
                            .append(Text.literal("A wild ").formatted(Formatting.YELLOW));

                    if ("HUNT".equals(payload.rarityCategoryName())) {
                        chatMessage.append(Text.literal(payload.name()).formatted(Formatting.GREEN))
                                .append(Text.literal(" (Hunting Target)").formatted(Formatting.DARK_GREEN));
                    } else {
                        if ("SHINY".equals(payload.rarityCategoryName())) {
                            chatMessage.append(createRainbowText(formattedCategory + " " + payload.name()));
                        } else {
                            chatMessage.append(Text.literal(formattedCategory + " " + payload.name())
                                    .styled(style -> style.withColor(payload.color())));
                        }
                    }

                    chatMessage.append(Text.literal(" ["));
                    if ("NEW".equals(payload.status())) {
                        chatMessage.append(Text.literal(payload.status()).formatted(Formatting.GREEN));
                    } else {
                        chatMessage.append(Text.literal(payload.status()).formatted(Formatting.GRAY));
                    }
                    chatMessage.append(Text.literal("]"));

                    chatMessage.append(Text.literal(" (" + payload.level() + ") has appeared at ").formatted(Formatting.YELLOW));
                    chatMessage.append(Text.literal(payload.pos().getX() + ", " + payload.pos().getY() + ", " + payload.pos().getZ()).formatted(Formatting.GREEN));
                    chatMessage.append(Text.literal(" (").formatted(Formatting.YELLOW));
                    chatMessage.append(Text.literal(String.format("%.1f", payload.distance()) + " blocks away").formatted(Formatting.GREEN));
                    chatMessage.append(Text.literal("). Hurry up!!").formatted(Formatting.YELLOW));

                    if (client.player != null) {
                        client.player.sendMessage(chatMessage, false);
                    }
                }

                if (ConfigManager.getClientConfig().alert_sounds_enabled && "NEW".equals(payload.status())) {
                    if (client.player != null) {
                        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 0.5F));
                    }
                }

                ACTIVE_WAYPOINTS.put(payload.uuid(), payload.pos());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(StatusUpdatePayload.ID, (payload, context) -> {
            if (!ConfigManager.getClientConfig().searching_enabled) return;

            MinecraftClient client = context.client();
            client.execute(() -> {
                if (ACTIVE_WAYPOINTS.remove(payload.uuid()) != null) {
                    LOGGER.info("Waypoint removed for: " + payload.name());

                    if (payload.updateType() == StatusUpdatePayload.UpdateType.DESPAWNED) {
                        if (ConfigManager.getClientConfig().alert_chat_enabled && client.player != null) {
                            MutableText prefix = Text.literal("[").formatted(Formatting.YELLOW)
                                    .append(Text.literal("Poke Notifier").formatted(Formatting.GOLD))
                                    .append(Text.literal("] ").formatted(Formatting.YELLOW));

                            MutableText despawnMessage = prefix
                                    .append(Text.literal("The wild " + payload.name() + " has fled...").formatted(Formatting.YELLOW));

                            client.player.sendMessage(despawnMessage, false);
                        }
                    }
                }
            });
        });

        // Receive "Catch 'em All" progress updates.
        ClientPlayNetworking.registerGlobalReceiver(CatchProgressPayload.ID, (payload, context) -> {
            String previousGeneration = currentCatchEmAllGeneration;

            context.client().execute(() -> {
                currentCatchEmAllGeneration = payload.generationName();
                catchCaughtCount = payload.caughtCount();
                catchTotalCount = payload.totalCount();
                customHuntListSize = payload.customHuntListSize();

                // If a generation was just activated, send a reminder message to the player.
                if ("none".equals(previousGeneration) && !"none".equals(currentCatchEmAllGeneration)) {
                    String genName = currentCatchEmAllGeneration.substring(0, 1).toUpperCase() + currentCatchEmAllGeneration.substring(1);
                    context.client().player.sendMessage(Text.literal("Catch 'em All mode is active for: ").append(Text.literal(genName).formatted(Formatting.GOLD)).formatted(Formatting.YELLOW), false);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModeStatusPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ActivationFeedbackHUD.show(Text.literal(payload.message()), payload.isActivation());
            });
        });

        // Receive global Pokédex completion announcements.
        ClientPlayNetworking.registerGlobalReceiver(GlobalAnnouncementPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.player == null) return;

                // --- MEJORA: Creamos un título para el toast y un mensaje para el chat ---
                Text toastTitle = Text.literal(payload.regionName()).formatted(Formatting.GOLD);
                Text chatMessage = Text.literal("Congratulations to ").formatted(Formatting.YELLOW)
                        .append(Text.literal(payload.playerName()).formatted(Formatting.GOLD))
                        .append(Text.literal(" for completing the ").formatted(Formatting.YELLOW))
                        .append(Text.literal(payload.regionName()).formatted(Formatting.GOLD))
                        .append(Text.literal(" Pokédex!").formatted(Formatting.YELLOW));

                // Si el jugador que recibe el paquete es el que completó el logro, muestra el toast.
                if (client.player.getName().getString().equals(payload.playerName())) {
                    AdvancementPlaquesCompat.showPlaque(toastTitle, true);
                }
                client.player.sendMessage(chatMessage, false);
            });
    });

    // Receive rank updates from the server.
    ClientPlayNetworking.registerGlobalReceiver(RankSyncPayload.ID, (payload, context) -> {
        context.client().execute(() -> {
            ClientRankCache.updateRanks(payload.ranks());
        });
        });
    }

    private static String formatCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return "Rare";
        }
        String[] words = categoryName.toLowerCase().split("_");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1);
        }
        return String.join(" ", words);
    }

    /**
     * Creates a text component with a rainbow color effect.
     * @param text The text to be colored.
     * @return A MutableText with the effect applied.
     */
    private static MutableText createRainbowText(String text) {
        MutableText rainbowText = Text.empty();
        Formatting[] rainbowColors = {
                Formatting.RED,
                Formatting.GOLD,
                Formatting.YELLOW,
                Formatting.GREEN,
                Formatting.AQUA,
                Formatting.LIGHT_PURPLE
        };
        for (int i = 0; i < text.length(); i++) {
            // Assign a rainbow color to each character, cycling through the color list.
            rainbowText.append(Text.literal(String.valueOf(text.charAt(i))).formatted(rainbowColors[i % rainbowColors.length]));
        }
        return rainbowText;
    }
}