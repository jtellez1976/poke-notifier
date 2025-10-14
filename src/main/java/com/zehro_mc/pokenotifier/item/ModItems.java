/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.item;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers all custom items, such as the Pokédex trophies.
 */
public class ModItems {

    public static final Item KANTO_TROPHY = registerItem("kanto_trophy");
    public static final Item JOHTO_TROPHY = registerItem("johto_trophy");
    public static final Item HOENN_TROPHY = registerItem("hoenn_trophy");
    public static final Item SINNOH_TROPHY = registerItem("sinnoh_trophy");
    public static final Item UNOVA_TROPHY = registerItem("unova_trophy");
    public static final Item KALOS_TROPHY = registerItem("kalos_trophy");
    public static final Item ALOLA_TROPHY = registerItem("alola_trophy");
    public static final Item GALAR_TROPHY = registerItem("galar_trophy");
    public static final Item PALDEA_TROPHY = registerItem("paldea_trophy");

    private static Item registerItem(String name) {
        return Registry.register(Registries.ITEM, Identifier.of(PokeNotifier.MOD_ID, name),
                new PokedexTrophyItem(new Item.Settings().maxCount(1)));
    }

    public static void registerModItems() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(KANTO_TROPHY);
            content.add(JOHTO_TROPHY);
            content.add(HOENN_TROPHY);
            content.add(SINNOH_TROPHY);
            content.add(UNOVA_TROPHY);
            content.add(KALOS_TROPHY);
            content.add(ALOLA_TROPHY);
            content.add(GALAR_TROPHY);
            content.add(PALDEA_TROPHY);
        });
    }
}