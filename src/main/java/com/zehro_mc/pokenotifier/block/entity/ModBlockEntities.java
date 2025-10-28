/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.block.entity;

import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.block.ModBlocks;
import com.zehro_mc.pokenotifier.block.TrophyPedestalBlockEntity;
import com.zehro_mc.pokenotifier.block.TrophyAltarBlockEntity;
import com.mojang.datafixers.types.Type;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers all custom Block Entity types for the mod.
 */
public class ModBlockEntities {
    public static final BlockEntityType<TrophyDisplayBlockEntity> TROPHY_DISPLAY_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(PokeNotifier.MOD_ID, "trophy_display_block_entity"),
                    BlockEntityType.Builder.create(TrophyDisplayBlockEntity::new, ModBlocks.TROPHY_DISPLAY_BLOCK).build((Type) null));
    
    public static final BlockEntityType<TrophyPedestalBlockEntity> TROPHY_PEDESTAL = 
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(PokeNotifier.MOD_ID, "trophy_pedestal"),
                    BlockEntityType.Builder.create(TrophyPedestalBlockEntity::new, ModBlocks.TROPHY_PEDESTAL).build((Type) null));
    
    public static final BlockEntityType<TrophyAltarBlockEntity> TROPHY_ALTAR = 
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(PokeNotifier.MOD_ID, "trophy_altar"),
                    BlockEntityType.Builder.create(TrophyAltarBlockEntity::new, ModBlocks.TROPHY_ALTAR).build((Type) null));

    public static void registerBlockEntities() {}
}