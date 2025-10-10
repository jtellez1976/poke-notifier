package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.networking.PokeNotifierPackets;
import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.networking.WaypointPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

    @Override
    public void onInitializeClient() {
        PokeNotifierPackets.registerS2CPackets();
        HudRenderCallback.EVENT.register(NotificationHUD::render);
        // La siguiente línea está comentada para evitar errores de compilación mientras nos enfocamos en los bugs.
        // ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ClientCommands.register(dispatcher));

        ClientPlayNetworking.registerGlobalReceiver(WaypointPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            client.execute(() -> {
                String formattedCategory = formatCategoryName(payload.rarityCategoryName());

                // Lógica para mostrar el HUD
                if (ConfigManager.getClientConfig().alert_toast_enabled) {
                    Text title = Text.literal("A ")
                            .append(Text.literal(formattedCategory + " " + payload.name())
                                    .styled(style -> style.withColor(payload.color())))
                            .append(Text.literal(" has appeared"));
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

                    // 3. Añadimos el nombre del Pokémon con su color de rareza
                    chatMessage.append(Text.literal(formattedCategory + " " + payload.name())
                            .styled(style -> style.withColor(payload.color())));

                    // 4. Añadimos el estado [NEW] o [CAUGHT] con su propio color
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
                if (ConfigManager.getClientConfig().alert_sounds_enabled && "NEW".equals(payload.status())) {
                    if (client.player != null) {
                        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 0.5F));
                    }
                }

                ACTIVE_WAYPOINTS.put(payload.uuid(), payload.pos());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(StatusUpdatePayload.ID, (payload, context) -> {
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
}