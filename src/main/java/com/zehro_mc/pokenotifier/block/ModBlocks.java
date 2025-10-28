package com.zehro_mc.pokenotifier.block;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block TROPHY_DISPLAY_BLOCK = registerBlock("trophy_display_block",
            new TrophyDisplayBlock(AbstractBlock.Settings.create()
                    .nonOpaque()
                    .luminance(state -> 12) // Emite luz.
                    .strength(-1.0f) // --- CORRECCIÓN: Hacemos el bloque indestructible como la Bedrock ---
                    .pistonBehavior(PistonBehavior.DESTROY)));
    
    public static final Block TROPHY_PEDESTAL = registerBlock("trophy_pedestal",
            new TrophyPedestalBlock(AbstractBlock.Settings.create()
                    .nonOpaque()
                    .strength(2.0f, 6.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .requiresTool()));
    
    public static final Block TROPHY_ALTAR = registerBlock("trophy_altar",
            new TrophyAltarBlock(AbstractBlock.Settings.create()
                    .nonOpaque()
                    .strength(3.0f, 8.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .requiresTool()));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(PokeNotifier.MOD_ID, name), block);
    }
    
    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(PokeNotifier.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {}
}