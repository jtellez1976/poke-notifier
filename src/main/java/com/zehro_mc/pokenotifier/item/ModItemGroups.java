package com.zehro_mc.pokenotifier.item;

import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    
    public static ItemGroup POKE_NOTIFIER_GROUP = null;
    
    public static void registerItemGroups() {
        PokeNotifier.LOGGER.info("Registering Item Groups for " + PokeNotifier.MOD_ID);
        
        // Only register creative tab in full mode (when on server with mod)
        // In client-only mode, items should not appear in creative
        POKE_NOTIFIER_GROUP = Registry.register(Registries.ITEM_GROUP,
                Identifier.of(PokeNotifier.MOD_ID, "poke_notifier"),
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(ModItems.KANTO_TROPHY))
                        .displayName(Text.translatable("itemgroup.poke-notifier"))
                        .entries((displayContext, entries) -> {
                            // Check if client-only mode - hide items
                            try {
                                Class<?> detectorClass = Class.forName("com.zehro_mc.pokenotifier.client.util.ServerModDetector");
                                java.lang.reflect.Method isClientOnly = detectorClass.getMethod("isClientOnly");
                                boolean clientOnly = (boolean) isClientOnly.invoke(null);
                                if (clientOnly) {
                                    return; // Don't add items in client-only mode
                                }
                            } catch (Exception e) {
                                // If detection fails, show items (default behavior)
                            }
                            
                            // Poké Notifier Device
                            entries.add(ModItems.POKE_NOTIFIER_DEVICE);
                            
                            // Trophy Items
                            entries.add(ModItems.KANTO_TROPHY);
                            entries.add(ModItems.JOHTO_TROPHY);
                            entries.add(ModItems.HOENN_TROPHY);
                            entries.add(ModItems.SINNOH_TROPHY);
                            entries.add(ModItems.UNOVA_TROPHY);
                            entries.add(ModItems.KALOS_TROPHY);
                            entries.add(ModItems.ALOLA_TROPHY);
                            entries.add(ModItems.GALAR_TROPHY);
                            entries.add(ModItems.PALDEA_TROPHY);
                            
                            // Trophy Blocks
                            entries.add(ModBlocks.TROPHY_DISPLAY_BLOCK);
                            entries.add(ModBlocks.TROPHY_PEDESTAL);
                            entries.add(ModBlocks.TROPHY_ALTAR);
                        })
                        .build());
    }
}
