package com.zehro_mc.pokenotifier.item;

import com.zehro_mc.pokenotifier.block.ModBlocks;
import com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity;
import com.zehro_mc.pokenotifier.component.ModDataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Formatting;

import java.util.List;

public class PokedexTrophyItem extends Item {

    public PokedexTrophyItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        if (context.getWorld().getBlockState(pos).isReplaceable()) {
            BlockState blockState = ModBlocks.TROPHY_DISPLAY_BLOCK.getDefaultState();
            context.getWorld().setBlockState(pos, blockState, 3);

            if (context.getWorld().getBlockEntity(pos) instanceof TrophyDisplayBlockEntity be) {
                ItemStack stack = context.getStack();
                String trophyId = Registries.ITEM.getId(stack.getItem()).toString();
                // --- CORRECCIÓN: Aseguramos que ownerUuid nunca sea null ---
                String ownerUuid = stack.getOrDefault(ModDataComponents.OWNER_UUID, "");
                be.setTrophyData(trophyId, ownerUuid);
            }

            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                context.getStack().decrement(1);
            }

            return ActionResult.SUCCESS;
        }
        return super.useOnBlock(context);
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
        // --- CORRECCIÓN: Llamamos a super para que el nombre del objeto se muestre primero ---
        super.appendTooltip(stack, context, tooltip, type);

        // --- DESCRIPCIONES ÚNICAS Y ÉPICAS ---
        tooltip.add(getRegionDescription(stack.getItem()));
        tooltip.add(Text.empty()); // Línea en blanco para separar

        // Leemos el propietario desde nuestro componente de datos personalizado.
        String ownerName = stack.get(ModDataComponents.OWNER_NAME);
        if (ownerName != null && !ownerName.isEmpty()) {
            tooltip.add(Text.literal("Owner: ").formatted(Formatting.GRAY).append(Text.literal(ownerName).formatted(Formatting.AQUA)));
        } else {
            // Si no tiene propietario, es probable que sea del creativo.
            tooltip.add(Text.literal("Owner: ").formatted(Formatting.GRAY).append(Text.literal("Creative Mode").formatted(Formatting.YELLOW)));
        }
        // No llamamos a super para tener control total sobre el tooltip.
    }

    @Environment(EnvType.CLIENT)
    private Text getRegionDescription(Item item) {
        Text description;
        if (item == ModItems.KANTO_TROPHY) description = Text.literal("For the one who started the journey where it all began.");
        else if (item == ModItems.JOHTO_TROPHY) description = Text.literal("A testament to exploring ancient traditions and new frontiers.");
        else if (item == ModItems.HOENN_TROPHY) description = Text.literal("Awarded to a trainer who balanced the forces of land and sea.");
        else if (item == ModItems.SINNOH_TROPHY) description = Text.literal("A symbol of mastering the very fabric of time and space.");
        else if (item == ModItems.UNOVA_TROPHY) description = Text.literal("For a hero who sought the truth and ideals of a distant land.");
        else if (item == ModItems.KALOS_TROPHY) description = Text.literal("Honoring a champion who embraced beauty, bonds, and mega power.");
        else if (item == ModItems.ALOLA_TROPHY) description = Text.literal("A memento from the islands, for a trainer who conquered the trials.");
        else if (item == ModItems.GALAR_TROPHY) description = Text.literal("Proof of a champion who reached for the stars in a dynamic region.");
        else if (item == ModItems.PALDEA_TROPHY) description = Text.literal("A treasure for the adventurer who explored the past and future.");
        else description = Text.literal("A reward for a truly dedicated Pokémon Master.");

        return description.copy().formatted(Formatting.GOLD);
    }
}