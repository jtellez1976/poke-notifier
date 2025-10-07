package com.zehro_mc.pokenotifier;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.networking.WaypointPayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public class RarePokemonNotifier {

    public static void onPokemonSpawn(PokemonEntity pokemonEntity) {
        Pokemon pokemon = pokemonEntity.getPokemon();
        RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon);

        // No notificar si la rareza es NONE o UNCOMMON por defecto
        if (rarity == RarityUtil.RarityCategory.NONE || rarity == RarityUtil.RarityCategory.UNCOMMON) {
            return;
        }

        PokeNotifier.TRACKED_POKEMON.put(pokemonEntity, rarity);
        PokeNotifier.LOGGER.info("Started tracking Pokémon: " + pokemon.getSpecies().getName());

        // Aplicar el efecto de brillo al Pokémon
        pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 2400, 0, false, false)); // 2 minutos de brillo

        BlockPos pokemonPos = pokemonEntity.getBlockPos();

        if (pokemonEntity.getServer() == null) return;

        FabricServerAudiences adventure = FabricServerAudiences.of(pokemonEntity.getServer());

        for (ServerPlayerEntity player : pokemonEntity.getServer().getPlayerManager().getPlayerList()) {
            double distance = player.getPos().distanceTo(pokemonPos.toCenterPos());

            if (distance > 200) {
                continue;
            }

            // Construir el mensaje detallado y colorido aquí en el servidor
            Component rarityText = rarity.getRarityName().color(rarity.getChatColor());
            Component pokemonName = Component.text(pokemon.getDisplayName().getString(), Style.style(rarity.getChatColor(), TextDecoration.BOLD));
            Component levelText = Component.text(" (Lvl. " + pokemon.getLevel() + ")");
            Component coordsText = Component.text(String.format("%d, %d, %d", pokemonPos.getX(), pokemonPos.getY(), pokemonPos.getZ()), NamedTextColor.GREEN);
            String distanceString = String.format("%.1f", distance);
            Component distanceText = Component.text(" (" + distanceString + " blocks away)");

            RegistryEntry<Biome> biomeRegistryEntry = player.getWorld().getBiome(pokemonPos);
            Identifier biomeId = biomeRegistryEntry.getKey().map(key -> key.getValue()).orElse(BiomeKeys.PLAINS.getValue());
            Component biomeName = Component.translatable("biome." + biomeId.getNamespace() + "." + biomeId.getPath());

            Component message = Component.text()
                    .append(Component.text("A wild ", NamedTextColor.WHITE))
                    .append(rarityText)
                    .append(Component.text(" "))
                    .append(pokemonName)
                    .append(levelText)
                    .append(Component.text(" has appeared at ", NamedTextColor.WHITE))
                    .append(coordsText)
                    .append(distanceText)
                    .append(Component.text(" in a ", NamedTextColor.WHITE))
                    .append(biomeName)
                    .append(Component.text(" biome!", NamedTextColor.WHITE))
                    .build();

            // Enviar el mensaje de chat al jugador
            adventure.audience(player).sendMessage(message);

            // Reproducir sonido para el jugador
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 1.0F);

            // Crear y enviar el paquete para el waypoint (esto es para la parte visual del waypoint, no el chat)
            WaypointPayload payload = new WaypointPayload(
                    pokemon.getUuid().toString(), // Usar UUID para identificar al Pokémon de forma única
                    pokemon.getDisplayName().getString(), // Nombre para mostrar
                    pokemonPos,
                    rarity.getWaypointColor(),
                    rarity.name(), // Nombre de la categoría de rareza
                    "Lvl " + pokemon.getLevel(), // Nivel
                    distance, // Distancia
                    biomeId // ID del bioma
            );
            ServerPlayNetworking.send(player, payload);

            PokeNotifier.LOGGER.info("Notified " + player.getName().getString() + " about a " + rarity.name() + " " + pokemon.getSpecies().getName() + " at " + pokemonPos);
        }
    }
}