package com.zehro_mc.pokenotifier.mixin.client;

import com.zehro_mc.pokenotifier.client.ClientRankCache;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), name = "text")
    private Text addPrefixToTabListName(Text originalText, PlayerListEntry entry) {
        // Inyectamos el prefijo al principio del nombre que el juego ya ha preparado.
        return ClientRankCache.getDecoratedName(entry.getProfile().getId(), originalText);
    }
}