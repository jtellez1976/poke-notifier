/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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

/**
 * Mixin to add rank prefixes to player names in the player list (TAB).
 */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void getDecoratedPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        // Get the rank and apply the text-based prefix for the TAB list.
        int rank = ClientRankCache.getRank(entry.getProfile().getId());
        Text originalName = cir.getReturnValue();
        cir.setReturnValue(PlayerRankUtil.getDecoratedPlayerNameForChat(rank, originalName));
    }
}