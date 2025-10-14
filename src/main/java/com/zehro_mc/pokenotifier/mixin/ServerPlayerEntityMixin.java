/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.mixin;

import com.zehro_mc.pokenotifier.util.PlayerRankUtil;
import com.zehro_mc.pokenotifier.util.PlayerRankManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to modify the display name of players on the server side.
 * This affects how names appear in the chat.
 */
@Mixin(PlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void addPrestigePrefixToDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        // Ensure we only apply this to server-side players.
        if (player instanceof ServerPlayerEntity) {
            Text originalName = cir.getReturnValue();
            int rank = PlayerRankManager.getRank(player.getUuid());
            cir.setReturnValue(PlayerRankUtil.getDecoratedPlayerNameForChat(rank, originalName));
        }
    }
}