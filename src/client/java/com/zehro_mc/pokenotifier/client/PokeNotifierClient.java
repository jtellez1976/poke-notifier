package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.networking.*;
import com.zehro_mc.pokenotifier.networking.ServerDebugStatusPayload;
import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.networking.WaypointPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PokeNotifierClient implements ClientModInitializer {

    public static final Map<String, BlockPos> ACTIVE_WAYPOINTS = new ConcurrentHashMap<>();
    public static final Logger LOGGER = LoggerFactory.getLogger(PokeNotifier.MOD_ID + "-Client");

    // --- Variables para el HUD de Progreso ---
    public static String currentCatchEmAllGeneration = "none";
    public static int catchCaughtCount = 0;
    public static int catchTotalCount = 0;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(NotificationHUD::render);
        HudRenderCallback.EVENT.register(CatchEmAllHUD::render); // Registramos el nuevo HUD
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ClientCommands.register(dispatcher));

        // --- NUEVO: Mensaje al iniciar sesión ---
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ConfigClient config = ConfigManager.getClientConfig();
            if (config.silent_mode_enabled) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Poke Notifier is in Silent Mode. Use '/pnc silent OFF' to re-enable notifications.").formatted(Formatting.YELLOW), false);
                }
            }
        });

        // Recibir el estado del debug mode desde el servidor
        ClientPlayNetworking.registerGlobalReceiver(ServerDebugStatusPayload.ID, (payload, context) -> {
            if (payload.debugModeEnabled()) {
                context.client().execute(() -> {
                    context.client().player.sendMessage(Text.literal("Poke Notifier Debug Mode is active on the server.").formatted(Formatting.AQUA), false);
                });
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(WaypointPayload.ID, (payload, context) -> {
            // --- NUEVO: Comprobación del interruptor principal ---
            if (!ConfigManager.getClientConfig().searching_enabled) return;

            MinecraftClient client = context.client();
            client.execute(() -> {
                String formattedCategory = formatCategoryName(payload.rarityCategoryName());

                // Lógica para mostrar el HUD
                if (ConfigManager.getClientConfig().alert_toast_enabled) {
                    MutableText title;
                    if ("HUNT".equals(payload.rarityCategoryName())) {
                        // Título especial para Hunting Mode
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

                // Lógica para enviar el mensaje al chat (CORREGIDA)
                if (ConfigManager.getClientConfig().alert_chat_enabled) {
                    // 1. Construimos el prefijo
                    MutableText prefix = Text.literal("[").formatted(Formatting.GREEN)
                            .append(Text.literal("Poke Notifier").formatted(Formatting.GOLD))
                            .append(Text.literal("] ").formatted(Formatting.GREEN));

                    // 2. Construimos el mensaje pieza por pieza
                    MutableText chatMessage = prefix
                            .append(Text.literal("A wild ").formatted(Formatting.YELLOW));

                    // 3. Lógica de texto principal
                    if ("HUNT".equals(payload.rarityCategoryName())) {
                        // Mensaje especial para Hunting Mode
                        chatMessage.append(Text.literal(payload.name()).formatted(Formatting.GREEN))
                                .append(Text.literal(" (Hunting Target)").formatted(Formatting.DARK_GREEN));
                    } else {
                        // Lógica normal para otras rarezas
                        if ("SHINY".equals(payload.rarityCategoryName())) {
                            chatMessage.append(createRainbowText(formattedCategory + " " + payload.name()));
                        } else {
                            chatMessage.append(Text.literal(formattedCategory + " " + payload.name())
                                    .styled(style -> style.withColor(payload.color())));
                        }
                    }

                    // 4. Añadimos el estado [NEW] o [CAUGHT]
                    chatMessage.append(Text.literal(" ["));
                    if ("NEW".equals(payload.status())) {
                        chatMessage.append(Text.literal(payload.status()).formatted(Formatting.GREEN));
                    } else {
                        chatMessage.append(Text.literal(payload.status()).formatted(Formatting.GRAY));
                    }
                    chatMessage.append(Text.literal("]"));

                    // 5. Añadimos el resto del texto, coloreando las coordenadas y la distancia
                    chatMessage.append(Text.literal(" (" + payload.level() + ") has appeared at ").formatted(Formatting.YELLOW));
                    chatMessage.append(Text.literal(payload.pos().getX() + ", " + payload.pos().getY() + ", " + payload.pos().getZ()).formatted(Formatting.GREEN));
                    chatMessage.append(Text.literal(" (").formatted(Formatting.YELLOW));
                    chatMessage.append(Text.literal(String.format("%.1f", payload.distance()) + " blocks away").formatted(Formatting.GREEN));
                    chatMessage.append(Text.literal("). Hurry up!!").formatted(Formatting.YELLOW));

                    if (client.player != null) {
                        client.player.sendMessage(chatMessage, false);
                    }
                }

                // Lógica para reproducir el sonido
                // Comprobamos la configuración ANTES de intentar reproducir el sonido.
                if (ConfigManager.getClientConfig().alert_sounds_enabled && "NEW".equals(payload.status())) {
                    if (client.player != null) {
                        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 0.5F));
                    }
                }

                ACTIVE_WAYPOINTS.put(payload.uuid(), payload.pos());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(StatusUpdatePayload.ID, (payload, context) -> {
            // --- NUEVO: Comprobación del interruptor principal ---
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

        // Recibir la actualización de progreso de "Catch 'em All"
        ClientPlayNetworking.registerGlobalReceiver(CatchProgressPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                currentCatchEmAllGeneration = payload.generationName();
                catchCaughtCount = payload.caughtCount();
                catchTotalCount = payload.totalCount();
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
     * Crea un componente de texto con un efecto de color arcoíris.
     * @param text El texto a colorear.
     * @return Un MutableText con el efecto aplicado.
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
            // Asigna un color del arcoíris a cada caracter, ciclando a través de la lista de colores.
            rainbowText.append(Text.literal(String.valueOf(text.charAt(i))).formatted(rainbowColors[i % rainbowColors.length]));
        }
        return rainbowText;
    }
}