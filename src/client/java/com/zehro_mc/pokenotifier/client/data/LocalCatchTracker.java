package com.zehro_mc.pokenotifier.client.data;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Client-side tracking for Custom Hunt and Catch 'em All in client-only mode.
 * Monitors party changes to detect new captures.
 */
public class LocalCatchTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokeNotifier.MOD_ID + "-LocalTracker");
    private static final Set<UUID> knownPokemon = new HashSet<>();
    private static boolean initialized = false;
    
    public static void initialize() {
        // Only initialize if we're in true client-only mode
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) {
            LOGGER.info("[FULL-SERVER] Skipping local catch tracker (server has mod)");
            return;
        }
        
        ClientOnlyDataManager.initialize();
        
        // Give device on first launch in client-only mode
        giveDeviceIfNeeded();
        
        LOGGER.info("Initializing client-side Pokemon tracker...");
        
        // Monitor party changes every tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                return;
            }
            
            // Initialize known Pokemon on first tick
            if (!initialized) {
                scanCurrentParty();
                initialized = true;
                // Removed excessive logging
                return;
            }
            
            // Check for new Pokemon every 20 ticks (1 second)
            if (client.world.getTime() % 20 == 0) {
                checkForNewCaptures();
            }
        });
        
        LOGGER.info("Local catch tracker initialized");
    }
    
    private static void giveDeviceIfNeeded() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Check if player already has device
        boolean hasDevice = false;
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            var stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof com.zehro_mc.pokenotifier.item.PokeNotifierDeviceItem) {
                hasDevice = true;
                break;
            }
        }
        
        if (!hasDevice) {
            // Give device via command (client-side can't directly add items)
            client.player.sendMessage(
                Text.literal("[Poke Notifier] ").formatted(Formatting.DARK_AQUA, Formatting.BOLD)
                    .append(Text.literal("Welcome! Craft your ").formatted(Formatting.GRAY))
                    .append(Text.literal("Poké Notifier Device").formatted(Formatting.GOLD))
                    .append(Text.literal(" to get started!").formatted(Formatting.GRAY)),
                false
            );
        }
    }
    
    private static void scanCurrentParty() {
        try {
            var party = com.cobblemon.mod.common.client.CobblemonClient.INSTANCE.getStorage().getParty();
            if (party != null) {
                for (Pokemon pokemon : party) {
                    if (pokemon != null) {
                        knownPokemon.add(pokemon.getUuid());
                        // Mark species as seen (simulates PC)
                        String speciesName = pokemon.getSpecies().getResourceIdentifier().getPath();
                        ClientOnlyDataManager.markSpeciesAsSeen(speciesName);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error scanning party", e);
        }
    }
    
    private static void checkForNewCaptures() {
        // Don't run in full-server mode
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return;
        
        try {
            var party = com.cobblemon.mod.common.client.CobblemonClient.INSTANCE.getStorage().getParty();
            if (party == null) return;
            
            String activeGen = ClientOnlyDataManager.getActiveGeneration();
            boolean hasActiveGen = !activeGen.equals("none");
            
            for (Pokemon pokemon : party) {
                if (pokemon != null) {
                    String speciesName = pokemon.getSpecies().getResourceIdentifier().getPath();
                    
                    // Always mark as seen for NEW/CAUGHT detection
                    ClientOnlyDataManager.markSpeciesAsSeen(speciesName);
                    
                    // Check if this is a new capture (not in knownPokemon)
                    if (!knownPokemon.contains(pokemon.getUuid())) {
                        knownPokemon.add(pokemon.getUuid());
                        onPokemonCaptured(pokemon);
                    }
                    // Even if not new capture, update Catch 'em All if active
                    else if (hasActiveGen && !ClientOnlyDataManager.hasCaught(activeGen, speciesName)) {
                        var genData = com.zehro_mc.pokenotifier.ConfigManager.getGenerationData(activeGen);
                        if (genData != null && genData.pokemon.contains(speciesName)) {
                            ClientOnlyDataManager.addCaughtPokemon(activeGen, speciesName);
                            // Update HUD counter immediately
                            com.zehro_mc.pokenotifier.client.PokeNotifierClient.catchCaughtCount = ClientOnlyDataManager.getCaughtCount(activeGen);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore - party might not be loaded yet
        }
    }
    
    private static void onPokemonCaptured(Pokemon pokemon) {
        String pokemonName = pokemon.getSpecies().getResourceIdentifier().getPath();
        // Removed excessive logging
        
        // Mark species as seen (simulates PC storage)
        ClientOnlyDataManager.markSpeciesAsSeen(pokemonName);
        
        // Check Custom Hunt
        if (ClientOnlyDataManager.isInCustomHunt(pokemonName)) {
            notifyCustomHuntCapture(pokemonName);
        }
        
        // Check Catch 'em All
        String activeGen = ClientOnlyDataManager.getActiveGeneration();
        if (!activeGen.equals("none")) {
            if (!ClientOnlyDataManager.hasCaught(activeGen, pokemonName)) {
                ClientOnlyDataManager.addCaughtPokemon(activeGen, pokemonName);
                notifyCatchEmAllProgress(pokemonName, activeGen);
            }
        }
    }
    
    // Custom Hunt Methods
    public static void addToCustomHunt(String pokemonName) {
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return;
        ClientOnlyDataManager.addToCustomHunt(pokemonName);
        // Removed excessive logging
    }
    
    public static void removeFromCustomHunt(String pokemonName) {
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return;
        ClientOnlyDataManager.removeFromCustomHunt(pokemonName);
    }
    
    public static void clearCustomHunt() {
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return;
        ClientOnlyDataManager.clearCustomHunt();
    }
    
    public static List<String> getCustomHuntList() {
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return new ArrayList<>();
        return ClientOnlyDataManager.getCustomHuntList();
    }
    
    // Catch 'em All Methods
    public static void enableCatchEmAll(String generation) {
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return;
        ClientOnlyDataManager.enableCatchEmAll(generation);
        
        // Update HUD variables
        com.zehro_mc.pokenotifier.client.PokeNotifierClient.currentCatchEmAllGeneration = generation;
        var genData = com.zehro_mc.pokenotifier.ConfigManager.getGenerationData(generation);
        if (genData != null) {
            com.zehro_mc.pokenotifier.client.PokeNotifierClient.catchTotalCount = genData.pokemon.size();
            com.zehro_mc.pokenotifier.client.PokeNotifierClient.catchCaughtCount = ClientOnlyDataManager.getCaughtCount(generation);
        }
        
        // Show activation toast with region name only
        String regionName = getRegionName(generation);
        com.zehro_mc.pokenotifier.client.ActivationFeedbackHUD.show(
            Text.literal("Now Tracking " + regionName), true
        );
    }
    
    public static void disableCatchEmAll() {
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return;
        ClientOnlyDataManager.disableCatchEmAll();
        
        // Reset HUD variables
        com.zehro_mc.pokenotifier.client.PokeNotifierClient.currentCatchEmAllGeneration = "none";
        com.zehro_mc.pokenotifier.client.PokeNotifierClient.catchCaughtCount = 0;
        com.zehro_mc.pokenotifier.client.PokeNotifierClient.catchTotalCount = 0;
        
        // Show deactivation toast
        com.zehro_mc.pokenotifier.client.ActivationFeedbackHUD.show(
            Text.literal("Tracking Disabled"), false
        );
    }
    
    public static String getActiveGeneration() {
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return "none";
        return ClientOnlyDataManager.getActiveGeneration();
    }
    
    public static int getCaughtCount(String generation) {
        if (!com.zehro_mc.pokenotifier.client.util.ServerModDetector.isClientOnly()) return 0;
        return ClientOnlyDataManager.getCaughtCount(generation);
    }
    
    // Notifications
    private static void notifyCustomHuntCapture(String pokemonName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                Text.literal("[Poke Notifier] ").formatted(Formatting.GOLD)
                    .append(Text.literal("Captured ").formatted(Formatting.YELLOW))
                    .append(Text.literal(pokemonName).formatted(Formatting.GREEN))
                    .append(Text.literal(" from your hunt list!").formatted(Formatting.YELLOW)),
                false
            );
        }
    }
    
    private static void notifyCatchEmAllProgress(String pokemonName, String generation) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            int count = ClientOnlyDataManager.getCaughtCount(generation);
            
            // Update HUD counter in real-time
            com.zehro_mc.pokenotifier.client.PokeNotifierClient.catchCaughtCount = count;
            
            client.player.sendMessage(
                Text.literal("[Catch 'em All] ").formatted(Formatting.GOLD)
                    .append(Text.literal("New: ").formatted(Formatting.YELLOW))
                    .append(Text.literal(pokemonName).formatted(Formatting.GREEN))
                    .append(Text.literal(" (" + count + " caught)").formatted(Formatting.GRAY)),
                false
            );
        }
    }
    
    private static String getRegionName(String generation) {
        var genData = com.zehro_mc.pokenotifier.ConfigManager.getGenerationData(generation);
        if (genData != null && genData.region != null) {
            return genData.region.substring(0, 1).toUpperCase() + genData.region.substring(1);
        }
        return generation.substring(0, 1).toUpperCase() + generation.substring(1);
    }
}
