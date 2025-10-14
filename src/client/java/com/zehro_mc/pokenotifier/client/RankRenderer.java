package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.util.RankInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class RankRenderer {

    private static final int ICON_SIZE = 9; // Tamaño del icono en píxeles
    private static final int ICON_SPACING = 1; // Espacio entre iconos
    private static final int TEXT_SPACING = 2; // Espacio entre el último icono y el texto

    /**
     * Dibuja el icono y el texto del rango y devuelve el ancho total dibujado.
     */
    public static int drawRank(DrawContext context, int x, int y, RankInfo rankInfo) {
        if (rankInfo == null) return 0;
        // Este método es para GUI 2D, lo dejaremos vacío por ahora para centrarnos en el Nametag.
        return 0;
    }

    public static int getRankWidth(RankInfo rankInfo) {
        if (rankInfo == null) return 0;
        int iconsWidth = rankInfo.iconCount() * (ICON_SIZE + ICON_SPACING);
        return iconsWidth + TEXT_SPACING + MinecraftClient.getInstance().textRenderer.getWidth(rankInfo.rank().displayText);
    }

    /**
     * Dibuja el icono y el texto del rango para un Nametag en el mundo 3D.
     * Devuelve el ancho total dibujado.
     */
    public static int drawNametagRank(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, RankInfo rankInfo) {
        if (rankInfo == null) return 0;
        
        int currentX = 0;
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        // 1. Dibujar los iconos
        // --- CORRECCIÓN: Usamos la API de renderizado 3D correcta ---
        // --- CORRECCIÓN FINAL: Usamos el RenderLayer correcto para texturas de GUI ---
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(rankInfo.rank().icon));
        for (int i = 0; i < rankInfo.iconCount(); i++) {
            float x1 = (float)currentX;
            float y1 = -4f; // Ajuste vertical para alinear con el texto
            float x2 = x1 + ICON_SIZE;
            float y2 = y1 + ICON_SIZE;
            float z = 0.0f;
            vertexConsumer.vertex(positionMatrix, x1, y1, z).color(255, 255, 255, 255).texture(0.0f, 0.0f).light(light);
            vertexConsumer.vertex(positionMatrix, x1, y2, z).color(255, 255, 255, 255).texture(0.0f, 1.0f).light(light);
            vertexConsumer.vertex(positionMatrix, x2, y2, z).color(255, 255, 255, 255).texture(1.0f, 1.0f).light(light); // CORRECCIÓN: El valor de color era inválido.
            vertexConsumer.vertex(positionMatrix, x2, y1, z).color(255, 255, 255, 255).texture(1.0f, 0.0f).light(light);
            currentX += ICON_SIZE + ICON_SPACING;
        }

        // 2. Dibujar el texto del rango
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        textRenderer.draw(rankInfo.rank().displayText, currentX, 0, 0xFFFFFF, false, positionMatrix, vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, light);

        return currentX + textRenderer.getWidth(rankInfo.rank().displayText);
    }
}