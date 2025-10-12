package com.zehro_mc.pokenotifier.item;

import com.zehro_mc.pokenotifier.component.ModDataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class PokedexTrophyItem extends Item {

    public PokedexTrophyItem(Settings settings) {
        super(settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public boolean hasGlint(ItemStack stack) {
        // El trofeo siempre brillará como un objeto encantado.
        return true;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        // Leemos el nombre de la región desde nuestro componente de datos personalizado.
        String regionName = stack.get(ModDataComponents.REGION_NAME);
        if (regionName != null) {
            tooltip.add(Text.literal("A trophy for completing the " + regionName + " Pokédex.").formatted(Formatting.GOLD));
        }
        // No llamamos a super para tener control total sobre el tooltip.
    }
}