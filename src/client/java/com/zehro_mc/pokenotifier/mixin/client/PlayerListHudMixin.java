package com.zehro_mc.pokenotifier.mixin.client;

import com.zehro_mc.pokenotifier.client.ClientRankCache;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void addPrefixToTabListName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(ClientRankCache.getDecoratedName(entry.getProfile().getId(), cir.getReturnValue()));
    }
}