package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.networking.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.networking.WaypointPayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kyori.adventure.platform.fabric.FabricClientAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PokeNotifierClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("PokeNotifierClient");
    private FabricClientAudiences adventure;
    public static final Map<String, Waypoint> activeWaypoints = new HashMap<>();

    @Override
    public void onInitializeClient() {
        this.adventure = FabricClientAudiences.of();

        // Receptor para el Waypoint y el mensaje de aparición
        ClientPlayNetworking.registerGlobalReceiver(WaypointPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Lógica del Waypoint (si la tienes)
                Waypoint waypoint = new Waypoint(
                        payload.uuid(),
                        payload.name(),
                        payload.pos(),
                        payload.color()
                );
                activeWaypoints.put(payload.uuid(), waypoint);
                LOGGER.info("Waypoint added for: " + payload.name());

                // Construir y enviar el mensaje de chat aquí en el cliente
                try {
                    Component message = createFormattedSpawnMessage(payload);
                    this.adventure.audience().sendMessage(message);
                } catch (Exception e) {
                    LOGGER.error("Failed to create or send spawn notification message", e);
                }
            });
        });

        // Receptor para DESPAWN o CAPTURA
        ClientPlayNetworking.registerGlobalReceiver(StatusUpdatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Eliminar el waypoint asociado
                activeWaypoints.remove(payload.uuid());
                LOGGER.info("Waypoint removed for: " + payload.name());

                // Mostrar el mensaje de estado
                try {
                    Component message = createStatusUpdateMessage(payload);
                    this.adventure.audience().sendMessage(message);
                } catch (Exception e) {
                    LOGGER.error("Failed to create or send status update message", e);
                }
            });
        });
    }

    private Component createFormattedSpawnMessage(WaypointPayload payload) {
        RarityUtil.RarityCategory rarity = RarityUtil.RarityCategory.valueOf(payload.rarityCategoryName());
        Identifier biomeId = payload.biomeId();

        // Construimos el mensaje usando las claves de traducción del archivo en_us.json
        Component prefix = Component.translatable("chat.poke-notifier.prefix");
        Component rarityText = rarity.getRarityName(); // Esto ya es un Component.translatable
        Component pokemonName = Component.text(payload.name(), rarity.getChatColor(), TextDecoration.BOLD);
        Component levelText = Component.text("(" + payload.level() + ")", NamedTextColor.YELLOW);
        Component coordsText = Component.text(String.format("%d, %d, %d", payload.pos().getX(), payload.pos().getY(), payload.pos().getZ()), NamedTextColor.GREEN);
        String distanceString = String.format("%.1f", payload.distance());
        Component biomeName = Component.translatable("biome." + biomeId.getNamespace() + "." + biomeId.getPath());

        // Usamos la clave principal y le pasamos los componentes como argumentos
        return prefix
                .append(Component.text("A wild ", NamedTextColor.YELLOW))
                .append(rarityText.color(rarity.getChatColor()))
                .append(Component.text(" "))
                .append(pokemonName)
                .append(Component.text(" "))
                .append(levelText)
                .append(Component.text(" has appeared at ", NamedTextColor.YELLOW))
                .append(coordsText)
                .append(Component.text(" (" + distanceString + " blocks away) in a ", NamedTextColor.YELLOW))
                .append(biomeName.color(NamedTextColor.YELLOW))
                .append(Component.text(" biome!", NamedTextColor.YELLOW));
    }

    private Component createStatusUpdateMessage(StatusUpdatePayload payload) {
        RarityUtil.RarityCategory rarity = RarityUtil.RarityCategory.valueOf(payload.rarityCategoryName());
        Component prefix = Component.translatable("chat.poke-notifier.prefix");

        if (payload.updateType() == StatusUpdatePayload.UpdateType.CAPTURED) {
            Component pokemonName = Component.text(payload.name(), rarity.getChatColor(), TextDecoration.BOLD);
            Component playerName = Component.text(payload.playerName(), NamedTextColor.AQUA);
            return Component.text()
                    .append(playerName)
                    .append(Component.text(" has captured the ", NamedTextColor.GREEN))
                    .append(pokemonName)
                    .append(Component.text("!", NamedTextColor.GREEN))
                    .build();
        } else { // DESPAWNED
            // Creamos el nombre sin negrita para el mensaje de despawn
            Component pokemonName = Component.text(payload.name(), rarity.getChatColor());
            return prefix
                    .append(Component.text("The wild ", NamedTextColor.YELLOW))
                    .append(pokemonName)
                    .append(Component.text(" has despawned...", NamedTextColor.YELLOW));
        }
    }
}