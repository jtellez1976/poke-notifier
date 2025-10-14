package com.zehro_mc.pokenotifier.mixin.client;

import com.zehro_mc.pokenotifier.util.PlayerRankUtil;
import com.zehro_mc.pokenotifier.client.ClientRankCache;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void getDecoratedPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        // --- CORRECCIÓN: Usamos la lógica correcta para construir el nombre ---
        int rank = ClientRankCache.getRank(entry.getProfile().getId());
        Text originalName = cir.getReturnValue();
        cir.setReturnValue(PlayerRankUtil.getDecoratedPlayerNameForChat(rank, originalName));
    }
}