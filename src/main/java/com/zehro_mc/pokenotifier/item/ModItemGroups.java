/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
    
    public static final ItemGroup POKE_NOTIFIER_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(PokeNotifier.MOD_ID, "poke_notifier"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.KANTO_TROPHY))
                    .displayName(Text.translatable("itemgroup.poke-notifier"))
                    .entries((displayContext, entries) -> {
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
    
    public static void registerItemGroups() {
        PokeNotifier.LOGGER.info("Registering Item Groups for " + PokeNotifier.MOD_ID);
    }
}
