package com.zehro_mc.pokenotifier.mixin.client;

import com.zehro_mc.pokenotifier.client.ClientRankCache;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @ModifyVariable(
            method = "renderLabelIfPresent",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text addPrestigePrefixToNametag(Text originalText, AbstractClientPlayerEntity player) {
        // Inyectamos el prefijo al principio del nombre que el juego ya ha preparado.
        return ClientRankCache.getDecoratedName(player.getUuid(), originalText);
    }
}