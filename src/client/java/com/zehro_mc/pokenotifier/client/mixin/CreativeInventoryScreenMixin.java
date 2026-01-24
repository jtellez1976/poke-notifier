package com.zehro.mc.pokenotifier.client.mixin;

import com.zehro_mc.pokenotifier.PokeNotifier;
import com.zehro_mc.pokenotifier.client.util.ServerModDetector;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryScreenMixin {
    
    /**
     * Hide Poke Notifier creative tab in client-only mode to prevent crashes
     * when trying to use items that don't exist on the server.
     */
    @Inject(method = "setSelectedTab", at = @At("HEAD"), cancellable = true)
    private void preventPokeNotifierTabInClientOnly(ItemGroup group, CallbackInfo ci) {
        if (ServerModDetector.isClientOnly() && group != null) {
            Identifier groupId = Registries.ITEM_GROUP.getId(group);
            Identifier pokeNotifierGroupId = Identifier.of(PokeNotifier.MOD_ID, "poke_notifier");
            
            if (groupId != null && groupId.equals(pokeNotifierGroupId)) {
                // Cancel switching to Poke Notifier tab in client-only mode
                ci.cancel();
            }
        }
    }
}
