/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.blocks;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    
    public static final Block TROPHY_PEDESTAL = registerBlock("trophy_pedestal",
            new TrophyPedestalBlock(AbstractBlock.Settings.create()
                    .strength(3.0f, 6.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .requiresTool()
                    .nonOpaque()));
    
    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(PokeNotifier.MOD_ID, name), block);
    }
    
    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(PokeNotifier.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }
    
    public static void registerModBlocks() {
        PokeNotifier.LOGGER.info("Registering blocks for " + PokeNotifier.MOD_ID);
    }
}