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

        // La lógica ahora se ejecuta para todos los spawns, ideal para pruebas.
        // La comprobación de 'isNaturalSpawn' y 'testModeEnabled' ha sido eliminada.

        RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon);

        // MANTENEMOS ESTE FILTRO: No notificar si la rareza es COMMON o UNCOMMON.
        if (rarity == RarityUtil.RarityCategory.COMMON || rarity == RarityUtil.RarityCategory.UNCOMMON) {
            return;
        }

        PokeNotifier.TRACKED_POKEMON.put(pokemonEntity, rarity);
        PokeNotifier.LOGGER.info("Started tracking Pokémon: " + pokemon.getSpecies().getName());

        int glowingTicks = ConfigManager.getClientConfig().glowing_duration_seconds * 20;
        pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, glowingTicks, 0, false, false));

        BlockPos pokemonPos = pokemonEntity.getBlockPos();

        if (pokemonEntity.getServer() == null) return;

        for (ServerPlayerEntity player : pokemonEntity.getServer().getPlayerManager().getPlayerList()) {
            double distance = player.getPos().distanceTo(pokemonPos.toCenterPos());

            if (distance > ConfigManager.getClientConfig().notification_distance) {
                continue;
            }

            String status = hasCaughtSpecies(player, pokemon) ? "CAUGHT" : "NEW";

            RegistryEntry<Biome> biomeRegistryEntry = player.getWorld().getBiome(pokemonPos);
            Identifier biomeId = biomeRegistryEntry.getKey().map(key -> key.getValue()).orElse(BiomeKeys.PLAINS.getValue());



            // Sanitizamos el nombre: lo pasamos a minúsculas y reemplazamos los espacios por guiones.
            String pokemonName = pokemon.getSpecies().getName().toLowerCase().replace(' ', '-');

            Identifier spriteIdentifier = Identifier.of(PokeNotifier.MOD_ID, "textures/pokemon/" + pokemonName + ".png");

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
        // First, check the player's party
        PokemonStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Iterator<Pokemon> partyIterator = party.iterator();
        while (partyIterator.hasNext()) {
            Pokemon p = partyIterator.next();
            if (p != null && p.getSpecies() == pokemonToFind.getSpecies()) {
                return true;
            }
        }

        // If not in the party, check the player's PC
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