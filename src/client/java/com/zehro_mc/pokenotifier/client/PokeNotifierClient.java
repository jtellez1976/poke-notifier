package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.StatusUpdatePayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import com.zehro_mc.pokenotifier.WaypointPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.kyori.adventure.platform.fabric.FabricClientAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PokeNotifierClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("PokeNotifierClient");
    private FabricClientAudiences adventure;

    private static final SoundEvent SHINY_SOUND = SoundEvent.of(Identifier.of("cobblemon", "shiny.glint"));

    @Override
    public void onInitializeClient() {
        // El registro de los tipos de payload ya se hace en la clase principal del mod (PokeNotifier.java).
        // El cliente solo necesita registrar los "handlers" para cuando reciba esos paquetes.

        this.adventure = FabricClientAudiences.of();

        // Receptor para el SPAWN de un Pokémon
        ClientPlayNetworking.registerGlobalReceiver(WaypointPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                var player = context.player();
                if (player == null) return;

                try {
                    RarityUtil.RarityCategory rarity = RarityUtil.RarityCategory.valueOf(payload.rarityCategoryName());

                    if (rarity == RarityUtil.RarityCategory.SHINY) {
                        player.getWorld().playSound(player, player.getBlockPos(), SHINY_SOUND, SoundCategory.PLAYERS, 0.7f, 1.0f);
                    } else {
                        player.getWorld().playSound(player, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    }

                    Component message = createFormattedMessage(payload);
                    this.adventure.audience().sendMessage(message);
                } catch (Exception e) {
                    LOGGER.error("Failed to create or send spawn notification message", e);
                }
            });
        });

        // Receptor para DESPAWN o CAPTURA
        ClientPlayNetworking.registerGlobalReceiver(StatusUpdatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                var player = context.player();
                if (player == null) return;

                try {
                    Component message = createStatusUpdateMessage(payload);
                    this.adventure.audience().sendMessage(message);
                } catch (Exception e) {
                    LOGGER.error("Failed to create or send status update message", e);
                }
            });
        });
    }

    private Component createFormattedMessage(WaypointPayload payload) {
        Identifier biomeId = payload.biomeId();
        RarityUtil.RarityCategory rarity = RarityUtil.RarityCategory.valueOf(payload.rarityCategoryName());

        Component prefix = Component.translatable("chat.poke-notifier.prefix", NamedTextColor.YELLOW);
        Component rarityText = rarity.toComponent();
        Component pokemonName = Component.text(payload.name(), Style.style(rarity.getTextColor(), TextDecoration.BOLD));
        Component levelText = Component.text(payload.level());
        String distanceString = String.format("%.1f", payload.distance());
        Component coordinates = Component.text(String.format("%d, %d, %d (%s blocks away)", payload.pos().getX(), payload.pos().getY(), payload.pos().getZ(), distanceString), NamedTextColor.GREEN);
        Component biomeName = Component.translatable("biome." + biomeId.getNamespace() + "." + biomeId.getPath());

        Component mainMessage = Component.translatable("chat.poke-notifier.spawn_message", NamedTextColor.YELLOW,
                rarityText, pokemonName, levelText, coordinates, biomeName);

        return prefix.append(mainMessage);
    }

    private Component createStatusUpdateMessage(StatusUpdatePayload payload) {
        RarityUtil.RarityCategory rarity = RarityUtil.RarityCategory.valueOf(payload.rarityCategoryName());

        Component prefix = Component.translatable("chat.poke-notifier.prefix", NamedTextColor.YELLOW);
        Component rarityText = rarity.toComponent();
        Component pokemonName = Component.text(payload.name(), Style.style(rarity.getTextColor(), TextDecoration.BOLD));
        Component levelText = Component.text(payload.level());

        String translationKey;
        switch (payload.updateType()) {
            case CAPTURED:
                translationKey = "chat.poke-notifier.capture_message";
                break;
            default: // DESPAWNED
                translationKey = "chat.poke-notifier.despawn_message";
                break;
        }

        // SOLUCIÓN: Añadimos el nivel del Pokémon como tercer argumento.
        return prefix.append(Component.translatable(translationKey, NamedTextColor.YELLOW, rarityText, pokemonName, levelText));
    }
}