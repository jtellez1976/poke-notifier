package com.zehro_mc.pokenotifier;

import com.cobblemon.mod.common.api.Priority;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.zehro_mc.pokenotifier.PokeNotifier.LOGGER;

public class RarePokemonNotifier {

    // Usamos un mapa para rastrear qué jugador fue notificado por cada Pokémon.
    private static final Map<UUID, UUID> ANNOUNCED_POKEMON = new HashMap<>();

    public static void register() {
        // Evento principal - spawn de Pokémon
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            PokemonEntity pokemonEntity = event.getEntity();
            World world = pokemonEntity.getWorld();

            if (world.isClient()) {
                return Unit.INSTANCE;
            }

            Pokemon pokemon = pokemonEntity.getPokemon();
            RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon);

            if (rarity != RarityUtil.RarityCategory.NONE) {
                LOGGER.info("[PokeNotifier-Debug] Pokémon spawned: {}", pokemon.getDisplayName().getString());
                LOGGER.info("[PokeNotifier-Debug] Recognized as {}!", rarity.name());
                handleRarePokemonSpawn(pokemonEntity, rarity);
            }

            return Unit.INSTANCE;
        });

        // Evento de despawn
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!world.isClient() && entity instanceof PokemonEntity pokemonEntity) {
                UUID notifiedPlayerUUID = ANNOUNCED_POKEMON.remove(pokemonEntity.getUuid());
                if (notifiedPlayerUUID != null) {
                    Pokemon pokemon = pokemonEntity.getPokemon();
                    LOGGER.info("[PokeNotifier-Debug] Announced Pokémon despawned: {}", pokemon.getDisplayName().getString());
                    handleStatusUpdate(pokemonEntity, notifiedPlayerUUID, StatusUpdatePayload.UpdateType.DESPAWNED);
                }
            }
        });

        // Evento de captura
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            Pokemon pokemon = event.getPokemon();
            UUID notifiedPlayerUUID = ANNOUNCED_POKEMON.remove(pokemon.getUuid());
            if (notifiedPlayerUUID != null) {
                LOGGER.info("[PokeNotifier-Debug] Announced Pokémon captured: {}", pokemon.getDisplayName().getString());
                handleStatusUpdate(pokemon.getEntity(), notifiedPlayerUUID, StatusUpdatePayload.UpdateType.CAPTURED);
            }
            return Unit.INSTANCE;
        });
    }

    private static void handleRarePokemonSpawn(PokemonEntity pokemonEntity, RarityUtil.RarityCategory rarity) {
        try {
            BlockPos pos = pokemonEntity.getBlockPos();
            Pokemon pokemon = pokemonEntity.getPokemon();

            pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 120, 0));

            PlayerEntity closestPlayer = pokemonEntity.getWorld().getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 128.0, false);

            if (closestPlayer instanceof ServerPlayerEntity serverPlayer) {
                ANNOUNCED_POKEMON.put(pokemonEntity.getUuid(), serverPlayer.getUuid());
                ANNOUNCED_POKEMON.put(pokemon.getUuid(), serverPlayer.getUuid());

                LOGGER.info("[PokeNotifier-Debug] Found player '{}'. Sending waypoint packet...", serverPlayer.getName().getString());
                sendWaypointPacket(serverPlayer, pokemonEntity, pos, rarity);
            } else {
                LOGGER.info("[PokeNotifier-Debug] Rare Pokémon spawned, but no player was found nearby.");
            }
        } catch (Exception e) {
            LOGGER.error("[PokeNotifier] Error in handleRarePokemonSpawn: {}", e.getMessage(), e);
        }
    }

    private static void sendWaypointPacket(ServerPlayerEntity player, PokemonEntity pokemonEntity, BlockPos pos, RarityUtil.RarityCategory rarity) {
        try {
            Identifier biomeId = player.getWorld().getBiome(pos).getKey().get().getValue();
            Pokemon pokemon = pokemonEntity.getPokemon();
            double distance = player.distanceTo(pokemonEntity);

            var payload = new WaypointPayload(
                    pokemon.getDisplayName().getString(),
                    pokemon.getSpecies().getName().substring(0, 1).toUpperCase(),
                    pos,
                    rarity.getWaypointColor(),
                    pokemon.getLevel(),
                    biomeId,
                    rarity.name(),
                    distance
            );
            ServerPlayNetworking.send(player, payload);
            LOGGER.info("[PokeNotifier-Debug] Waypoint packet sent successfully to {}!", player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("[PokeNotifier] Error sending waypoint packet: {}", e.getMessage(), e);
        }
    }

    private static void handleStatusUpdate(Entity pokemonEntity, UUID notifiedPlayerUUID, StatusUpdatePayload.UpdateType updateType) {
        try {
            if (pokemonEntity == null) return;

            ServerPlayerEntity serverPlayer = pokemonEntity.getServer().getPlayerManager().getPlayer(notifiedPlayerUUID);

            if (serverPlayer != null) {
                Pokemon pokemon = ((PokemonEntity) pokemonEntity).getPokemon();
                RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon);

                var payload = new StatusUpdatePayload(
                        pokemon.getDisplayName().getString(),
                        pokemon.getLevel(),
                        rarity.name(),
                        updateType
                );

                ServerPlayNetworking.send(serverPlayer, payload);
                LOGGER.info("[PokeNotifier-Debug] Status update packet ({}) sent successfully to {}!", updateType.name(), serverPlayer.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.error("[PokeNotifier] Error in handleStatusUpdate: {}", e.getMessage(), e);
        }
    }
}