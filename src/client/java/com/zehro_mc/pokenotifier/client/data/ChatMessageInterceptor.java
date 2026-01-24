package com.zehro_mc.pokenotifier.client.data;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts chat messages to detect Pokemon captures from Cobblemon.
 * This allows tracking captures even when Pokemon go directly to PC.
 */
public class ChatMessageInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokeNotifier.MOD_ID + "-ChatInterceptor");
    
    // Cobblemon capture message patterns
    private static final Pattern PC_PATTERN = Pattern.compile("(.+) has been added to Your PC");
    private static final Pattern PARTY_PATTERN = Pattern.compile("(.+) has been added to your party");
    
    public static void initialize() {
        LOGGER.info("Initializing chat message interceptor...");
        
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return; // Ignore overlay messages
            
            String messageText = message.getString();
            
            // Try to extract Pokemon name from capture message
            String pokemonName = extractPokemonName(messageText);
            if (pokemonName != null) {
                onPokemonCaptured(pokemonName);
            }
        });
        
        LOGGER.info("Chat message interceptor initialized");
    }
    
    private static String extractPokemonName(String message) {
        // Pattern 1: "X has been added to Your PC"
        Matcher matcher = PC_PATTERN.matcher(message);
        if (matcher.find()) {
            String name = cleanPokemonName(matcher.group(1));
            LOGGER.info("[CAPTURE DETECTED - PC] Raw: '{}' -> Clean: '{}'", matcher.group(1), name);
            return name;
        }
        
        // Pattern 2: "X has been added to your party"
        matcher = PARTY_PATTERN.matcher(message);
        if (matcher.find()) {
            String name = cleanPokemonName(matcher.group(1));
            LOGGER.info("[CAPTURE DETECTED - PARTY] Raw: '{}' -> Clean: '{}'", matcher.group(1), name);
            return name;
        }
        
        return null;
    }
    
    private static String cleanPokemonName(String name) {
        // Remove "Your party was full! " prefix if present
        name = name.replace("Your party was full! ", "");
        
        // Remove color codes, special characters, and convert to lowercase
        return name.replaceAll("§[0-9a-fk-or]", "")
                   .replaceAll("[^a-zA-Z0-9]", "")
                   .toLowerCase()
                   .trim();
    }
    
    private static void onPokemonCaptured(String pokemonName) {
        LOGGER.info("[PROCESSING CAPTURE] Species: {}", pokemonName);
        
        // Mark species as seen (includes PC)
        ClientOnlyDataManager.markSpeciesAsSeen(pokemonName);
        
        // Check Custom Hunt
        if (ClientOnlyDataManager.isInCustomHunt(pokemonName)) {
            LOGGER.info("[CUSTOM HUNT] Removed {} from hunt list", pokemonName);
        }
        
        // Check Catch 'em All
        String activeGen = ClientOnlyDataManager.getActiveGeneration();
        if (!activeGen.equals("none")) {
            var genData = com.zehro_mc.pokenotifier.ConfigManager.getGenerationData(activeGen);
            if (genData != null && genData.pokemon.contains(pokemonName)) {
                if (!ClientOnlyDataManager.hasCaught(activeGen, pokemonName)) {
                    ClientOnlyDataManager.addCaughtPokemon(activeGen, pokemonName);
                    int newCount = ClientOnlyDataManager.getCaughtCount(activeGen);
                    
                    // Update HUD counter immediately
                    com.zehro_mc.pokenotifier.client.PokeNotifierClient.catchCaughtCount = newCount;
                    
                    LOGGER.info("[CATCH EM ALL] Added {} to {} - New count: {}", pokemonName, activeGen, newCount);
                    
                    // Show notification
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.sendMessage(
                            Text.literal("[Catch 'em All] ").formatted(Formatting.GOLD)
                                .append(Text.literal("New: ").formatted(Formatting.YELLOW))
                                .append(Text.literal(pokemonName).formatted(Formatting.GREEN))
                                .append(Text.literal(" (" + newCount + " caught)").formatted(Formatting.GRAY)),
                            false
                        );
                    }
                }
            }
        }
    }
}
