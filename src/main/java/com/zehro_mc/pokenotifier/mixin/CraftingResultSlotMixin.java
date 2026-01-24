package com.zehro_mc.pokenotifier.mixin;

import com.zehro_mc.pokenotifier.events.CraftingHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {
    
    @Shadow
    private PlayerEntity player;
    
    @Inject(method = "onCrafted(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    private void onItemCrafted(ItemStack stack, CallbackInfo ci) {
        if (this.player != null) {
            CraftingHandler.onCraftItem(stack, this.player);
        }
    }
}
