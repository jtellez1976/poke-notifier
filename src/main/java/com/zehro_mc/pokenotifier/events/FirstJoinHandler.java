package com.zehro_mc.pokenotifier.events;

import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.data.PlayerNotifierData;
import com.zehro_mc.pokenotifier.item.ModItems;
import com.zehro_mc.pokenotifier.item.PokeNotifierDeviceItem;
import com.zehro_mc.pokenotifier.networking.ModeStatusPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FirstJoinHandler {

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            
            // Check if first join gifts are enabled
            if (!ConfigManager.getServerConfig().give_notifier_on_first_join) {
                return;
            }
            
            // Check if player already received the device
            if (PlayerNotifierData.hasReceivedNotifier(player.getUuid())) {
                return;
            }
            
            // Give the Poké Notifier Device
            ItemStack device = new ItemStack(ModItems.POKE_NOTIFIER_DEVICE);
            PokeNotifierDeviceItem.setOwner(device, player.getName().getString());
            
            if (player.getInventory().insertStack(device)) {
                PlayerNotifierData.markAsReceived(player.getUuid());
                
                // Send welcome message if enabled
                if (ConfigManager.getServerConfig().notifier_welcome_message) {
                    sendWelcomeMessage(player);
                }
                
                PokeNotifier.LOGGER.info("Gave Poké Notifier Device to new player: {}", player.getName().getString());
            } else {
                PokeNotifier.LOGGER.warn("Failed to give Poké Notifier Device to {}: inventory full", 
                    player.getName().getString());
            }
        });
    }

    private static void sendWelcomeMessage(ServerPlayerEntity player) {
        // Send personal toast notification using ModeStatusPayload
        // This will trigger AdvancementPlaques toast for this player only
        ServerPlayNetworking.send(player, new ModeStatusPayload("Trainer's Technology", true));
        
        // Also send a chat message with usage instructions
        player.sendMessage(Text.literal("")
            .append(Text.literal("[Poke Notifier] ").formatted(Formatting.DARK_AQUA, Formatting.BOLD))
            .append(Text.literal("You received a ").formatted(Formatting.GRAY))
            .append(Text.literal("Poké Notifier Device").formatted(Formatting.GOLD))
            .append(Text.literal(" • Press ").formatted(Formatting.GRAY))
            .append(Text.literal("[N]").formatted(Formatting.GREEN, Formatting.BOLD))
            .append(Text.literal(" to use").formatted(Formatting.GRAY)),
            false
        );
    }
}
