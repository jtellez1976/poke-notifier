package com.zehro_mc.pokenotifier.block.entity;

import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.block.ModBlocks;
import com.mojang.datafixers.types.Type;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<TrophyDisplayBlockEntity> TROPHY_DISPLAY_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(PokeNotifier.MOD_ID, "trophy_display_block_entity"),
                    BlockEntityType.Builder.create(TrophyDisplayBlockEntity::new, ModBlocks.TROPHY_DISPLAY_BLOCK).build((Type) null));

    public static void registerBlockEntities() {}
}