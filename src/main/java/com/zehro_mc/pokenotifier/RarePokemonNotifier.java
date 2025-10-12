package com.zehro_mc.pokenotifier;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.zehro_mc.pokenotifier.model.GenerationData;
import com.zehro_mc.pokenotifier.model.PlayerCatchProgress;
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
import java.util.Set;

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

    public static void onPokemonSpawn(PokemonEntity pokemonEntity) {
        if (pokemonEntity == null) return;

        Pokemon pokemon = pokemonEntity.getPokemon();

        // En Cobblemon 1.6+, la forma de detectar un spawn natural es comprobar que no tenga propietario.
        // Y ahora, también comprobamos que no tenga nuestra etiqueta de prueba.
        boolean isFromTestSpawn = pokemon.getPersistentData().getBoolean("pokenotifier_test_spawn");
        boolean isNaturalSpawn = pokemonEntity.getOwnerUuid() == null && !pokemonEntity.isRemoved() && !isFromTestSpawn;

        // CORRECCIÓN: La lógica correcta es: si el spawn NO es natural, solo debemos continuar
        // si el modo de prueba está activado. Si no, salimos.
        if (!isNaturalSpawn && !ConfigManager.getServerConfig().enable_test_mode) {
            return;
        }

        BlockPos pokemonPos = pokemonEntity.getBlockPos();
        if (pokemonEntity.getServer() == null) return;

        for (ServerPlayerEntity player : pokemonEntity.getServer().getPlayerManager().getPlayerList()) {
            // --- LÓGICA DE RAREZA NORMAL (Ahora con mayor prioridad) ---
            RarityUtil.RarityCategory rarity = RarityUtil.getRarity(pokemon, player);

            // Si el Pokémon tiene una rareza notificable (o es shiny), lo notificamos y terminamos para este jugador.
            if (rarity != RarityUtil.RarityCategory.COMMON) {
                double distance = player.getPos().distanceTo(pokemonPos.toCenterPos());
                if (distance <= ConfigManager.getClientConfig().notification_distance) {
                    sendNotification(player, pokemonEntity, rarity);
                }
                // Continuamos al siguiente jugador para no enviar notificaciones duplicadas (ej. Raro y de Caza).
                continue;
            }

            // --- LÓGICA CATCH 'EM ALL (si no se cumplió la rareza normal) ---
            PlayerCatchProgress progress = ConfigManager.getPlayerCatchProgress(player.getUuid());
            if (!progress.active_generations.isEmpty()) {
                String activeGen = progress.active_generations.iterator().next();
                GenerationData genData = ConfigManager.getGenerationData(activeGen);
                // Usamos el path del Identifier para tener el nombre limpio y consistente (ej: nidoran_f, mr_mime)
                String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();

                if (genData != null && genData.pokemon.contains(pokemonName)) {
                    Set<String> caughtInGen = progress.caught_pokemon.getOrDefault(activeGen, Set.of());

                    if (!caughtInGen.contains(pokemonName)) {
                        sendNotification(player, pokemonEntity, RarityUtil.RarityCategory.HUNT);
                    }
                }
            }
        }
    }

    /**
     * Método de ayuda para enviar la notificación a un jugador.
     */
    private static void sendNotification(ServerPlayerEntity player, PokemonEntity pokemonEntity, RarityUtil.RarityCategory rarity) {
        Pokemon pokemon = pokemonEntity.getPokemon();
        BlockPos pokemonPos = pokemonEntity.getBlockPos();
        double distance = player.getPos().distanceTo(pokemonPos.toCenterPos());

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

        // Usamos getShowdownName() para obtener un nombre limpio y consistente para la ruta del archivo.
        // Esto soluciona problemas con nombres como "Nidoran♀" o "Mr. Mime".
        String spriteName = pokemon.getForm().getSpecies().getResourceIdentifier().getPath();

        // --- LÓGICA PARA SPRITES SHINY ---
        // Si el Pokémon es shiny, cambiamos la ruta a la subcarpeta /shiny/
        // Y añadimos el sufijo "shiny" al nombre del archivo.
        String spritePath = "textures/pokemon/";
        if (pokemon.getShiny()) {
            spritePath += "shiny/";
            spriteName += "shiny";
        }
        Identifier spriteIdentifier = Identifier.of(PokeNotifier.MOD_ID, spritePath + spriteName + ".png");

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
