package com.zehro_mc.pokenotifier.events;

import com.zehro_mc.pokenotifier.item.ModItems;
import com.zehro_mc.pokenotifier.item.PokeNotifierDeviceItem;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class CraftingHandler {
    
    public static void register() {
        // This will be called from PokeNotifier.java
        // We'll use a mixin for crafting detection instead
    }
    
    public static void onCraftItem(ItemStack stack, PlayerEntity player) {
        if (stack.getItem() == ModItems.POKE_NOTIFIER_DEVICE) {
            String owner = PokeNotifierDeviceItem.getOwner(stack);
            if (owner == null || owner.isEmpty()) {
                PokeNotifierDeviceItem.setOwner(stack, player.getName().getString());
            }
        }
    }
}
