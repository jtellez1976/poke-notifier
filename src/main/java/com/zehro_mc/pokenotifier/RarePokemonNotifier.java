package com.zehro_mc.pokenotifier;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.zehro_mc.pokenotifier.networking.WaypointPayload;
import com.zehro_mc.pokenotifier.util.RarityUtil;
import kotlin.Unit;
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

    /**
     * Registra el listener de spawns en los eventos de Cobblemon.
     * Este es el método correcto para suscribirse a todos los spawns, incluidos los de comandos.
     */
    public static void register() {
        PokeNotifier.LOGGER.info("[Poke Notifier] Registering spawn listener...");
        // La forma correcta es suscribirse a POKEMON_ENTITY_SPAWN, que nos da un evento con la entidad.
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            onPokemonSpawn(event.getEntity());
            return Unit.INSTANCE;
        });
    }

    private static void onPokemonSpawn(PokemonEntity pokemonEntity) {
        if (pokemonEntity == null) return;

        Pokemon pokemon = pokemonEntity.getPokemon();

        // En Cobblemon 1.6+, la forma de detectar un spawn natural es comprobar que no tenga propietario.
        boolean isNaturalSpawn = pokemonEntity.getOwnerUuid() == null && !pokemonEntity.isRemoved();

        if (!isNaturalSpawn && !ConfigManager.getServerConfig().enable_test_mode) {
            return; // Ignora si no es natural y el test_mode está apagado.
        }

        BlockPos pokemonPos = pokemonEntity.getBlockPos();
        if (pokemonEntity.getServer() == null) return;

        for (ServerPlayerEntity player : pokemonEntity.getServer().getPlayerManager().getPlayerList()) {
            RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon, player);
            if (rarity == RarityUtil.RarityCategory.COMMON) continue;

            double distance = player.getPos().distanceTo(pokemonPos.toCenterPos());
            if (distance > ConfigManager.getClientConfig().notification_distance) continue;

            if (!PokeNotifier.TRACKED_POKEMON.containsKey(pokemonEntity)) {
                PokeNotifier.TRACKED_POKEMON.put(pokemonEntity, rarity);

                if (ConfigManager.getServerConfig().debug_mode_enabled) {
                    PokeNotifier.LOGGER.info("Started tracking Pokémon: " + pokemon.getSpecies().getName());
                }

                int glowingTicks = ConfigManager.getClientConfig().glowing_duration_seconds * 20;
                pokemonEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, glowingTicks, 0, false, false));
            }

            String status = hasCaughtSpecies(player, pokemon) ? "CAUGHT" : "NEW";
            RegistryEntry<Biome> biomeRegistryEntry = player.getWorld().getBiome(pokemonPos);
            Identifier biomeId = biomeRegistryEntry.getKey().map(key -> key.getValue()).orElse(BiomeKeys.PLAINS.getValue());

            // Nombre sanitizado (minúsculas y guiones)
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

            if (ConfigManager.getServerConfig().debug_mode_enabled) {
                PokeNotifier.LOGGER.info("Notified " + player.getName().getString() + " about a " + rarity.name() + " " + pokemon.getSpecies().getName() + " at " + pokemonPos);
            }
        }
    }

    private static boolean hasCaughtSpecies(ServerPlayerEntity player, Pokemon pokemonToFind) {
        // Check player's party
        PokemonStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Iterator<Pokemon> partyIterator = party.iterator();
        while (partyIterator.hasNext()) {
            Pokemon p = partyIterator.next();
            if (p != null && p.getSpecies() == pokemonToFind.getSpecies()) return true;
        }

        // Check PC
        PokemonStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        Iterator<Pokemon> pcIterator = pc.iterator();
        while (pcIterator.hasNext()) {
            Pokemon p = pcIterator.next();
            if (p != null && p.getSpecies() == pokemonToFind.getSpecies()) return true;
        }

        return false;
    }
}
