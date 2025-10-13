package com.zehro_mc.pokenotifier.block;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
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

    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(PokeNotifier.MOD_ID, name), block);
    }

    public static void registerModBlocks() {}
}