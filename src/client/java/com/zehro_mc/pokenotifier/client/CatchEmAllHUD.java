package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.PokeNotifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class CatchEmAllHUD {

    private static final Identifier ASH_TEXTURE = Identifier.of(PokeNotifier.MOD_ID, "textures/gui/ash.png");

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        // Si el modo no está activo o no hay información, no dibujamos nada.
        // CORRECCIÓN: Añadimos una comprobación para asegurarnos de que la variable no es nula o vacía.
        if (PokeNotifierClient.currentCatchEmAllGeneration == null || "none".equals(PokeNotifierClient.currentCatchEmAllGeneration) || PokeNotifierClient.catchTotalCount == 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // --- CONSTANTES DE DISEÑO ---
        int ashWidth = 64;
        int ashHeight = 64;
        int margin = 5;
        int boxPadding = 4;

        // Formateamos el texto del progreso
        String genName = PokeNotifierClient.currentCatchEmAllGeneration.substring(0, 1).toUpperCase() + PokeNotifierClient.currentCatchEmAllGeneration.substring(1);
        Text progressText = Text.literal(genName + ": ").formatted(Formatting.YELLOW) // Cambiamos el color a amarillo para mayor visibilidad
                .append(Text.literal(PokeNotifierClient.catchCaughtCount + "/" + PokeNotifierClient.catchTotalCount).formatted(Formatting.GREEN));

        // --- CÁLCULOS DE POSICIÓN Y TAMAÑO ---
        int textWidth = client.textRenderer.getWidth(progressText);
        int boxWidth = textWidth + (boxPadding * 2);
        int boxHeight = client.textRenderer.fontHeight + (boxPadding * 2);

        // Posicionamiento dinámico sobre el chat
        ChatHud chatHud = client.inGameHud.getChatHud();
        int chatHeight = chatHud.getHeight();
        int hudHeight = ashHeight; // La altura total del HUD está determinada por el sprite de Ash

        int anchorX = margin;
        int anchorY = screenHeight - chatHeight - hudHeight - margin;

        // Habilitamos el blending para la transparencia
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // --- DIBUJADO DE ELEMENTOS ---
        // 1. Dibujar el sprite de Ash
        context.drawTexture(ASH_TEXTURE, anchorX, anchorY, 0, 0, ashWidth, ashHeight, ashWidth, ashHeight);

        // 2. Dibujar la caja del score (efecto 3D)
        int boxX = anchorX + ashWidth;
        int boxY = anchorY + (hudHeight / 2) - (boxHeight / 2);
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x80000000); // Fondo semi-transparente

        // 3. Dibujar el texto del score
        int textX = boxX + boxPadding;
        int textY = boxY + boxPadding;
        context.drawTextWithShadow(client.textRenderer, progressText, textX, textY, 0xFFFFFF);

        RenderSystem.disableBlend();
    }
}