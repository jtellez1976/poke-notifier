package com.zehro_mc.pokenotifier.mixin.client;

import com.zehro_mc.pokenotifier.client.ClientRankCache;
import com.zehro_mc.pokenotifier.client.RankRenderer;
import com.zehro_mc.pokenotifier.util.PlayerRankUtil;
import com.zehro_mc.pokenotifier.util.RankInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject( // --- CORRECCIÓN: La firma del método ahora es 100% precisa ---
            method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void renderCustomNametag(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        // Cancelamos el método original para tomar control total del renderizado.
        ci.cancel();

        int rankCount = ClientRankCache.getRank(player.getUuid());
        RankInfo rankInfo = PlayerRankUtil.getRankInfo(rankCount);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int rankDisplayWidth = RankRenderer.getRankWidth(rankInfo);
        MutableText playerName = player.getDisplayName().copy();
        if (rankCount >= 9) playerName.formatted(Formatting.GOLD);
        float nameWidth = textRenderer.getWidth(player.getDisplayName()); // Usamos el nombre original para el ancho
        float totalWidth = rankDisplayWidth + 2 + nameWidth;

        matrices.push();
        matrices.translate(-totalWidth / 2, 0, 0);

        // 1. Dibujamos el rango (iconos y texto)
        int drawnRankWidth = RankRenderer.drawNametagRank(matrices, vertexConsumers, light, rankInfo);

        // 2. Dibujamos el nombre del jugador, desplazado por el ancho del rango.
        textRenderer.draw(playerName, drawnRankWidth + 2, 0, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, light);

        matrices.pop();
    }
}