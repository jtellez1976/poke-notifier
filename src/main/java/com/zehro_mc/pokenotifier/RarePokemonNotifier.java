package com.zehro_mc.pokenotifier;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.zehro_mc.pokenotifier.networking.WaypointPayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

import java.util.Iterator;

public class RarePokemonNotifier {

    public static void onPokemonSpawn(PokemonEntity pokemonEntity) {
        Pokemon pokemon = pokemonEntity.getPokemon();
        RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon);

        // No notificar si la rareza es NONE o UNCOMMON
        if (rarity == RarityUtil.RarityCategory.NONE || rarity == RarityUtil.RarityCategory.UNCOMMON) {
            return;
        }

        PokeNotifier.TRACKED_POKEMON.put(pokemonEntity, rarity);
        PokeNotifier.LOGGER.info("Started tracking Pokémon: " + pokemon.getSpecies().getName());

        // Aplicar el efecto de brillo usando la duración de la configuración
        int glowingTicks = ConfigManager.getConfig().glowing_duration_seconds * 20;
        pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, glowingTicks, 0, false, false));

        BlockPos pokemonPos = pokemonEntity.getBlockPos();

        if (pokemonEntity.getServer() == null) return;

        for (ServerPlayerEntity player : pokemonEntity.getServer().getPlayerManager().getPlayerList()) {
            double distance = player.getPos().distanceTo(pokemonPos.toCenterPos());

            if (distance > ConfigManager.getConfig().notification_distance) {
                continue;
            }

            // Verificar si el jugador ya posee esta especie en su equipo o PC
            String status = hasCaughtSpecies(player, pokemon) ? "CAUGHT" : "NEW";

            // Obtenemos el ID del bioma
            RegistryEntry<Biome> biomeRegistryEntry = player.getWorld().getBiome(pokemonPos);
            Identifier biomeId = biomeRegistryEntry.getKey().map(key -> key.getValue()).orElse(BiomeKeys.PLAINS.getValue());

            // Reproducir sonido solo si el Pokémon es nuevo para el jugador
            if (status.equals("NEW")) {
                player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 1.0F);
            }

            // Construimos el Identifier para nuestro sprite personalizado.
            String pokemonName = pokemon.getSpecies().getName().toLowerCase();
            Identifier spriteIdentifier = Identifier.of(PokeNotifier.MOD_ID, "textures/pokemon/" + pokemonName + ".png");

            // Crear y enviar el paquete con toda la información al cliente
            WaypointPayload payload = new WaypointPayload(
                    pokemon.getUuid().toString(),
                    pokemon.getDisplayName().getString(),
                    pokemonPos,
                    rarity.getWaypointColor(),
                    status,
                    rarity.name(),
                    "Lvl " + pokemon.getLevel(),
                    distance,
                    biomeId,
                    spriteIdentifier
            );
            ServerPlayNetworking.send(player, payload);

            PokeNotifier.LOGGER.info("Notified " + player.getName().getString() + " about a " + rarity.name() + " " + pokemon.getSpecies().getName() + " at " + pokemonPos);
        }
    }

    private static boolean hasCaughtSpecies(ServerPlayerEntity player, Pokemon pokemonToFind) {
        // Primero, revisamos el equipo (party) del jugador
        PokemonStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Iterator<Pokemon> partyIterator = party.iterator();
        while (partyIterator.hasNext()) {
            Pokemon p = partyIterator.next();
            if (p != null && p.getSpecies() == pokemonToFind.getSpecies()) {
                return true;
            }
        }

        // Si no está en el equipo, revisamos el PC del jugador
        PokemonStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        Iterator<Pokemon> pcIterator = pc.iterator();
        while (pcIterator.hasNext()) {
            Pokemon p = pcIterator.next();
            if (p != null && p.getSpecies() == pokemonToFind.getSpecies()) {
                return true;
            }
        }

        return false;
    }
}