/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.mixin.client;

import com.zehro_mc.pokenotifier.client.ClientRankCache;
import com.zehro_mc.pokenotifier.util.PlayerRankUtil;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to add rank prefixes to player nametags by modifying the text
 * right before it's rendered.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @ModifyVariable(
            method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;" +
                    "Lnet/minecraft/text/Text;" +
                    "Lnet/minecraft/client/util/math/MatrixStack;" +
                    "Lnet/minecraft/client/render/VertexConsumerProvider;" +
                    "IF)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true,
            require = 0
    )
    private Text addPrefixToNametag(Text originalText, AbstractClientPlayerEntity player) {
        // Get the rank from our client-side cache and apply the same decoration as the chat.
        int rank = ClientRankCache.getRank(player.getUuid());
        return PlayerRankUtil.getDecoratedPlayerNameForChat(rank, originalText);
    }
}