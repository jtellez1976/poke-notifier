package com.zehro_mc.pokenotifier.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigPokemon;
import kotlin.Unit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Announces rare or ultra-rare Pokémon evolutions safely after animation.
 */
public class EvolutionListener {

    public static void register() {
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.NORMAL, EvolutionListener::onPokemonEvolved);
    }

    private static Unit onPokemonEvolved(EvolutionCompleteEvent event) {
        Pokemon evolvedPokemon = event.getPokemon();
        @Nullable ServerPlayerEntity owner = evolvedPokemon.getOwnerPlayer();
        if (owner == null) return Unit.INSTANCE;

        String evolvedName = capitalize(evolvedPokemon.getSpecies().getName());
        String preEvolutionName = tryGetPreviousSpecies(event, evolvedPokemon);

        // Prevent displaying "Unknown Pokémon"
        if (preEvolutionName.equals("Unknown Pokémon")) preEvolutionName = "their Pokémon";

        ConfigPokemon config = ConfigManager.getPokemonConfig();

        boolean isUltra = config.ULTRA_RARE.contains(evolvedName.toLowerCase());
        boolean isRare = config.RARE.contains(evolvedName.toLowerCase());
        if (!isUltra && !isRare) return Unit.INSTANCE;

        Formatting color = isUltra ? Formatting.LIGHT_PURPLE : Formatting.GOLD;

        // Build message with isolated formatting
        Text message = Text.literal("🎉 Congratulations to ").formatted(Formatting.YELLOW)
                .append(Text.literal("").formatted(Formatting.RESET)) // reset color before player name
                .append(owner.getDisplayName())
                .append(Text.literal(" for evolving ").formatted(Formatting.YELLOW))
                .append(Text.literal(preEvolutionName).formatted(Formatting.AQUA))
                .append(Text.literal(" into a powerful ").formatted(Formatting.YELLOW))
                .append(Text.literal(evolvedName).formatted(color))
                .append(Text.literal("!").formatted(Formatting.YELLOW));

        MinecraftServer server = owner.getServer();
        if (server == null) return Unit.INSTANCE;

        // Delay 3 seconds after animation
        scheduleDelayedTask(server, 60 * 3, () -> {
            server.getPlayerManager().broadcast(message, false);
            server.getPlayerManager().getPlayerList().forEach(player ->
                    player.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                            SoundCategory.MASTER, 0.8F, 1.2F)
            );
        });

        return Unit.INSTANCE;
    }

    /** Safely tries to get the previous Pokémon species name. */
    private static String tryGetPreviousSpecies(EvolutionCompleteEvent event, Pokemon evolvedPokemon) {
        try {
            Object evo = event.getEvolution();
            if (evo != null) {
                Method getFrom = evo.getClass().getMethod("getFromSpecies");
                Object fromSpecies = getFrom.invoke(evo);
                if (fromSpecies != null) {
                    Method getName = fromSpecies.getClass().getMethod("getName");
                    return capitalize((String) getName.invoke(fromSpecies));
                }
            }

            // fallback: look into Pokémon NBT / persistent data
            var data = evolvedPokemon.getPersistentData();
            if (data != null && data.contains("previous_species")) {
                return capitalize(data.getString("previous_species"));
            }
        } catch (Exception ignored) {}

        return "Unknown Pokémon";
    }

    /** Delay task using safe async call. */
    private static void scheduleDelayedTask(MinecraftServer server, int delayTicks, Runnable task) {
        new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 30L);
                server.execute(task);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) return "Unknown Pokémon";
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
